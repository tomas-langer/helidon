/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.di.metrics;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.ExecutableMethod;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

/**
 * Base of metrics interceptors.
 *
 * @param <T> metric type
 * @param <A> annotation type
 */
abstract class InterceptorBase<T extends Metric, A extends Annotation> implements MethodInterceptor<Object, Object> {
    private final Map<ExecutableMethod<Object, Object>, T> METRIC_CACHE = new ConcurrentHashMap<>();

    private final MetricRegistry registry;
    private final Class<A> annotationClass;
    private final Class<T> metricClass;
    private final MetricType metricType;

    /**
     * Provide information needed to lookup a metric.
     *
     * @param registry registry to use
     * @param metricClass metric interface
     * @param annotationClass metric annotation
     * @param metricType type of metric
     */
    InterceptorBase(MetricRegistry registry,
                    Class<T> metricClass,
                    Class<A> annotationClass,
                    MetricType metricType) {
        this.registry = registry;
        this.metricClass = metricClass;
        this.annotationClass = annotationClass;
        this.metricType = metricType;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        AnnotationValue<A> annotation = context.getAnnotation(annotationClass);

        T metric = findMetric(context.getExecutableMethod(), annotation);

        try {
            Object response = prepareAndInvoke(metric, annotation, context);
            postInvoke(metric, annotation, context, response, null);
            return response;
        } catch (Exception e) {
            postInvoke(metric, annotation, context, null, e);
            throw e;
        }
    }

    /**
     * Logic to be run after invocation.
     *
     * @param metric metric instance
     * @param annotValue annotation value
     * @param context execution context
     * @param result actual result of execution (may be null)
     * @param thrown exception if one was thrown (may be null)
     */
    protected void postInvoke(T metric,
                              AnnotationValue<A> annotValue,
                              MethodInvocationContext<Object, Object> context,
                              Object result,
                              Exception thrown) {
    }

    /**
     * Logic to be run before invocation and invoke the method.
     *
     * @param metric metric instance
     * @param annotValue annotation value
     * @param context execution context
     * @return result of the call
     */
    protected abstract Object prepareAndInvoke(T metric,
                                               AnnotationValue<A> annotValue,
                                               MethodInvocationContext<Object, Object> context);

    private T findMetric(ExecutableMethod<Object, Object> executableMethod,
                         AnnotationValue<A> annotation) {
        return METRIC_CACHE.computeIfAbsent(executableMethod, it -> {
            String name = annotation.stringValue("name").orElse("");
            boolean absolute = annotation.booleanValue("absolute").orElse(false);
            String displayName = annotation.stringValue("displayName").orElse("");
            String description = annotation.stringValue("description").orElse("");
            String unit = annotation.stringValue("unit").orElse(MetricUnits.NANOSECONDS);
            boolean reusable = annotation.booleanValue("reusable").orElse(false);
            Tag[] tags = toTags(annotation.stringValues("tags"));
            name = fixName(name, absolute, executableMethod);
            displayName = displayName.isBlank() ? name : displayName;

            Metadata meta = Metadata.builder()
                    .withName(name)
                    .withDisplayName(displayName)
                    .withDescription(description)
                    .withType(metricType)
                    .withUnit(unit)
                    .reusable(reusable)
                    .build();

            switch (metricType) {
            case TIMER:
                return metricClass.cast(registry.timer(meta, tags));
            case COUNTER:
                return metricClass.cast(registry.counter(meta, tags));
            case CONCURRENT_GAUGE:
                return metricClass.cast(registry.concurrentGauge(meta, tags));
            case METERED:
                return metricClass.cast(registry.meter(meta, tags));
            default:
                throw new IllegalStateException("Metric type " + metricType + " is not supported for interceptors");
            }
        });
    }

    private Tag[] toTags(String[] tagStrings) {
        List<Tag> result = new ArrayList<>(tagStrings.length);
        for (String tagString : tagStrings) {
            final int eq = tagString.indexOf("=");
            if (eq > 0) {
                final String tagName = tagString.substring(0, eq);
                final String tagValue = tagString.substring(eq + 1);
                result.add(new Tag(tagName, tagValue));
            }
        }
        return result.toArray(new Tag[0]);
    }

    private String fixName(String name, boolean absolute, ExecutableMethod<Object, Object> executableMethod) {
        if (name.isBlank()) {
            return executableMethod.getDeclaringType().getName() + '.' + executableMethod.getMethodName();
        } else {
            if (absolute) {
                return name;
            } else {
                // prefix with class name
                return executableMethod.getDeclaringType().getName() + '.' + name;
            }
        }
    }
}
