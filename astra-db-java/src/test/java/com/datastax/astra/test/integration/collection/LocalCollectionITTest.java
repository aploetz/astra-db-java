package com.datastax.astra.test.integration.collection;

import com.datastax.astra.client.DataAPIClients;
import com.datastax.astra.client.Database;

/**
 * Allow to test Collection information.
 */
class LocalCollectionITTest extends AbstractCollectionITTest {

    /** {@inheritDoc} */
    @Override
    protected Database initDatabase() {
        return DataAPIClients.createDefaultLocalDatabase();
    }

}
