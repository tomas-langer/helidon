package io.helidon.di.metrics;

import javax.inject.Named;
import javax.inject.Singleton;

import io.helidon.di.webserver.ResponseSupport;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;

@Singleton
@Internal
class InterceptSimplyTimed extends InterceptorBase<SimpleTimer, SimplyTimed> {

    InterceptSimplyTimed(@Named("application") MetricRegistry registry) {
        super(registry,
              SimpleTimer.class,
              SimplyTimed.class,
              MetricType.SIMPLE_TIMER);
    }

    @Override
    protected Object prepareAndInvoke(SimpleTimer metric,
                                      AnnotationValue<SimplyTimed> annotValue,
                                      MethodInvocationContext<Object, Object> context) {
        SimpleTimer.Context timerContext = metric.time();
        return ResponseSupport.intercept(context, timerContext::stop, thr -> timerContext.stop());
    }
}
