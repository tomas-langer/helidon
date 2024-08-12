/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
