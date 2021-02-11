package io.helidon.di.metrics;

import javax.inject.Named;
import javax.inject.Singleton;

import io.helidon.di.webserver.ResponseSupport;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Timed;

@Singleton
@Internal
class InterceptTimed extends InterceptorBase<Timer, Timed> {

    InterceptTimed(@Named("application") MetricRegistry registry) {
        super(registry,
              Timer.class,
              Timed.class,
              MetricType.SIMPLE_TIMER);
    }

    @Override
    protected Object prepareAndInvoke(Timer metric,
                                      AnnotationValue<Timed> annotValue,
                                      MethodInvocationContext<Object, Object> context) {
        Timer.Context timerContext = metric.time();
        return ResponseSupport.intercept(context, timerContext::stop, thr -> timerContext.stop());
    }
}
