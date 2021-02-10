package io.helidon.examples.di.basics;

import javax.inject.Singleton;

import io.helidon.config.Config;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;

@Factory
public class MyFactory {
    private final Config config;

    protected MyFactory(Config config) {
        this.config = config;
    }

    @Singleton
    public Produced produce() {
        return new Produced(config.get("server.port").asString().get());
    }

    @EachBean(ForEachSocket.class)
    public Produced produceForEach(ForEachSocket config) {
        return new Produced(config.message());
    }

    public static class Produced {
        private final String message;

        public Produced(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "Produced{" +
                    "message='" + message + '\'' +
                    '}';
        }
    }
}
