package io.helidon.examples.di.basics;

import io.helidon.common.Reflected;

@Reflected
public class ErrorObject {
    private final String message;
    private final String cause;

    ErrorObject(String message, String cause) {
        this.message = message;
        this.cause = cause;
    }

    public String getMessage() {
        return message;
    }

    public String getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "ErrorObject{" +
                "message='" + message + '\'' +
                ", cause='" + cause + '\'' +
                '}';
    }

    public static ErrorObject create(Throwable error) {
        Throwable rootCause = rootCause(error);

        if (rootCause == null) {
            return new ErrorObject(error.getMessage(), "unknown");
        } else {
            return new ErrorObject(error.getMessage(), rootCause.getMessage());
        }
    }

    private static Throwable rootCause(Throwable error) {
        Throwable base = error;
        Throwable cause;
        Throwable lastCause = null;

        while (true) {
            cause = base.getCause();
            if (cause != null && cause != base) {
                // real cause of the exception
                lastCause = cause;
                base = cause;
            } else {
                break;
            }
        }

        return lastCause;
    }
}
