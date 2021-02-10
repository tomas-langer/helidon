package io.helidon.examples.di.basics;

import io.helidon.config.Config;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;

@EachProperty("server.sockets")
public class ForEachSocket {
    private final Config config;

    protected ForEachSocket(@Parameter String name, Config config) {
        this.config = config.get("server.sockets." + name);
    }

    String message() {
        return config.get("port").asString().get();
    }
}
