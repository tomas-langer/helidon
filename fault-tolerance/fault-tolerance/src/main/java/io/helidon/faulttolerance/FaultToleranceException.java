package io.helidon.faulttolerance;

public abstract class FaultToleranceException extends RuntimeException{
    public FaultToleranceException(String message) {
        super(message);
    }

    public FaultToleranceException(String message, Throwable cause) {
        super(message, cause);
    }
}
