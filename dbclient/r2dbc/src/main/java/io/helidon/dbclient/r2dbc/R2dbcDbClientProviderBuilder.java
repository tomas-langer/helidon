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

import io.helidon.common.GenericType;
import io.helidon.common.mapper.MapperManager;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbClientService;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbStatements;
import io.helidon.dbclient.spi.DbClientProviderBuilder;
import io.helidon.dbclient.spi.DbMapperProvider;

public class R2dbcDbClientProviderBuilder implements DbClientProviderBuilder {
    @Override
    public DbClientProviderBuilder config(Config config) {
        return null;
    }

    @Override
    public DbClientProviderBuilder url(String url) {
        return null;
    }

    @Override
    public DbClientProviderBuilder username(String username) {
        return null;
    }

    @Override
    public DbClientProviderBuilder password(String password) {
        return null;
    }

    @Override
    public DbClientProviderBuilder statements(DbStatements statements) {
        return null;
    }

    @Override
    public DbClientProviderBuilder addMapperProvider(DbMapperProvider provider) {
        return null;
    }

    @Override
    public DbClientProviderBuilder mapperManager(MapperManager manager) {
        return null;
    }

    @Override
    public DbClientProviderBuilder addService(DbClientService clientService) {
        return null;
    }

    @Override
    public DbClient build() {
        return null;
    }

    @Override
    public DbClientProviderBuilder addMapper(DbMapper dbMapper, GenericType mappedType) {
        return null;
    }

    @Override
    public DbClientProviderBuilder addMapper(DbMapper dbMapper, Class mappedClass) {
        return null;
    }
}
