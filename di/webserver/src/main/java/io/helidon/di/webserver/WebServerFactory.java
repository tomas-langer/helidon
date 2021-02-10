package io.helidon.di.webserver;

import javax.inject.Singleton;

import io.helidon.config.Config;
import io.helidon.webserver.WebServer;

import io.micronaut.context.annotation.Factory;

@Factory
class WebServerFactory {
    private final Config config;

    protected WebServerFactory(Config config) {
        this.config = config;
    }

    @Singleton
    protected WebServer.Builder webServerBuilder() {
        return WebServer.builder()
                .config(config.get("server"));
    }
}
