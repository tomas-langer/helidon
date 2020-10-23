/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.dbclient.r2dbc;

import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;

import io.r2dbc.h2.H2ConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;
import org.reactivestreams.FlowAdapters;

import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL;

public class Main {
    public static void main(String[] args) {
        /*
         .option(PROTOCOL, "postgresql") // driver identifier, PROTOCOL is delegated as DRIVER by the pool.
   .option(HOST, "…")
   .option(PORT, "…")
   .option(USER, "…")
   .option(PASSWORD, "…")
   .option(DATABASE, "…")
         */
        ConnectionFactory factory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                                                                    .option(DRIVER, "pool")
                // only mem or file is supported for now
                                                                    .option(PROTOCOL, "h2:mem")
                .option(H2ConnectionFactoryProvider.OPTIONS, "INIT=runscript from 'classpath:/h2.create.sql'")
                                                                    //.option(PROTOCOL, "h2:tcp")
//                                                                    .option(HOST, "localhost")
//                                                                    .option(PORT, 9092)
//                                                                    .option(USER, "sa")
//                                                                    .option(PASSWORD, "")
                                                                    .option(DATABASE, "test")
                                                                    .build());

        Single.create(FlowAdapters.toFlowPublisher(factory.create()))
                .flatMapSingle(conn -> {
                    Statement select = conn.createStatement("SELECT * FROM GREETING");
                    return Single.create(FlowAdapters.toFlowPublisher(select.execute()));
                })
                .flatMapSingle(result -> Multi.create(FlowAdapters.toFlowPublisher(result.map(Main::toDbRow)))
                        .forEach(Main::printIt))
                .await();
    }

    private static void printIt(String it) {
        System.out.println("Row: " + it);
    }

    private static String toDbRow(Row row, RowMetadata rowMetadata) {
        return row.get(0, String.class) + ": " + row.get(1, String.class);
    }
}
