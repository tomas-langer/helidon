package io.helidon.metrics;

import java.util.List;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.metrics.api.Metrics;
import io.helidon.metrics.api.Tag;
import io.helidon.service.inject.api.Injection;
import io.helidon.service.inject.api.Interception;
import io.helidon.service.inject.api.InvocationContext;

@Injection.Singleton
@Injection.NamedByClass(Metrics.Counted.class)
@Weight(Weighted.DEFAULT_WEIGHT - 10)
class CountedInterceptor implements Interception.Interceptor {
    @Override
    public <V> V proceed(InvocationContext ctx, Chain<V> chain, Object... args) throws Exception {
        List<Annotation> annotations = ctx.typeAnnotations();
        TypedElementInfo typedElementInfo = ctx.elementInfo();

        return chain.proceed(args);
    }

    record CounterConfiguration(String counterName,
                                String description,
                                List<Tag> tags,
                                boolean countThrown,
                                boolean countSuccess) {

    }
}
