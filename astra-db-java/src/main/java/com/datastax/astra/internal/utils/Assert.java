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

package com.datastax.astra.internal.utils;

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
 * Syntaxic sugar for common validations.
 * 
 * @author Cedrick LUNVEN (@clunven)
 */
public class Assert {
    
    /**
     * Hide default.
     */
    private Assert() {}
    
    /**
     * Input string should not be empty.
     *
     * @param s
     *      string value
     * @param name
     *      param name
     */
    public static void hasLength(String s, String name) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException("Parameter '" + name + "' should be null nor empty");
        }
    }
    
    /**
     * Input object should not be null
     * @param o
     *      object value
     * @param name
     *      param name
     */
    public static void notNull(Object o, String name) {
        if (o == null) {
            throw new IllegalArgumentException("Parameter '" + name + "' should be null nor empty");
        }
    }
    
    /**
     * Check condition at start.
     *
     * @param condition
     *      predicate should be true
     * @param msg
     *      error message
     */
    public static void isTrue(boolean condition, String msg) {
        if (!condition) {
            throw new IllegalArgumentException(msg);
        }
    }

}
