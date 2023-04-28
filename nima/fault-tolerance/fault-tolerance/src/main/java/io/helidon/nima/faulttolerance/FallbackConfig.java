package io.helidon.nima.faulttolerance;

import java.util.Set;
import java.util.function.Function;

import io.helidon.builder.Builder;
import io.helidon.builder.Singular;

/**
 * {@link io.helidon.nima.faulttolerance.Fallback} configuration.
 */
public interface FallbackConfig<T> {
    /**
     * A fallback function.
     *
     * @return fallback function to obtain alternative result
     */
    Function<Throwable, ? extends T> fallback();

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
}
