package io.helidon.declarative.tests.http;

import io.helidon.common.Weight;
import io.helidon.service.inject.api.EntryPointInterceptor;
import io.helidon.service.inject.api.Injection;
import io.helidon.service.inject.api.Interception;
import io.helidon.service.inject.api.InvocationContext;

@Injection.Singleton
@Weight(1000)
class SomeEntryPointInterceptor implements EntryPointInterceptor {
    @Override
    public <T> T proceed(InvocationContext invocationContext, Interception.Interceptor.Chain<T> chain, Object... args)
            throws Exception {
        System.out.println("Pre entry-point");
        try {
            return chain.proceed(args);
        } finally {
            System.out.println("Post entry-point");
        }
    }
}
