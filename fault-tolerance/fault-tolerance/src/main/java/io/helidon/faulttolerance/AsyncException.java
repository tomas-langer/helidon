package io.helidon.faulttolerance;

class AsyncException extends RuntimeException {
    public AsyncException(Throwable cause) {
        super(cause);
    }
}
