package io.helidon.nima.faulttolerance;

import io.helidon.common.Weight;
import io.helidon.common.types.TypedElementName;
import io.helidon.pico.api.PicoServices;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named("io.helidon.nima.faulttolerance.FaultTolerance.CircuitBreaker")
@Weight(FaultTolerance.WEIGHT_CIRCUIT_BREAKER)
@Singleton
class CircuitBreakerInterceptor extends InterceptorBase<CircuitBreaker> {
    CircuitBreakerInterceptor() {
        super(PicoServices.realizedServices(), CircuitBreaker.class, FaultTolerance.CircuitBreaker.class);
    }

    @Override
    CircuitBreaker obtainHandler(TypedElementName elementInfo, InterceptorBase.CacheRecord cacheRecord) {
        return generatedMethod(CircuitBreakerMethod.class, cacheRecord)
                .map(CircuitBreakerMethod::circuitBreaker)
                .orElseGet(() -> services().lookup(CircuitBreaker.class).get());
    }
}