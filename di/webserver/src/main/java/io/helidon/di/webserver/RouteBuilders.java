package io.helidon.di.webserver;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import io.helidon.webserver.ErrorHandler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

@Singleton
public class RouteBuilders {
    private final Routing.Builder defaultRouting = Routing.builder();
    private final Map<String, Routing.Builder> namedRoutings = new HashMap<>();
    private final Map<Class<? extends Throwable>, ErrorHandler<?>> errorHandlers = new LinkedHashMap<>();

    protected RouteBuilders(List<SocketConfig> socketConfigs) {
        for (SocketConfig socketConfig : socketConfigs) {
            if (WebServer.DEFAULT_SOCKET_NAME.equals(socketConfig.name())) {
                continue;
            }
            namedRoutings.put(socketConfig.name(), Routing.builder());
        }
    }

    public Routing.Builder routing() {
        return defaultRouting;
    }

    public Optional<Routing.Builder> routing(String routingName) {
        if (routingName.equals(WebServer.DEFAULT_SOCKET_NAME)) {
            return Optional.of(routing());
        }
        return Optional.ofNullable(namedRoutings.get(routingName));
    }

    public <T extends Throwable> void errorHandler(Class<T> throwableType, ErrorHandler<T> handler) {
        errorHandlers.put(throwableType, handler);
    }

    @SuppressWarnings("unchecked")
    Map<String, Routing.Builder> routes() {
        namedRoutings.put(WebServer.DEFAULT_SOCKET_NAME, defaultRouting);

        namedRoutings.values()
                .forEach(builder -> {
                    errorHandlers.forEach((throwableClass, aHandler) -> {
                        Class<Throwable> clazz = (Class<Throwable>) throwableClass;
                        ErrorHandler<Throwable> handler = (ErrorHandler<Throwable>) aHandler;
                        builder.error(clazz, handler);
                    });
                });

        return namedRoutings;
    }
}
