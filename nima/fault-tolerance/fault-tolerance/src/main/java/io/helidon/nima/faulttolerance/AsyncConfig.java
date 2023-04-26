package io.helidon.nima.faulttolerance;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import io.helidon.builder.Builder;
import io.helidon.builder.config.ConfigBean;
import io.helidon.config.metadata.ConfiguredOption;

/**
 * {@link io.helidon.nima.faulttolerance.Async} configuration bean.
 */
@ConfigBean(value = "fault-tolerance.asyncs", repeatable = true)
@Builder(interceptor = AsyncConfigInterceptor.class)
public interface AsyncConfig {
    /**
     * Name for debugging, error reporting, monitoring.
     *
     * @return name of this async
     */
    Optional<String> name();

    /**
     * Name of an executor service. This is only honored when service registry is used.
     *
     * @return name fo the {@link java.util.concurrent.ExecutorService} to lookup.
     * @see #executor()
     */
    @ConfiguredOption
    Optional<String> executorName();

    /**
     * Executor service. Will be used to run the asynchronous tasks.
     *
     * @return explicit executor service
     */
    Optional<ExecutorService> executor();

    Optional<CompletableFuture<Async>> onStart();
}
