package io.helidon.nima.faulttolerance;

import io.helidon.builder.BuilderInterceptor;

class TimeoutConfigInterceptor implements BuilderInterceptor<TimeoutConfigDefault.Builder> {
    @Override
    public TimeoutConfigDefault.Builder intercept(TimeoutConfigDefault.Builder target) {
        if (target.name().isEmpty()) {
            target.__config()
                    .ifPresent(cfg -> target.name(cfg.name()));
        }

        return target;
    }
}
