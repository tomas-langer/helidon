package io.helidon.service.tests.inject.interception;

import java.util.HashSet;
import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.service.inject.api.Injection;
import io.helidon.service.inject.api.Interception;
import io.helidon.service.inject.api.InvocationContext;

@Injection.Singleton
@Injection.NamedByClass(Construct.class)
class ConstructorInterceptor implements Interception.Interceptor {
    static final Set<TypeName> CONSTRUCTED = new HashSet<>();

    @Override
    public <V> V proceed(InvocationContext ctx, Chain<V> chain, Object... args) throws Exception {
        CONSTRUCTED.add(ctx.serviceInfo().serviceType());
        return chain.proceed(args);
    }
}
