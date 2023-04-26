package io.helidon.nima.faulttolerance;

import io.helidon.pico.api.Contract;

/**
 * A generated service to support circuit breaker without resorting to Class.forName() for exception types.
 * @deprecated only for generated code
 */
@Contract
@Deprecated
public interface CircuitBreakerMethod extends FtMethod {
    /**
     * Provide a circuit breaker instance that should be used with this method.
     * If the {@link io.helidon.nima.faulttolerance.FaultTolerance.CircuitBreaker} annotation contains a name, we will attempt to obtain the named instance from
     * registry. If such a named instance does not exist a new circuit breaker will be created from the annotation.
     *
     * @return circuit breaker instance
     */
    CircuitBreaker circuitBreaker();
}
