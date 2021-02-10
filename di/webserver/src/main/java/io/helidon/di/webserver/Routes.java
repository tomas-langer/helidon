package io.helidon.di.webserver;

import java.util.Map;

import io.helidon.webserver.Routing;

class Routes {
    private final Map<String, Routing> routings;

    public Routes(Map<String, Routing> routings) {
        this.routings = routings;
    }

    public Map<String, Routing> routings() {
        return routings;
    }
}
