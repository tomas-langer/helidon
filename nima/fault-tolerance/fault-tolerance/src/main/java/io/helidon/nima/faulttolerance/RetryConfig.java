package io.helidon.nima.faulttolerance;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import io.helidon.builder.Builder;
import io.helidon.builder.Singular;
import io.helidon.builder.config.ConfigBean;
import io.helidon.config.metadata.ConfiguredOption;

/**
 * {@link Retry} configuration bean.
 */
@ConfigBean(value = "fault-tolerance.retries", repeatable = true, wantDefaultConfigBean = true)
@Builder(interceptor = RetryConfigInterceptor.class)
public interface RetryConfig {
    /**
     * Default calls to make.
     * @see #calls()
     */
    int DEFAULT_CALLS = 3;
    /**
     * Default delay between retries.
     * @see #delay()
     */
    Duration DEFAULT_DELAY = Duration.ofMillis(200);
    /**
     * Default overall timeout.
     * @see #overallTimeout()
     */
    Duration DEFAULT_OVERALL_TIMEOUT = Duration.ofSeconds(1);

    /**
     * Name for debugging, error reporting, monitoring.
     *
     * @return name of this retry
     */
    Optional<String> name();

    /**
     * Number of calls (first try + retries).
     *
     * @return number of desired calls, must be 1 (means no retries) or higher.
     */
    @ConfiguredOption("3")
    int calls();

    /**
     * Base delay between try and retry.
     * Defaults to {@code 200 ms}.
     *
     * @return delay between retries (combines with retry policy)
     */
    @ConfiguredOption("PT0.2S")
    Duration delay();

    /**
     * Delay retry policy factor. If unspecified (value of {@code -1}), Jitter retry policy would be used, unless
     * jitter is also unspecified.
     * <p>
     * Default when {@link Retry.DelayingRetryPolicy} is used is {@code 2}.
     *
     * @return delay factor for delaying retry policy
     */
    @ConfiguredOption("-1")
    double delayFactor();

    /**
     * Jitter for {@link Retry.JitterRetryPolicy}. If unspecified (value of {@code -1}),
     * delaying retry policy is used. If both this value, and {@link #delayFactor()} are specified, delaying retry policy
     * would be used.
     *
     * @return jitter
     */
    @ConfiguredOption("PT-1S")
    Duration jitter();

    /**
     * Overall timeout of all retries combined.
     *
     * @return overall timeout
     */
    @ConfiguredOption("PT1S")
    Duration overallTimeout();

    /**
     * These throwables will not be considered retriable, all other will.
     *
     * @return throwable classes to skip retries
     * @see #applyOn()
     */
    @Singular
    Set<Class<? extends Throwable>> skipOn();

    /**
     * These throwables will be considered retriable.
     *
     * @return throwable classes to trigger retries
     * @see #skipOn()
     */
    @Singular
    Set<Class<? extends Throwable>> applyOn();

    /**
     * Explicitly configured retry policy.
     *
     * @return retry policy
     */
    Optional<Retry.RetryPolicy> retryPolicy();
}
