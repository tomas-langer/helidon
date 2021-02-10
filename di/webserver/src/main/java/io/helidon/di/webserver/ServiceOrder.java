package io.helidon.di.webserver;

import io.micronaut.core.order.Ordered;

public final class ServiceOrder {
    /**
     * Server is started at a very low priority, to allow components to start later than itself.
     */
    public static final int SERVER = Ordered.LOWEST_PRECEDENCE - 1000;
    /**
     * Access log must be the first to register, as we need all requests logged.
     */
    public static final int ACCESS_LOG = Ordered.HIGHEST_PRECEDENCE;
    public static final int SECURITY = Ordered.HIGHEST_PRECEDENCE + 150;
    public static final int CORS = Ordered.HIGHEST_PRECEDENCE + 250;
    public static final int METRICS = Ordered.HIGHEST_PRECEDENCE + 350;
    public static final int HEALTH = Ordered.HIGHEST_PRECEDENCE + 450;
    public static final int STATIC_CONTENT = Ordered.HIGHEST_PRECEDENCE + 550;

    private ServiceOrder() {
    }
}
