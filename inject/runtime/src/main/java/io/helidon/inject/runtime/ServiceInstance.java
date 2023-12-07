package io.helidon.inject.runtime;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import io.helidon.inject.api.InjectTypes;
import io.helidon.inject.service.Descriptor;
import io.helidon.inject.service.InjectionContext;
import io.helidon.inject.service.InterceptionMetadata;

interface ServiceInstance<T> extends Supplier<T> {
    static <T> ServiceInstance<T> create(InterceptionMetadata interceptionMetadata,
                                         InjectionContext ctx,
                                         Descriptor<T> source) {
        if (source.scopes().contains(InjectTypes.SINGLETON)) {
            return new SingletonInstance<>(ctx, interceptionMetadata, source);
        }
        return new OnDemandInstance<>(ctx, interceptionMetadata, source);
    }

    static <T> ServiceInstance<T> create(Descriptor<T> source, T instance) {
        return new ExplicitInstance<>(source, instance);
    }

    default void construct() {
    }

    default void inject() {
    }

    default void postConstruct() {
    }

    default void preDestroy() {

    }

    private static <T> T inject(Descriptor<T> source,
                                InjectionContext ctx,
                                InterceptionMetadata interceptionMetadata,
                                T instance) {

        // using linked set, so we can see in debugging what was injected first
        Set<Descriptor.MethodSignature> injected = new LinkedHashSet<>();
        source.inject(ctx, interceptionMetadata, injected, instance);
        return instance;
    }

    class ExplicitInstance<T> implements ServiceInstance<T> {
        private final Descriptor<T> source;
        private final T instance;

        ExplicitInstance(Descriptor<T> source, T instance) {
            this.source = source;
            this.instance = instance;
        }

        @Override
        public T get() {
            return instance;
        }

        @Override
        public void preDestroy() {
            source.preDestroy(instance);
        }
    }

    class SingletonInstance<T> implements ServiceInstance<T> {
        private final InjectionContext ctx;
        private final InterceptionMetadata interceptionMetadata;
        private final Descriptor<T> source;

        private volatile T instance;

        private SingletonInstance(InjectionContext ctx, InterceptionMetadata interceptionMetadata, Descriptor<T> source) {
            this.ctx = ctx;
            this.interceptionMetadata = interceptionMetadata;
            this.source = source;
        }

        @Override
        public T get() {
            return instance;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void construct() {
            instance = (T) source.instantiate(ctx, interceptionMetadata);
        }

        @Override
        public void inject() {
            ServiceInstance.inject(source, ctx, interceptionMetadata, instance);
        }

        @Override
        public void postConstruct() {
            source.postConstruct(instance);
        }

        @Override
        public void preDestroy() {
            source.preDestroy(instance);
        }
    }

    class OnDemandInstance<T> implements ServiceInstance<T> {
        private final InjectionContext ctx;
        private final InterceptionMetadata interceptionMetadata;
        private final Descriptor<T> source;

        OnDemandInstance(InjectionContext ctx, InterceptionMetadata interceptionMetadata, Descriptor<T> source) {
            this.ctx = ctx;
            this.interceptionMetadata = interceptionMetadata;
            this.source = source;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            T instance = (T) source.instantiate(ctx, interceptionMetadata);
            return ServiceInstance.inject(source, ctx, interceptionMetadata, instance);
        }
    }
}
