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
package io.helidon.dbclient.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClientServiceContext;
import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbStatementInsert;
import io.helidon.dbclient.common.DbStatementContext;

class JdbcStatementInsert extends JdbcStatement<DbStatementInsert, Single<Long>> implements DbStatementInsert {
    private static final Logger LOGGER = Logger.getLogger(JdbcStatementInsert.class.getName());

    JdbcStatementInsert(JdbcExecuteContext executeContext,
                        DbStatementContext statementContext) {
        super(executeContext, statementContext);
    }

    @Override
    protected Single<Long> doExecute(Single<DbClientServiceContext> dbContextFuture,
                                     CompletableFuture<Void> statementFuture,
                                     CompletableFuture<Long> queryFuture) {

        executeContext().addFuture(queryFuture);

        // query and statement future must always complete either OK, or exceptionally
        dbContextFuture.exceptionally(throwable -> {
            statementFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });

        return dbContextFuture
                .flatMapSingle(dbContext -> doExecute(dbContext, statementFuture, queryFuture));
    }

    @Override
    public Single<List<DbColumn>> executeReturnIds() {
        CompletableFuture<Long> queryFuture = new CompletableFuture<>();
        CompletableFuture<Void> statementFuture = new CompletableFuture<>();
        DbClientServiceContext context = DbClientServiceContext.create(dbType())
                .resultFuture(queryFuture)
                .statementFuture(statementFuture);

        update(context);
        Single<DbClientServiceContext> dbContextFuture = clientContext().invokeServices(context);
        executeContext().addFuture(queryFuture);

        // query and statement future must always complete either OK, or exceptionally
        dbContextFuture.exceptionally(throwable -> {
            statementFuture.completeExceptionally(throwable);
            queryFuture.completeExceptionally(throwable);
            return null;
        });

        return dbContextFuture
                .flatMapSingle(dbContext -> doExecuteReturnIds(dbContext, statementFuture, queryFuture));
    }

    private Single<List<DbColumn>> doExecuteReturnIds(DbClientServiceContext dbContext,
                                                      CompletableFuture<Void> statementFuture,
                                                      CompletableFuture<Long> queryFuture) {
        return Single.create(connection())
                .flatMapSingle(connection -> doExecuteReturnsIds(dbContext, connection, statementFuture, queryFuture));
    }

    private Single<Long> doExecute(DbClientServiceContext dbContext,
                                   CompletableFuture<Void> statementFuture,
                                   CompletableFuture<Long> queryFuture) {

        return Single.create(connection())
                .flatMapSingle(connection -> doExecute(dbContext, connection, statementFuture, queryFuture));
    }

    private Single<List<DbColumn>> doExecuteReturnsIds(DbClientServiceContext dbContext,
                                                       Connection connection,
                                                       CompletableFuture<Void> statementFuture,
                                                       CompletableFuture<Long> queryFuture) {

        CompletableFuture<List<DbColumn>> result = new CompletableFuture<>();
        executorService().submit(() -> callStatementReturningIds(dbContext, connection, statementFuture, queryFuture, result));

        // the query future is reused, as it completes with the number of updated records
        return Single.create(result);
    }

    private Single<Long> doExecute(DbClientServiceContext dbContext,
                                   Connection connection,
                                   CompletableFuture<Void> statementFuture,
                                   CompletableFuture<Long> queryFuture) {

        executorService().submit(() -> callStatement(dbContext, connection, statementFuture, queryFuture));

        // the query future is reused, as it completes with the number of updated records
        return Single.create(queryFuture);
    }

    private void callStatementReturningIds(DbClientServiceContext dbContext,
                                           Connection connection,
                                           CompletableFuture<Void> statementFuture,
                                           CompletableFuture<Long> queryFuture,
                                           CompletableFuture<List<DbColumn>> result) {

        try {
            PreparedStatement preparedStatement = build(connection, dbContext);
            long count = preparedStatement.executeLargeUpdate();
            statementFuture.complete(null);

            List<DbColumn> list = new LinkedList<>();

            try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                DbColumn metadata = createMetadataSingleColumn(rs);
                if (metadata != null) {
                    while (rs.next()) {
                        list.add(JdbcDbColumn.create(mapperManager(), metadata, rs.getObject(1)));
                    }
                }
            }

            queryFuture.complete(count);
            result.complete(list);
            preparedStatement.close();
        } catch (Exception e) {
            statementFuture.completeExceptionally(e);
            queryFuture.completeExceptionally(e);
            result.completeExceptionally(e);
        }
    }

    private DbColumn createMetadataSingleColumn(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        if (columnCount == 0) {
            return null;
        }
        if (columnCount != 1) {
            LOGGER.fine("Insert statement returned more than one column for generated ids, only first is used");
        }

        String name = metaData.getColumnLabel(1);
        String sqlType = metaData.getColumnTypeName(1);
        Class<?> javaClass = JdbcStatementQuery.classByName(metaData.getColumnClassName(1));

        return JdbcDbColumn.create(javaClass, sqlType, name);
    }

    private void callStatement(DbClientServiceContext dbContext,
                               Connection connection,
                               CompletableFuture<Void> statementFuture,
                               CompletableFuture<Long> queryFuture) {
        try {
            PreparedStatement preparedStatement = build(connection, dbContext);
            long count = preparedStatement.executeLargeUpdate();
            statementFuture.complete(null);
            queryFuture.complete(count);
            preparedStatement.close();
        } catch (Exception e) {
            statementFuture.completeExceptionally(e);
            queryFuture.completeExceptionally(e);
        }
    }
}
