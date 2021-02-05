package io.helidon.mn.webserver;

import java.util.Optional;

import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public interface HttpExchange {
    ServerRequest request();
    ServerResponse response();
    default Optional<Object> entity() {
        return Optional.empty();
    }
    default Optional<Throwable> throwable() {
        return Optional.empty();
    }

    static HttpExchange create(ServerRequest req, ServerResponse res) {
        return new HttpExchange() {
            @Override
            public ServerRequest request() {
                return req;
            }

            @Override
            public ServerResponse response() {
                return res;
            }
        };
    }

    static HttpExchange create(ServerRequest req, ServerResponse res, Object entity) {
        return new HttpExchange() {
            @Override
            public ServerRequest request() {
                return req;
            }

            @Override
            public ServerResponse response() {
                return res;
            }

            @Override
            public Optional<Object> entity() {
                return Optional.of(entity);
            }
        };
    }

    static HttpExchange create(ServerRequest req, ServerResponse res, Throwable throwable) {
        return new HttpExchange() {
            @Override
            public ServerRequest request() {
                return req;
            }

            @Override
            public ServerResponse response() {
                return res;
            }

            @Override
            public Optional<Throwable> throwable() {
                return Optional.of(throwable);
            }
        };
    }
}
