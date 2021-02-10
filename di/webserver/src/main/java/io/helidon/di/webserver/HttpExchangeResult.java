package io.helidon.di.webserver;

import java.util.Optional;

import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public interface HttpExchangeResult extends HttpExchange {
    Object methodReturn();

    static HttpExchangeResult create(HttpExchange exchange, Object methodReturn) {
        return new HttpExchangeResult() {
            @Override
            public ServerRequest request() {
                return exchange.request();
            }

            @Override
            public ServerResponse response() {
                return exchange.response();
            }

            @Override
            public Optional<Object> entity() {
                return exchange.entity();
            }

            @Override
            public Object methodReturn() {
                return methodReturn;
            }
        };
    }
}
