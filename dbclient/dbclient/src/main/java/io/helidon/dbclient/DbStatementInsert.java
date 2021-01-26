/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.dbclient;

import java.util.List;

import io.helidon.common.reactive.Single;

/**
 * DML Database statement.
 * A DML statement modifies records in the database and returns the number of modified records.
 */
public interface DbStatementInsert extends DbStatement<DbStatementInsert, Single<Long>> {
    default Single<List<DbColumn>> executeReturnIds() {
        throw new UnsupportedOperationException("Execute returning ids is not implemented yet for this type");
    }
}
