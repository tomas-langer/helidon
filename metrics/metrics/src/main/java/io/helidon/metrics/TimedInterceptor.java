package io.helidon.metrics;

import java.time.Duration;
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
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MeterRegistry;
import io.helidon.metrics.api.Metrics;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Tag;
import io.helidon.metrics.api.Timer;
import io.helidon.service.inject.api.Injection;
import io.helidon.service.inject.api.Interception;
import io.helidon.service.inject.api.InvocationContext;

@Injection.Singleton
@Injection.NamedByClass(Metrics.Timed.class)
@Weight(Weighted.DEFAULT_WEIGHT - 10)
class TimedInterceptor implements Interception.Interceptor {
    private static final TypeName TIMED = TypeName.create(Metrics.Timed.class);
    private static final Annotation DEFAULTS = Annotation.builder()
            .typeName(TIMED)
            .putValue("value", "")
            .putValue("absoluteName", false)
            .putValue("description", "")
            .putValue("tags", List.of())
            .putValue("applyOn", Metrics.ApplyOn.ALL)
            .putValue("scope", Meter.Scope.APPLICATION)
            .putValue("unit", Meter.BaseUnits.NANOSECONDS)
            .build();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<CacheKey, TimerData> timers = new HashMap<>();
    private final LazyValue<MetricsFactory> metricsFactory = LazyValue.create(MetricsFactory::getInstance);
    private final LazyValue<MeterRegistry> meterRegistry = LazyValue.create(() -> metricsFactory.get().globalRegistry());

    @Override
    public <V> V proceed(InvocationContext ctx, Chain<V> chain, Object... args) throws Exception {
        TimerData timer = timer(ctx);

        long now = System.nanoTime();
        try {
            var response = chain.proceed(args);
            if (timer.countSuccess) {
                timer.timer().record(Duration.ofNanos(System.nanoTime() - now));
            }
            return response;
        } catch (Throwable t) {
            if (timer.countThrown) {
                timer.timer().record(Duration.ofNanos(System.nanoTime() - now));
            }
            throw t;
        }
    }

    private TimerData timer(InvocationContext ctx) {

        TypedElementInfo typedElementInfo = ctx.elementInfo();
        CacheKey ck = new CacheKey(ctx.serviceInfo().serviceType(),
                                   typedElementInfo.elementName(),
                                   typedElementInfo.parameterArguments());
        lock.readLock().lock();
        try {
            TimerData timerData = timers.get(ck);
            if (timerData != null) {
                return timerData;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            TimerData timerData = createTimer(ck, ctx, typedElementInfo);
            timers.put(ck, timerData);
            return timerData;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // todo right now it cannot be a repeated annotation
    private TimerData createTimer(CacheKey ck, InvocationContext ctx, TypedElementInfo typedElementInfo) {
        // first check if there is a method annotation
        Optional<Annotation> methodTimed = typedElementInfo.findAnnotation(TIMED);
        Annotation typeTimed = MetricInterceptors.findAnnotation(ctx.typeAnnotations(), TIMED).orElse(DEFAULTS);

        String name = MetricInterceptors.namePrefix(ck.clazz(), typeTimed);
        if (methodTimed.isPresent()) {
            name = MetricInterceptors.metricName(name, ck.methodName(), methodTimed.get(), Meter.Type.TIMER);
        }

        Annotation relevant = methodTimed.orElse(typeTimed);

        String description = relevant.stringValue("description").orElse("Counter for method " + ck.methodName);
        String scope = relevant.stringValue("scope").orElse(Meter.Scope.APPLICATION);
        Metrics.ApplyOn applyOn = relevant.enumValue("applyOn", Metrics.ApplyOn.class).orElse(Metrics.ApplyOn.ALL);
        Iterable<Tag> tags = Metrics.tags(relevant.stringValues("tags").orElseGet(List::of).toArray(new String[0]));
        String unit = relevant.stringValue("unit").orElse(Meter.BaseUnits.NANOSECONDS);

        return new TimerData(name,
                             applyOn == Metrics.ApplyOn.ALL || applyOn == Metrics.ApplyOn.FAILURE,
                             applyOn == Metrics.ApplyOn.ALL || applyOn == Metrics.ApplyOn.SUCCESS,
                             meterRegistry.get()
                                     .getOrCreate(Timer.builder(name)
                                                          .description(description)
                                                          .scope(scope)
                                                          .baseUnit(unit)
                                                          .tags(tags)));
    }

    record CacheKey(TypeName clazz, String methodName, List<TypedElementInfo> parameters) {
    }

    record TimerData(String name,
                     boolean countThrown,
                     boolean countSuccess,
                     Timer timer) {

    }
}
