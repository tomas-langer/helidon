package io.helidon.inject;

import io.helidon.inject.service.Injection;

@Injection.Singleton
class RequestonControlImpl implements RequestonControl {
    private static final ThreadLocal<Scope> REQUEST_SCOPES = new ThreadLocal<>();

    /**
     * Start request scope.
     */
    Scope startRequestScope();
}
