package io.helidon.nima.faulttolerance;

import io.helidon.builder.BuilderInterceptor;

class AsyncConfigInterceptor implements BuilderInterceptor<AsyncConfigDefault.Builder> {
    @Override
    public AsyncConfigDefault.Builder intercept(AsyncConfigDefault.Builder target) {
        if (target.name().isEmpty()) {
            target.__config()
                    .ifPresent(cfg -> target.name(cfg.name()));
        }

        return target;
    }
}
