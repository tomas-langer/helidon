/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.dbclient.jdbc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.helidon.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbRow;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;

class InsertReturningIdTest {

    private static DbClient dbClient;

    @BeforeAll
    static void setUp() {
        LogConfig.configureRuntime();

        Config config = Config.builder()
                .addSource(ConfigSources.classpath("insert-returning-id-test.yaml"))
                .build();

        dbClient = DbClient.builder()
                .config(config.get("db"))
                .build();

        initSchema(dbClient);
    }

    private static void initSchema(DbClient dbClient) {
        dbClient.execute(exec -> exec
                .namedDml("create-pokemons"))
                .await(10, TimeUnit.SECONDS);
    }

    @AfterAll
    static void stopDb() {
        dbClient.close()
                .await(10, TimeUnit.SECONDS);
    }

    @Test
    void testReturnIds() {
        List<DbColumn> ids = dbClient.execute(exec -> exec
                .createNamedInsert("insert-pokemon")
                .params("Charmander")
                .executeReturnIds())
                .await(10, TimeUnit.SECONDS);

        assertThat(ids, iterableWithSize(1));
        DbColumn column = ids.get(0);

        long id = column.as(Long.class);
        assertThat(id, not(0));

        // now check the record exists
        Optional<DbRow> charmander = dbClient.execute(exec -> exec
                .namedGet("select-pokemon-by-name", "Charmander"))
                .await(10, TimeUnit.SECONDS);

        assertThat(charmander, not(Optional.empty()));
        DbRow row = charmander.get();
        DbColumn idColumn = row.column("ID");

        assertThat(idColumn.as(Long.class), is(id));
    }
}
