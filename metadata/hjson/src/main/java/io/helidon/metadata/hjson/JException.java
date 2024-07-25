package io.helidon.metadata.hjson;

/**
 * Exception marking a problem with JSON operations.
 */
public class JException extends RuntimeException {
    /**
     * Create a new instance with a customized message.
     *
     * @param message message to use
     */
    public JException(String message) {
        super(message);
    }
}
