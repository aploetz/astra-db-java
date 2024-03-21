package com.datastax.astra;


import com.datastax.astra.devops.db.AstraDBDevopsClient;
import com.datastax.astra.devops.db.DbOpsClient;
import com.datastax.astra.devops.db.domain.CloudProviderType;
import com.datastax.astra.devops.db.domain.Database;
import com.datastax.astra.devops.db.domain.DatabaseCreationRequest;
import com.datastax.astra.devops.db.domain.DatabaseStatusType;
import com.datastax.astra.devops.db.exception.DatabaseNotFoundException;
import com.datastax.astra.devops.utils.ApiLocator;
import com.datastax.astra.devops.utils.Assert;
import com.datastax.astra.devops.utils.AstraEnvironment;
import com.datastax.astra.devops.utils.AstraRc;
import io.stargate.sdk.data.client.DataApiClient;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.stargate.sdk.utils.AnsiUtils.green;
import static io.stargate.sdk.utils.Utils.readEnvVariable;

/**
 * Main Client for AstraDB, it implements administration and Data Api Operations.
 *
 * @see AstraDBDevopsClient Client specialized in the Devops API operations
 * @see DataApiClient Client specialized for Data API operation
 */
@Slf4j
public class AstraDBAdmin implements Admin {

    /** Default cloud provider. (free-tier) */
    public static final CloudProviderType FREE_TIER_CLOUD = CloudProviderType.GCP;

    /** Default region. (free-tier) */
    public static final String FREE_TIER_CLOUD_REGION = "us-east1";

    /** Header name used to hold the Astra Token. */
    public static final String TOKEN_HEADER_PARAM = "X-Token";

    /** Default keyspace (same created by the ui). */
    public static final String DEFAULT_KEYSPACE = "default_keyspace";

    /** Default options. */
    public static final AstraClientOptions DEFAULT_OPTIONS = AstraClientOptions.builder().build();

    /** Client for Astra Devops Api. */
    final AstraDBDevopsClient devopsDbClient;

    /** Options to personalized http client other client options. */
    final AstraClientOptions astraClientOptions;

    /** Astra Environment. */
    final AstraEnvironment env;

    /** Astra Token (credentials). */
    final String token;

    /** Side Http Client (use only to resume a db). */
    final HttpClient httpClient;

    /** Token read for environment variable ASTRA_DB_APPLICATION_TOKEN (if any). */
    static String astraConfigToken;

    /*
     * Load token values from environment variables and ~/.astrarc.
     */
    static {
        new AstraRc().getSectionKey(
                AstraRc.ASTRARC_DEFAULT,
                AstraRc.ASTRA_DB_APPLICATION_TOKEN).ifPresent(s -> astraConfigToken = s);
        // lookup env variable
        readEnvVariable(AstraRc.ASTRA_DB_APPLICATION_TOKEN).ifPresent(s -> astraConfigToken = s);
    }

    /**
     * Default constructor. The token is read from environment variable <code>ASTRA_DB_APPLICATION_TOKEN</code> or
     * file <code>~/.astrarc</code>, section <code>default</code>,  key <code>ASTRA_DB_APPLICATION_TOKEN</code>.
     */
    public AstraDBAdmin() {
        this(astraConfigToken);
    }

    /**
     * Constructor with an authentification token, defaulting to production environment, and default http options.
     *
     * @param token
     *      authentication token
     */
    public AstraDBAdmin(String token) {
        this(token, DEFAULT_OPTIONS);
    }

    /**
     * Constructor with an authentification token, defaulting to production environment, and default http options.
     *
     * @param token
     *      authentication token
     * @param astraClientOptions
     *      options for AstraDb.
     */
    public AstraDBAdmin(String token, AstraClientOptions astraClientOptions) {
        this(token, astraClientOptions, AstraEnvironment.PROD);
    }

    /**
     * Initialization with an authentification token and target environment, Use this constructor for testing purpose.
     *
     * @param token
     *      authentication token
     * @param env
     *      target Astra environment
     * @param astraClientOptions
     *      options for AstraDb.
     */
    public AstraDBAdmin(String token, AstraClientOptions astraClientOptions, AstraEnvironment env) {
        this.env = env;
        this.token = token;
        this.devopsDbClient = new AstraDBDevopsClient(token, env);
        this.astraClientOptions = astraClientOptions;

        // Local Agent for Resume
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        httpClientBuilder.version(astraClientOptions.asHttpClientOptions().getHttpVersion());
        httpClientBuilder.connectTimeout(Duration.ofSeconds(astraClientOptions.asHttpClientOptions().getConnectionRequestTimeoutInSeconds()));
        this.httpClient = httpClientBuilder.build();
    }

    // --------------------
    // -- Databases     ---
    // --------------------

    /**
     * List available database names.
     *
     * @return
     *      list of database names
     */
    public List<String> listDatabaseNames() {
        return listDatabases().map(db -> db.getInfo().getName()).collect(Collectors.toList());
    }

    /**
     * List active databases with vector enabled in current organization.
     *
     * @return
     *      active databases list
     */
    public Stream<Database> listDatabases() {
        return devopsDbClient
                .findAllNonTerminated()
                .filter(db -> db.getInfo().getDbType() != null);
    }

    /**
     * Create new database with a name on free tier. The database name should not exist in the tenant.
     *
     * @param name
     *    database name
     * @return
     *    database identifier
     */
    public UUID createDatabase(@NonNull String name) {
        return createDatabase(name, FREE_TIER_CLOUD, FREE_TIER_CLOUD_REGION);
    }

