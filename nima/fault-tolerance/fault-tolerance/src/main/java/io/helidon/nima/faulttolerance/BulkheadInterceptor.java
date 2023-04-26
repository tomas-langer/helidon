package io.helidon.nima.faulttolerance;

import io.helidon.common.Weight;
import io.helidon.common.types.AnnotationAndValue;
import io.helidon.common.types.TypedElementName;
import io.helidon.pico.api.PicoServices;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named("io.helidon.nima.faulttolerance.FaultTolerance.Bulkhead")
@Weight(FaultTolerance.WEIGHT_BULKHEAD)
@Singleton
class BulkheadInterceptor extends InterceptorBase<Bulkhead> {
    BulkheadInterceptor() {
        super(PicoServices.realizedServices(), Bulkhead.class, FaultTolerance.Bulkhead.class);
    }

    @Override
    Bulkhead obtainHandler(TypedElementName elementInfo, InterceptorBase.CacheRecord cacheRecord) {
        return namedHandler(elementInfo, this::fromAnnotation);
    }

    private Bulkhead fromAnnotation(AnnotationAndValue annotation) {
        int limit = annotation.value("limit").map(Integer::parseInt).orElse(BulkheadConfig.DEFAULT_LIMIT);
        int queueLength = annotation.value("queueLength").map(Integer::parseInt).orElse(BulkheadConfig.DEFAULT_QUEUE_LENGTH);
        String name = annotation.value("name").orElse("bulkhead-") + System.identityHashCode(annotation);

        return io.helidon.nima.faulttolerance.Bulkhead.create(BulkheadConfigDefault.builder()
                                       .name(name)
                                       .queueLength(queueLength)
                                       .limit(limit)
                                       .build());
    }
}
