package io.helidon.nima.faulttolerance;

import java.util.List;
import java.util.Optional;

import io.helidon.common.GenericType;
import io.helidon.common.Weight;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementName;
import io.helidon.pico.api.InvocationContext;
import io.helidon.pico.api.PicoException;
import io.helidon.pico.api.PicoServices;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
@Named("io.helidon.nima.faulttolerance.FaultTolerance.Fallback")
@Weight(FaultTolerance.WEIGHT_FALLBACK)
@Singleton
class FallbackInterceptor extends InterceptorBase<Fallback> {
    FallbackInterceptor() {
        super(PicoServices.realizedServices(), Fallback.class, FaultTolerance.Fallback.class);
    }

    @Override
    public <V> V proceed(InvocationContext ctx, Chain<V> chain, Object... args) {

        try {
            return chain.proceed(args);
        } catch (Throwable t) {
            // these are our cache keys
            // class name of the service
            TypeName typeName = ctx.serviceTypeName();
            // method this was declared on (as Fallback can only be defined on a method)
            String methodName = ctx.elementInfo().elementName();
            Optional<TypedElementName[]> params = ctx.elementArgInfo();

            CacheRecord cacheRecord = new CacheRecord(typeName, methodName, params);
            FallbackMethod<V, Object> fallbackMethod = generatedMethod(FallbackMethod.class, cacheRecord)
                    .orElseGet(() -> new FailingFallbackMethod(cacheRecord));

            try {
                return fallbackMethod.fallback(ctx.serviceProvider().get(), t, args);
            } catch (RuntimeException e) {
                e.addSuppressed(t);
                throw e;
            } catch (Throwable x) {
                x.addSuppressed(t);
                throw new SupplierException("Failed to invoke fallback method: " + cacheRecord, x);
            }
        }
    }

    private static class FailingFallbackMethod implements FallbackMethod {
        private final CacheRecord cacheRecord;

        private FailingFallbackMethod(CacheRecord cacheRecord) {
            this.cacheRecord = cacheRecord;
        }

        @Override
        public Object fallback(Object service, Throwable throwable, Object... arguments) {
            throw new PicoException("Could not find a service that implements fallback method for: "
                                            + cacheRecord);
        }

        @Override
        public List<GenericType<?>> parameterTypes() {
            return List.of();
        }
    }
}
