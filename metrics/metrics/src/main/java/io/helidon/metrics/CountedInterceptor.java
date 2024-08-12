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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MeterRegistry;
import io.helidon.metrics.api.Metrics;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Tag;
import io.helidon.service.inject.api.Injection;
import io.helidon.service.inject.api.Interception;
import io.helidon.service.inject.api.InvocationContext;

@Injection.Singleton
@Injection.NamedByClass(Metrics.Counted.class)
@Weight(Weighted.DEFAULT_WEIGHT - 10)
class CountedInterceptor implements Interception.Interceptor {
    private static final TypeName COUNTED = TypeName.create(Metrics.Counted.class);
    private static final Annotation DEFAULTS = Annotation.builder()
            .typeName(COUNTED)
            .putValue("value", "")
            .putValue("absoluteName", false)
            .putValue("description", "")
            .putValue("tags", List.of())
            .putValue("applyOn", Metrics.ApplyOn.ALL)
            .putValue("scope", Meter.Scope.APPLICATION)
            .build();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<CacheKey, CounterData> counters = new HashMap<>();
    private final LazyValue<MetricsFactory> metricsFactory = LazyValue.create(MetricsFactory::getInstance);
    private final LazyValue<MeterRegistry> meterRegistry = LazyValue.create(() -> metricsFactory.get().globalRegistry());

    @Override
    public <V> V proceed(InvocationContext ctx, Chain<V> chain, Object... args) throws Exception {
        CounterData counter = counter(ctx);

        try {
            var response = chain.proceed(args);
            if (counter.countSuccess) {
                counter.counter.increment();
            }
            return response;
        } catch (Throwable t) {
            if (counter.countThrown) {
                counter.counter.increment();
            }
            throw t;
        }
    }

    private CounterData counter(InvocationContext ctx) {

        TypedElementInfo typedElementInfo = ctx.elementInfo();
        CacheKey ck = new CacheKey(ctx.serviceInfo().serviceType(),
                                   typedElementInfo.elementName(),
                                   typedElementInfo.parameterArguments());
        lock.readLock().lock();
        try {
            CounterData counterData = counters.get(ck);
            if (counterData != null) {
                return counterData;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            CounterData counterData = createCounter(ck, ctx, typedElementInfo);
            counters.put(ck, counterData);
            return counterData;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // todo right now it cannot be a repeated annotation
    private CounterData createCounter(CacheKey ck, InvocationContext ctx, TypedElementInfo typedElementInfo) {
        // first check if there is a method annotation
        Optional<Annotation> methodCounted = typedElementInfo.findAnnotation(COUNTED);
        Annotation typeCounted = MetricInterceptors.findAnnotation(ctx.typeAnnotations(), COUNTED).orElse(DEFAULTS);

        String name = MetricInterceptors.namePrefix(ck.clazz(), typeCounted);
        if (methodCounted.isPresent()) {
            name = MetricInterceptors.metricName(name, ck.methodName(), methodCounted.get(), Meter.Type.COUNTER);
        }
        Annotation relevant = methodCounted.orElse(typeCounted);

        String description = relevant.stringValue("description").orElse("Counter for method " + ck.methodName);
        String scope = relevant.stringValue("scope").orElse(Meter.Scope.APPLICATION);
        Metrics.ApplyOn applyOn = relevant.enumValue("applyOn", Metrics.ApplyOn.class).orElse(Metrics.ApplyOn.ALL);
        Iterable<Tag> tags = Metrics.tags(relevant.stringValues("tags").orElseGet(List::of).toArray(new String[0]));

        return new CounterData(name,
                               applyOn == Metrics.ApplyOn.ALL || applyOn == Metrics.ApplyOn.FAILURE,
                               applyOn == Metrics.ApplyOn.ALL || applyOn == Metrics.ApplyOn.SUCCESS,
                               meterRegistry.get()
                                       .getOrCreate(Counter.builder(name)
                                                            .description(description)
                                                            .scope(scope)
                                                            .tags(tags)));
    }

    record CacheKey(TypeName clazz, String methodName, List<TypedElementInfo> parameters) {
    }

    record CounterData(String counterName,
                       boolean countThrown,
                       boolean countSuccess,
                       Counter counter) {

    }
}
