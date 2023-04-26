package io.helidon.nima.faulttolerance;

import java.util.List;
import java.util.Optional;

import io.helidon.builder.Builder;
import io.helidon.builder.Singular;
import io.helidon.builder.config.ConfigBean;
import io.helidon.config.metadata.ConfiguredOption;

/**
 * {@link io.helidon.nima.faulttolerance.Bulkhead} configuration bean.
 */
@ConfigBean(value = "fault-tolerance.bulkheads", repeatable = true)
@Builder(interceptor = BulkheadConfigInterceptor.class)
public interface BulkheadConfig {
    /**
     * Default limit.
     * @see #limit();
     */
    int DEFAULT_LIMIT = 10;

    /**
     * Default queue lengths.
     * @see #queueLength()
     */
    int DEFAULT_QUEUE_LENGTH = 10;

    /**
     * Maximal number of parallel requests going through this bulkhead.
     * When the limit is reached, additional requests are enqueued.
     *
     * @return maximal number of parallel calls, defaults is {@value DEFAULT_LIMIT}
     */
    @ConfiguredOption("10")
    int limit();

    /**
     * Maximal number of enqueued requests waiting for processing.
     * When the limit is reached, additional attempts to invoke
     * a request will receive a {@link BulkheadException}.
     *
     * @return length of the queue
     */
    @ConfiguredOption("10")
    int queueLength();

    /**
     * Queue listeners of this bulkhead.
     *
     * @return queue listeners
     */
    @Singular
    List<Bulkhead.QueueListener> queueListeners();

    /**
     * Name for debugging, error reporting, monitoring.
     *
     * @return name of this bulkhead
     */
    Optional<String> name();
}
