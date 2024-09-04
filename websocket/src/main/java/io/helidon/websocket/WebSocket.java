package io.helidon.websocket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * WebSocket related annotations and other Helidon declarative related types.
 */
public final class WebSocket {
    private WebSocket() {
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    public @interface OnOpen {
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    public @interface OnMessage {
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    public @interface OnError {
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    public @interface MaxMessageSize {
        long value();
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    public @interface OnClose {
    }

    public interface CloseReason {
        static CloseReason create(int code, String reason) {
            return new CloseReason() {
                @Override
                public int code() {
                    return code;
                }

                @Override
                public String reason() {
                    return reason;
                }
            };
        }
        int code();
        String reason();
    }
}
