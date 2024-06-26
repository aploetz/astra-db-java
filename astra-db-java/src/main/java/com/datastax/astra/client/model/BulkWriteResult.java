package com.datastax.astra.client.model;

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

import com.datastax.astra.internal.api.ApiResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Store the list of responses returned by the bulk write.
 */
@Getter @Setter
public class BulkWriteResult {

    /**
     * List of responses returned by the bulk write.
     */
    List<ApiResponse> responses;

    /**
     * Constructor with the number of operations.
     *
     * @param size
     *      number of operations to process.
     */
    public BulkWriteResult(int size) {
        this.responses = new ArrayList<>(size);
    }



}
