package io.helidon.nima.faulttolerance;

import io.helidon.builder.BuilderInterceptor;

class RetryConfigInterceptor implements BuilderInterceptor<RetryConfigDefault.Builder> {
    @Override
    public RetryConfigDefault.Builder intercept(RetryConfigDefault.Builder target) {
        if (target.name().isEmpty()) {
            target.__config()
                    .ifPresent(cfg -> target.name(cfg.name()));
        }
        if (target.retryPolicy().isEmpty()) {
            target.retryPolicy(retryPolicy(target));
        }
        return target;
    }

    /**
     * Retry policy created from this configuration.
     *
     * @return retry policy to use
     */
    private Retry.RetryPolicy retryPolicy(RetryConfigDefault.Builder target) {
        if (target.jitter().toSeconds() == -1) {
            Retry.DelayingRetryPolicy.Builder delayBuilder = Retry.DelayingRetryPolicy.builder()
                    .calls(target.calls())
                    .delay(target.delay());

            if (target.delayFactor() != -1) {
                delayBuilder.delayFactor(target.delayFactor());
            }
            return delayBuilder.build();
        }
        if (target.delayFactor() != -1) {
            return Retry.DelayingRetryPolicy.builder()
                    .calls(target.calls())
                    .delayFactor(target.delayFactor())
                    .delay(target.delay())
                    .build();
        }
        return Retry.JitterRetryPolicy.builder()
                .calls(target.calls())
                .delay(target.delay())
                .jitter(target.jitter())
                .build();
    }
}
