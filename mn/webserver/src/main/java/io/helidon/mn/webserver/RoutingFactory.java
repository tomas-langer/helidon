package io.helidon.mn.webserver;

import javax.inject.Singleton;

import io.helidon.webserver.Routing;

import io.micronaut.context.annotation.Factory;

@Factory
public class RoutingFactory {

    @Singleton
    protected Routing.Builder builder() {
        return Routing.builder();
    }

    @Singleton
    protected Routing routing(Routing.Builder builder) {
        return builder.build();
    }
}
