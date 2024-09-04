package io.helidon.declarative.tests.http;

import java.util.List;

import io.helidon.service.registry.Service;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.websocket.WsRoute;
import io.helidon.webserver.websocket.WsRouting;

@Service.Provider
class WsRegistrar {
    WsRegistrar(WebServerConfig.Builder configBuilder, List<WsRoute> routes) {
        var wsRouting = WsRouting.builder();
        routes.forEach(wsRouting::route);

        configBuilder.addRouting(wsRouting);
    }
}
