package io.helidon.examples.quickstart.se;

import java.util.Objects;

import io.helidon.http.Status;

/**
 * A business exception class mapped to responses by {@link io.helidon.examples.quickstart.se.QuickstartErrorHandler}.
 */
class QuickstartException extends RuntimeException {
    private final Status status;

    QuickstartException(Status status, String message) {
        super(Objects.requireNonNull(message));

        this.status = Objects.requireNonNull(status);
    }

    Status status() {
        return status;
    }
}
