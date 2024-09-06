package io.helidon.webserver.websocket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * WebSocket server related annotation and Helidon Declarative types.
 */
public class WebSocketServer {
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    public @interface Endpoint {
        String[] subProtocols() default {};

    }
}
