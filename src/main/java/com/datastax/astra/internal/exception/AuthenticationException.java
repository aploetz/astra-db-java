/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datastax.astra.internal.exception;

/*-
 * #%L
 * Data API Java Client
 * --
 * Copyright (C) 2024 DataStax
 * --
 * Licensed under the Apache License, Version 2.0
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * Specialized Error.
 *
 * @author Cedrick LUNVEN (@clunven)
 */
public class AuthenticationException extends IllegalStateException {
    
    /** Serial. */
    private static final long serialVersionUID = -4491748257797687008L;
    
    /**
     * Default Constructor.
     */
    public AuthenticationException() {
        this("Cannot authenticate, check token and/or credentials");
    }
    
    /**
     * Constructor with message
     * @param msg
     *      message
     */
    public AuthenticationException(String msg) {
        super(msg);
    }

    /**
     * Constructor with exception
     * @param parent
     *      parent exception
     */
    public AuthenticationException(Throwable parent) {
        this("Cannot authenticate, check token and/or credentials", parent);
    }
    
    /**
     * Constructor with message and exception
     * @param msg
     *      message
     * @param parent
     *      parent exception      
     */
    public AuthenticationException(String msg, Throwable parent) {
        super(msg, parent);
    }

}