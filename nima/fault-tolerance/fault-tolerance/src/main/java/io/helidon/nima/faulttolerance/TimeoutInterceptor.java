package io.helidon.nima.faulttolerance;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import io.helidon.common.Weight;
import io.helidon.common.types.AnnotationAndValue;
import io.helidon.common.types.TypedElementName;
import io.helidon.pico.api.PicoServices;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named("io.helidon.nima.faulttolerance.FaultTolerance.Timeout")
@Weight(FaultTolerance.WEIGHT_TIMEOUT)
@Singleton
class TimeoutInterceptor extends InterceptorBase<Timeout> {
    TimeoutInterceptor() {
        super(PicoServices.realizedServices(), Timeout.class, FaultTolerance.Timeout.class);
    }

    @Override
    Timeout obtainHandler(TypedElementName elementInfo, CacheRecord cacheRecord) {
        return namedHandler(elementInfo, this::fromAnnotation);
    }

    private Timeout fromAnnotation(AnnotationAndValue annotation) {
        String name = annotation.value("name").orElse("timeout-") + System.identityHashCode(annotation);
        long timeout = annotation.value("time").map(Long::parseLong).orElse(10L);
        ChronoUnit unit = annotation.value("timeUnit").map(ChronoUnit::valueOf).orElse(ChronoUnit.SECONDS);
        boolean currentThread = annotation.value("currentThread").map(Boolean::parseBoolean).orElse(false);

        return io.helidon.nima.faulttolerance.Timeout.create(TimeoutConfigDefault.builder()
                                      .name(name)
                                      .timeout(Duration.of(timeout, unit))
                                      .currentThread(currentThread)
                                      .build());
    }
}
