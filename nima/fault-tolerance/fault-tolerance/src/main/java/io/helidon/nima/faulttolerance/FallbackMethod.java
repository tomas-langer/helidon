package io.helidon.nima.faulttolerance;

import io.helidon.pico.api.Contract;

/**
 * A generated service to support fallback without reflection.
 *
 * @param <T> type of the response of the method
 * @param <S> type of the service that hosts the method
 * @deprecated only for generated code
 */
@Contract
@Deprecated
public interface FallbackMethod<T, S> extends FtMethod {
    /**
     * Fallback method generated based on the {@link io.helidon.nima.faulttolerance.FaultTolerance.Fallback} annotation.
     * This generated type will check if the throwable should be handled or not, and either throws it, or executes the fallback.
     *
     * @param throwable throwable thrown by the original code (if check, it is wrapped in a runtime exception)
     * @param arguments original arguments to the method
     * @return result obtained from the fallback method (or throws the throwable if fallback should not be done)
     */
    T fallback(S service, Throwable throwable, Object... arguments) throws Throwable;
}
