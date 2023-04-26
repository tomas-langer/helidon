package io.helidon.nima.faulttolerance;

import io.helidon.builder.BuilderInterceptor;

class CircuitBreakerConfigInterceptor implements BuilderInterceptor<CircuitBreakerConfigDefault.Builder> {
    @Override
    public CircuitBreakerConfigDefault.Builder intercept(CircuitBreakerConfigDefault.Builder target) {
        if (target.name().isEmpty()) {
            target.__config()
                    .ifPresent(cfg -> target.name(cfg.name()));
        }

        return target;
    }
}
