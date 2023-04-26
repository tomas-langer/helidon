package io.helidon.nima.faulttolerance.processor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.common.types.TypeName;

abstract class FtMethodCreatorBase {
    private static final Map<CacheRecord, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();

    protected String className(TypeName annotationType, TypeName enclosingType, String methodName) {
        AtomicInteger counter = COUNTERS.computeIfAbsent(new CacheRecord(annotationType, enclosingType, methodName),
                                                          it -> new AtomicInteger());

        // package.TypeName_AnnotationName_Counter
        return enclosingType.className().replace('.', '_') + "_"
                + annotationType.className().replace('.', '_') + "_"
                + methodName + "_"
                + counter.incrementAndGet();
    }

    private record CacheRecord(TypeName annotation, TypeName enclosingType, String meethodName) {
    }
}
