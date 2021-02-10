package io.helidon.di.dbclient;

import io.helidon.dbclient.DbClient;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.Internal;

@Factory
@Internal
public class DbClientFactory {
    protected DbClientFactory() {
    }

    @EachBean(DbClientConfig.class)
    protected DbClient.Builder dbClientBuilder(DbClientConfig config) {
        return config.dbClientBuilder();
    }

    @EachBean(DbClient.Builder.class)
    protected DbClient dbClientNamed(DbClient.Builder builder) {
        return builder.build();
    }
}


