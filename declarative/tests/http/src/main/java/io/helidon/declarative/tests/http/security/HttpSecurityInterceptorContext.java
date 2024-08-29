package io.helidon.declarative.tests.http.security;

class HttpSecurityInterceptorContext {
    private boolean shouldFinish;

    // tracing support
    private boolean traceSuccess = true;
    private String traceDescription;
    private Throwable traceThrowable;

    void clearTrace() {
        traceSuccess = true;
        traceDescription = null;
        traceThrowable = null;
    }

    boolean shouldFinish() {
        return shouldFinish;
    }

    void shouldFinish(boolean shouldFinish) {
        this.shouldFinish = shouldFinish;
    }

    boolean traceSuccess() {
        return traceSuccess;
    }

    void traceSuccess(boolean traceSuccess) {
        this.traceSuccess = traceSuccess;
    }

    String traceDescription() {
        return traceDescription;
    }

    void traceDescription(String traceDescription) {
        this.traceDescription = traceDescription;
    }

    Throwable traceThrowable() {
        return traceThrowable;
    }

    void traceThrowable(Throwable traceThrowable) {
        this.traceThrowable = traceThrowable;
    }
}
