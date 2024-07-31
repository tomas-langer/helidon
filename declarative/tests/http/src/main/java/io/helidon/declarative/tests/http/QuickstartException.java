package io.helidon.declarative.tests.http;

import java.util.Objects;

import io.helidon.http.Status;

/**
 * A business exception class mapped to responses by {@link QuickstartErrorHandler}.
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
