package io.helidon.declarative.webserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.common.Weighted;
import io.helidon.common.Weights;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.service.inject.api.EntryPointInterceptor;
import io.helidon.service.inject.api.GeneratedInjectService;
import io.helidon.service.inject.api.Injection;
import io.helidon.service.inject.api.InvocationContext;
import io.helidon.service.inject.api.Qualifier;
import io.helidon.service.inject.api.ServiceInstance;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

@Injection.Singleton
class HttpEntryPointsImpl implements HttpEntryPoints {
    private final boolean noInterceptors;
    private final List<HttpEntryPointInterceptor> interceptors;

    @Injection.Inject
    HttpEntryPointsImpl(List<ServiceInstance<EntryPointInterceptor>> entryPointInterceptors,
                        List<ServiceInstance<HttpEntryPointInterceptor>> httpEntryPointInterceptors) {
        this.noInterceptors = entryPointInterceptors.isEmpty() && httpEntryPointInterceptors.isEmpty();
        this.interceptors = merge(entryPointInterceptors, httpEntryPointInterceptors);
    }

    @Override
    public Handler handler(GeneratedInjectService.Descriptor<?> descriptor,
                           Set<Qualifier> typeQualifiers,
                           List<Annotation> typeAnnotations,
                           TypedElementInfo methodInfo,
                           Handler actualHandler) {

        if (noInterceptors) {
            return actualHandler;
        }

        InvocationContext ctx = InvocationContext.builder()
                .typeAnnotations(typeAnnotations)
                .elementInfo(methodInfo)
                .serviceInfo(descriptor)
                .build();

        return new EntryPointHandler(ctx, interceptors, actualHandler);
    }

    private static List<HttpEntryPointInterceptor> merge(List<ServiceInstance<EntryPointInterceptor>> entryPoints,
                                                         List<ServiceInstance<HttpEntryPointInterceptor>> httpEntryPoints) {

        List<WeightedInterceptor> merged = new ArrayList<>();
        httpEntryPoints.stream()
                .map(it -> new WeightedInterceptor(it.get(),
                                                   it.weight()))
                .forEach(merged::add);

        entryPoints.stream()
                .map(it -> new WeightedInterceptor(toHttpEntryPoint(it.get()), it.weight()))
                .forEach(merged::add);
        Weights.sort(merged);

        return merged.stream()
                .map(WeightedInterceptor::interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    private static HttpEntryPointInterceptor toHttpEntryPoint(EntryPointInterceptor entryPointInterceptor) {
        return (invocationContext, chain, request, response) -> {
            entryPointInterceptor.proceed(invocationContext, chain, request, response);
        };
    }

    private record WeightedInterceptor(HttpEntryPointInterceptor interceptor,
                                       double weight) implements Weighted {
    }

    private static class EntryPointHandler implements Handler {
        private final InvocationContext ctx;
        private final List<HttpEntryPointInterceptor> interceptors;
        private final Handler actualHandler;

        private EntryPointHandler(InvocationContext ctx, List<HttpEntryPointInterceptor> interceptors, Handler actualHandler) {
            this.ctx = ctx;
            this.interceptors = interceptors;
            this.actualHandler = actualHandler;
        }

        @Override
        public void handle(ServerRequest req, ServerResponse res) throws Exception {
            createChain().proceed(req, res);
        }

        private HttpEntryPointInterceptor.Chain createChain() {
            return new Invocation(ctx, interceptors, actualHandler);
        }
    }

    private static class Invocation implements HttpEntryPointInterceptor.Chain {
        private final InvocationContext ctx;
        private final List<HttpEntryPointInterceptor> interceptors;
        private final Handler actualHandler;

        private int interceptorPos;

        private Invocation(InvocationContext ctx, List<HttpEntryPointInterceptor> interceptors, Handler actualHandler) {
            this.ctx = ctx;
            this.interceptors = interceptors;
            this.actualHandler = actualHandler;
        }

        @Override
        public void proceed(ServerRequest request, ServerResponse response) throws Exception {
            if (interceptorPos < interceptors.size()) {
                var interceptor = interceptors.get(interceptorPos);
                interceptorPos++;
                try {
                    interceptor.proceed(ctx, this, request, response);
                    return;
                } catch (Exception e) {
                    interceptorPos--;
                    throw e;
                }
            }
            actualHandler.handle(request, response);
        }

        @Override
        public String toString() {
            return String.valueOf(ctx.elementInfo());
        }
    }
}
