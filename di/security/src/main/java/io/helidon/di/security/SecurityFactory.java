package io.helidon.di.security;

import javax.inject.Singleton;

import io.helidon.config.Config;
import io.helidon.security.Security;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.webserver.WebServer;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;

@Internal
@Factory
class SecurityFactory {
    private final Config config;

    protected SecurityFactory(Config config) {
        this.config = config.get("security");
    }

    @Singleton
    public Security.Builder builder() {
        return Security.builder()
                .config(config);
    }

    @Singleton
    public Security security(Security.Builder builder) {
        return builder.build();
    }

    @Singleton
    @Requires(classes = WebServer.class)
    public WebSecurity webSecurity(Security security) {
        return WebSecurity.create(security, config);
    }
}
