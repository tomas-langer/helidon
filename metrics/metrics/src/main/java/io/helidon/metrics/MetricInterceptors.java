package io.helidon.metrics;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;
import io.helidon.metrics.api.Meter;

final class MetricInterceptors {
    private MetricInterceptors() {
    }

    static Optional<Annotation> findAnnotation(List<Annotation> annotations, TypeName type) {
        for (Annotation annotation : annotations) {
            if (type.equals(annotation.typeName())) {
                return Optional.of(annotation);
            }
        }
        return Optional.empty();
    }

    static String metricName(String prefix, String methodName, Annotation annotation, Meter.Type type) {
        Optional<String> value = annotation.stringValue("value").filter(Predicate.not(String::isEmpty));
        if (value.isPresent()) {
            boolean absolute = annotation.booleanValue("absoluteName").orElse(false);
            return absolute ? value.get() : prefix + value.get();
        }

        return prefix + methodName + "." + type.typeName();
    }

    static String namePrefix(TypeName clazz, Annotation annotation) {
        Optional<String> namePrefix = annotation.stringValue("value");
        if (namePrefix.filter(Predicate.not(String::isEmpty)).isPresent()) {
            return namePrefix.get() + ".";
        }
        return clazz.fqName() + ".";
    }
}
