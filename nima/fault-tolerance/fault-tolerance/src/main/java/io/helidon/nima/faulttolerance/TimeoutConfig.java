package io.helidon.nima.faulttolerance;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import io.helidon.builder.Builder;
import io.helidon.builder.config.ConfigBean;
import io.helidon.config.metadata.ConfiguredOption;

/**
 * {@link io.helidon.nima.faulttolerance.Timeout} configuration bean.
 */
@ConfigBean(value = "fault-tolerance.timeouts", wantDefaultConfigBean = true, repeatable = true)
@Builder(interceptor = TimeoutConfigInterceptor.class)
public interface TimeoutConfig {
    /**
     * Name for debugging, error reporting, monitoring.
     *
     * @return name of this timeout
     */
    Optional<String> name();

    /**
     * Duration to wait before timing out.
     * Defaults to {@code 10 seconds}.
     *
     * @return timeout
     */
    @ConfiguredOption("PT10S")
    Duration timeout();

    /**
     * Flag to indicate that code must be executed in current thread instead
     * of in an executor's thread. This flag is {@code false} by default.
     *
     * @return  whether to execute on current thread ({@code true}), or in an executor service ({@code false}})
     */
    @ConfiguredOption("false")
    boolean currentThread();

    /**
     * Executor service to schedule the timeout.
     *
     * @return executor service to use
     */
    Optional<ExecutorService> executor();
}
