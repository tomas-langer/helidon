package io.helidon.nima.faulttolerance;

import io.helidon.builder.BuilderInterceptor;

class BulkheadConfigInterceptor implements BuilderInterceptor<BulkheadConfigDefault.Builder> {
    @Override
    public BulkheadConfigDefault.Builder intercept(BulkheadConfigDefault.Builder target) {
        if (target.name().isEmpty()) {
            target.__config()
                    .ifPresent(cfg -> target.name(cfg.name()));
        }
        return target;
    }
}
