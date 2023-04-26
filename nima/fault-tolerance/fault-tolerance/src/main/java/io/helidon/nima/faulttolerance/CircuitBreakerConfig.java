package io.helidon.nima.faulttolerance;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import io.helidon.builder.Builder;
import io.helidon.builder.Singular;
import io.helidon.builder.config.ConfigBean;
import io.helidon.config.metadata.ConfiguredOption;

@ConfigBean(value = "fault-tolerance.circuit-breaker", repeatable = true)
@Builder(interceptor = CircuitBreakerConfigInterceptor.class)
public interface CircuitBreakerConfig {
    Duration DEFAULT_DELAY = Duration.ofSeconds(5);
    int DEFAULT_ERROR_RATIO = 60;
    int DEFAULT_SUCCESS_THRESHOLD = 1;
    int DEFAULT_VOLUME = 10;

    Optional<String> name();

    /**
     * How long to wait before transitioning from open to half-open state.
     *
     * @return delay
     */
    @ConfiguredOption("PT5S")
    Duration delay();

    /**
     * How many failures out of 100 will trigger the circuit to open.
     * This is adapted to the {@link #volume()} used to handle the window of requests.
     * <p>If errorRatio is 40, and volume is 10, 4 failed requests will open the circuit.
     * Default is {@value #DEFAULT_ERROR_RATIO}.
     *
     * @return percent of failure that trigger the circuit to open
     * @see #volume()
     */
    @ConfiguredOption("60")
    int errorRatio();

    /**
     * Rolling window size used to calculate ratio of failed requests.
     * Default is {@value #DEFAULT_VOLUME}.
     *
     * @return how big a window is used to calculate error errorRatio
     * @see #errorRatio()
     */
    @ConfiguredOption("10")
    int volume();

    /**
     * How many successful calls will close a half-open circuit.
     * Nevertheless, the first failed call will open the circuit again.
     * Default is {@value #DEFAULT_SUCCESS_THRESHOLD}.
     *
     * @return number of calls
     */
    @ConfiguredOption("1")
    int successThreshold();

    /**
     * Executor service to schedule future tasks.
     *
     * @return executor to use
     */
    Optional<ExecutorService> executor();

    /**
     * These throwables will not be considered failures, all other will.
     *
     * @return throwable classes to not be considered a failure
     * @see #applyOn()
     */
    @Singular
    Set<Class<? extends Throwable>> skipOn();

    /**
     * These throwables will be considered failures.
     *
     * @return throwable classes to be considered a failure
     * @see #skipOn()
     */
    @Singular
    Set<Class<? extends Throwable>> applyOn();
}
