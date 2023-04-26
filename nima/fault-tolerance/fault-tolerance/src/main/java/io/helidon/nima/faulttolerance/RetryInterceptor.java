package io.helidon.nima.faulttolerance;

import io.helidon.common.Weight;
import io.helidon.common.types.TypedElementName;
import io.helidon.pico.api.PicoServices;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named("io.helidon.nima.faulttolerance.FaultTolerance.Retry")
@Weight(FaultTolerance.WEIGHT_RETRY)
@Singleton
class RetryInterceptor extends InterceptorBase<Retry> {
    RetryInterceptor() {
        super(PicoServices.realizedServices(), Retry.class, FaultTolerance.Retry.class);
    }

    @Override
    Retry obtainHandler(TypedElementName elementInfo, InterceptorBase.CacheRecord cacheRecord) {
        return super.generatedMethod(RetryMethod.class, cacheRecord)
                .map(RetryMethod::retry)
                .orElseGet(() -> services().lookup(Retry.class).get());
    }
}
