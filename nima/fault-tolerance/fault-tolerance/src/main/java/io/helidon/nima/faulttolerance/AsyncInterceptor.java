package io.helidon.nima.faulttolerance;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import io.helidon.common.Weight;
import io.helidon.common.types.AnnotationAndValue;
import io.helidon.common.types.TypedElementName;
import io.helidon.pico.api.PicoServices;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named("io.helidon.nima.faulttolerance.FaultTolerance.Async")
@Weight(FaultTolerance.WEIGHT_ASYNC)
@Singleton
class AsyncInterceptor extends InterceptorBase<Async> {
    AsyncInterceptor() {
        super(PicoServices.realizedServices(), Async.class, FaultTolerance.Async.class);
    }

    @Override
    <V> V invokeHandler(Async ftHandler, Chain<V> chain, Object[] args) {
        try {
            return doInvoke(ftHandler, chain, args);
        } catch (RuntimeException e) {
            // we want to re-throw runtime exceptions
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable e) {
            throw new SupplierException("Failed to invoke asynchronous supplier", e.getCause());
        }
    }

    private <V> V doInvoke(Async ftHandler, Chain<V> chain, Object[] args) throws Throwable {
        try {
            return ftHandler.invoke(() -> chain.proceed(args))
                    .get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            cause.addSuppressed(e);
            throw cause;
        } catch (InterruptedException e) {
            throw new SupplierException("Failed to invoke asynchronous supplier, interrupted", e);
        }
    }

    @Override
    Async obtainHandler(TypedElementName elementInfo, CacheRecord cacheRecord) {
        return namedHandler(elementInfo, this::fromAnnotation);
    }

    private Async fromAnnotation(AnnotationAndValue annotation) {
        String name = annotation.value("name").orElse("async-") + System.identityHashCode(annotation);
        ExecutorService executorService = annotation.value("executorName")
                .filter(Predicate.not(String::isBlank))
                .flatMap(it -> lookupNamed(ExecutorService.class, it))
                .orElseGet(() -> FaultTolerance.executor().get());

        return io.helidon.nima.faulttolerance.Async.create(AsyncConfigDefault.builder()
                                    .name(name)
                                    .executor(executorService)
                                    .build());
    }
}
