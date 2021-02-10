package io.helidon.di.dbclient;

import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Internal;

@EachProperty(value = "db", primary = "default")
@Internal
class DbClientConfig {
    private final DbClient.Builder dbClientBuilder = DbClient.builder();

    protected DbClientConfig(@Parameter String name, Config config) {
        dbClientBuilder.config(config.get("db." + name));
    }

    DbClient.Builder dbClientBuilder() {
        return dbClientBuilder;
    }
}
