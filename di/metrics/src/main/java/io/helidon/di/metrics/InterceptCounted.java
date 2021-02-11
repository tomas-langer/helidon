package io.helidon.di.metrics;

import javax.inject.Named;
import javax.inject.Singleton;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.Counted;

@Singleton
@Internal
class InterceptCounted extends InterceptorBase<Counter, Counted> {

    /**
     * Create a new interceptor using the application metrics registry.
     *
     * @param metricRegistry registry to create counters in
     */
    InterceptCounted(@Named("application") MetricRegistry metricRegistry) {
        super(metricRegistry,
              Counter.class,
              Counted.class,
              MetricType.COUNTER);
    }

    @Override
    protected Object prepareAndInvoke(Counter metric,
                                      AnnotationValue<Counted> annotValue,
                                      MethodInvocationContext<Object, Object> context) {
        metric.inc();
        return context.proceed();
    }
}