    /**
     * Create new database with a name on the specified cloud provider and region.
     * If the database with same name already exists it will be resumed if not active.
     * The method will wait for the database to be active.
     *
     * @param name
     *      database name
     * @param cloud
     *      cloud provider
     * @param cloudRegion
     *      cloud region
     * @return
     *      database identifier
     */
    public UUID createDatabase(@NonNull String name, @NonNull CloudProviderType cloud, @NonNull String cloudRegion) {
        Optional<Database> optDb = listDatabases().filter(db->name.equals(db.getInfo().getName())).findFirst();
        // Handling all cases for the user
        if (optDb.isPresent()) {
            Database db = optDb.get();
            switch(db.getStatus()) {
                case ACTIVE:
                    log.info("Database " + green("{}") + " already exists and is ACTIVE.", name);
                    return UUID.fromString(db.getId());
                case MAINTENANCE:
                case INITIALIZING:
                case PENDING:
                case RESUMING:
                    log.info("Database {} already exists and is in {} state, waiting for it to be ACTIVE", name, db.getStatus());
                    waitForDatabase(devopsDbClient.database(db.getId()));
                    return UUID.fromString(db.getId());
                case HIBERNATED:
                    log.info("Database {} is in {} state, resuming...", name, db.getStatus());
                    resumeDb(db);
                    waitForDatabase(devopsDbClient.database(db.getId()));
                    return UUID.fromString(db.getId());
                default:
                    throw new IllegalStateException("Database already exist but cannot be activate");
            }
        }
        // Database is not present, creating and waiting for it to be active.
        UUID newDbId = UUID.fromString(devopsDbClient.create(DatabaseCreationRequest.builder()
                .name(name)
                .cloudProvider(cloud)
                .cloudRegion(cloudRegion)
                .keyspace(DEFAULT_KEYSPACE)
                .withVector().build()));
        log.info("Database {} is starting (id={}): it will take about a minute please wait...", name, newDbId);
        waitForDatabase(devopsDbClient.database(newDbId.toString()));
        return newDbId;
    }

    /**
     * Delete a Database if exists from its name
     *
     * @param databaseId
     *    database identifier
     * @return
     *      if the db has been deleted
     */
    public boolean dropDatabase(@NonNull UUID databaseId) {
        getDatabaseInformations(databaseId);
        devopsDbClient.database(databaseId.toString()).delete();
        return true;
    }

    /**
     * Find a database from its id.
     *
     * @param id
     *          a database name
     * @return
     *          list of db matching the criteria
     */
    public Database getDatabaseInformations(@NonNull UUID id) {
        Assert.notNull(id, "Database identifier should not be null");
        return devopsDbClient.findById(id.toString())
                .orElseThrow(() -> new DatabaseNotFoundException(id.toString()));
    }

    // --------------------
    // == Sub resources  ==
    // --------------------

    /**
     * Access the database functions.
     *
     * @param databaseId
     *      database identifier
     * @return
     *      database client
     */
    public AstraDatabase getDatabase(UUID databaseId) {
        String databaseRegion = devopsDbClient
                .findById(databaseId.toString())
                .map(db -> db.getInfo().getRegion())
                .orElseThrow(() -> new DatabaseNotFoundException(databaseId.toString()));
        return new AstraDatabase(
                new AstraApiEndpoint(databaseId, databaseRegion, env),
                token, AstraDBAdmin.DEFAULT_KEYSPACE, astraClientOptions);
    }

    /**
     * Access the database functions.
     *
     * @param databaseId
     *      database identifier
     * @return
     *      database client
     */
    public AstraDBDatabaseAdmin getDatabaseAdmin(UUID databaseId) {
        return new AstraDBDatabaseAdmin(token, databaseId, env, astraClientOptions);
    }

    /**
     * Wait for db to have proper status.
     *
     * @param dbc
     *      database client
     */
    @SuppressWarnings("java:S2925")
    private void waitForDatabase(DbOpsClient dbc) {
        long top = System.currentTimeMillis();
        while(DatabaseStatusType.ACTIVE != getStatus(dbc) && ((System.currentTimeMillis()-top) < 1000L*180)) {
            try {
                Thread.sleep( 5000);
                System.out.print("■");
                System.out.flush();
            } catch (InterruptedException e) {
                log.warn("Interrupted {}",e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        if (getStatus(dbc) != DatabaseStatusType.ACTIVE) {
            throw new IllegalStateException("Database is not in expected state after timeouts");
        }
    }

    /**
     * Retrieve the status of a database.
     * @param dbc
     *      database client
     * @return
     *      database status
     */
    private DatabaseStatusType getStatus(DbOpsClient dbc) {
        return dbc.find().orElseThrow(() -> new DatabaseNotFoundException(dbc.getDatabaseId())).getStatus();
    }

    /**
     * Database name.
     *
     * @param db
     *      database name
     */
    private void resumeDb(Database db) {
        try {
            // Compute Endpoint for the Keyspace
            String endpoint = ApiLocator.getApiRestEndpoint(db.getId(), db.getInfo().getRegion()) + "/v2/schemas/keyspace";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header(TOKEN_HEADER_PARAM, token)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 500) {
                throw new IllegalStateException("Cannot resume database, please check your account");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted {}",e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Resuming request might have failed, please check {}}",e.getMessage());
        }
    }

}
