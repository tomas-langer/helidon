package io.helidon.declarative.tests.http;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.buffers.BufferData;
import io.helidon.common.parameters.Parameters;
import io.helidon.http.Headers;
import io.helidon.http.HttpPrologue;
import io.helidon.http.PathMatcher;
import io.helidon.http.PathMatchers;
import io.helidon.service.inject.api.Injection;
import io.helidon.service.registry.Service;
import io.helidon.webserver.websocket.WsRoute;
import io.helidon.websocket.WebSocket;
import io.helidon.websocket.WsCloseCodes;
import io.helidon.websocket.WsCloseException;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;

@Injection.Singleton
public class WsGeneratedRoute implements Service.InstanceProvider<WsRoute> {
    private final Supplier<WsChatEndpoint> endpoint;

    WsGeneratedRoute(Supplier<WsChatEndpoint> endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public WsRoute get() {
        return WsRoute.create("/ws/{name}",
                              new GeneratedListener(endpoint.get()));
    }

    private static class GeneratedListener implements WsListener {
        private final WsChatEndpoint wsChatEndpoint;
        private final PathMatcher pathMatcher;

        private volatile String username;
        private StringBuilder messageBuilder = new StringBuilder();

        private GeneratedListener(WsChatEndpoint wsChatEndpoint) {
            this.wsChatEndpoint = wsChatEndpoint;
            this.pathMatcher = PathMatchers.create("/ws/{name}");
        }

        @Override
        public void onMessage(WsSession session, String text, boolean last) {
            messageBuilder.append(text);
            if (last) {
                wsChatEndpoint.onMessage(session, new WsChatEndpoint.Message(messageBuilder.toString()));
            }
        }

        @Override
        public void onMessage(WsSession session, BufferData buffer, boolean last) {
            throw new WsCloseException("Only text messages supported", WsCloseCodes.CANNOT_ACCEPT);
        }

        @Override
        public void onClose(WsSession session, int status, String reason) {
            wsChatEndpoint.onClose(session, WebSocket.CloseReason.create(status, reason));
        }

        @Override
        public void onError(WsSession session, Throwable t) {
            wsChatEndpoint.onError(session, t);
        }

        @Override
        public void onOpen(WsSession session) {
            wsChatEndpoint.onOpen(session, username);
        }

        @Override
        public Optional<Headers> onHttpUpgrade(HttpPrologue prologue, Headers headers) {
            Parameters parameters = pathMatcher.match(prologue.uriPath())
                    .path()
                    .pathParameters();
            this.username = parameters.get("username");

            return Optional.empty();
        }
    }
}
