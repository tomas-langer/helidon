package io.helidon.di.security;

import javax.inject.Singleton;

import io.helidon.config.Config;
import io.helidon.di.ContextStartedEvent;
import io.helidon.di.webserver.RouteBuilders;
import io.helidon.di.webserver.ServiceOrder;
import io.helidon.security.Security;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.webserver.WebServer;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;

@Singleton
@Requires(classes = WebServer.class)
@Internal
@Order(ServiceOrder.SECURITY)
class SecurityService implements ApplicationEventListener<ContextStartedEvent> {
    private final Config config;
    private final Security security;
    private final WebSecurity webSecurity;
    private final RouteBuilders routeBuilders;

    SecurityService(Config config, Security security, WebSecurity webSecurity, RouteBuilders routeBuilders) {
        this.config = config;
        this.security = security;
        this.webSecurity = webSecurity;
        this.routeBuilders = routeBuilders;
    }

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {
        if (!security.enabled()) {
            return;
        }

        Config webServerConfig = config.get("security.web-server");
        if (webServerConfig.exists() && webServerConfig.get("enabled").asBoolean().orElse(true)) {
            routeBuilders.routing()
                    .register(webSecurity);
        }
    }
}
