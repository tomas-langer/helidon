package io.helidon.declarative.tests.http.websocket;

import java.util.List;

import io.helidon.service.registry.Service;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.WebServerRegistryService;
import io.helidon.webserver.websocket.WsRoute;
import io.helidon.webserver.websocket.WsRouting;

@Service.Provider
class WsRegistrar implements WebServerRegistryService {
    private final List<WsRoute> routes;

    WsRegistrar(List<WsRoute> routes) {
        this.routes = routes;
    }

    @Override
    public void updateBuilder(WebServerConfig.BuilderBase<?, ?> builder) {
        var wsRouting = WsRouting.builder();
        routes.forEach(wsRouting::route);

        builder.addRouting(wsRouting);
    }
}
