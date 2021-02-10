package io.helidon.di.webserver;

import javax.inject.Singleton;

import io.helidon.config.Config;
import io.helidon.webserver.WebServer;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;

class SocketConfig {
    private final String name;
    private final String host;
    private final int port;

    SocketConfig(String name, Config socketConfig) {
        this.name = name;
        this.host = socketConfig.get("host").asString().orElse(null);
        this.port = socketConfig.get("port").asInt().orElse(0);
    }

    public String name() {
        return name;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    @Factory
    static class SocketConfigFactory {
        private final Config config;

        protected SocketConfigFactory(Config config) {
            this.config = config;
        }

        @Singleton
        public SocketConfig produce() {
            Config socketConfig = config.get("server");
            return new SocketConfig(WebServer.DEFAULT_SOCKET_NAME, socketConfig);
        }

        @EachBean(SocketConfigs.class)
        public SocketConfig produceForEach(SocketConfigs config) {
            return config.socketConfig();
        }
    }

    @EachProperty("server.sockets")
    static class SocketConfigs {
        private final SocketConfig socketConfig;

        protected SocketConfigs(@Parameter String name, Config config) {
            this.socketConfig = new SocketConfig(name, config.get("server.sockets." + name));
        }

        SocketConfig socketConfig() {
            return socketConfig;
        }
    }
}
