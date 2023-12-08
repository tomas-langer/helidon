package io.helidon.inject.runtime;

import java.util.List;
import java.util.Optional;

import io.helidon.common.LazyValue;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderBindable;
import io.helidon.inject.service.IpId;

/**
 * A service provider bound to another service provider for an injection point.
 *
 * @param <T> type of the provided service
 */
class BoundServiceProvider<T> extends DescribedServiceProvider<T> implements ServiceProvider<T> {
    private final ServiceProvider<T> binding;
    private final LazyValue<T> instance;
    private final LazyValue<List<T>> instances;

    private BoundServiceProvider(ServiceProvider<T> binding, IpId ipId) {
        super(binding.serviceInfo());

        this.binding = binding;
        ContextualServiceQuery query = ContextualServiceQuery.builder()
                .injectionPointInfo(ipId)
                .serviceInfoCriteria(ServiceInfoCriteria.create(ipId))
                .expected(false)
                .build();
        this.instance = LazyValue.create(() -> binding.first(query).orElse(null));
        this.instances = LazyValue.create(() -> binding.list(query));
    }

    /**
     * Creates a bound service provider to a specific binding.
     *
     * @param binding the bound service provider
     * @param ipId    the binding context
     * @return the service provider created, wrapping the binding delegate provider
     */
    static <V> ServiceProvider<V> create(ServiceProvider<V> binding,
                                         IpId ipId) {

        if (binding instanceof ServiceProviderBase<V> base) {
            if (!base.isProvider()) {
                return binding;
            }
        }
        return new BoundServiceProvider<>(binding, ipId);
    }

    @Override
    public String toString() {
        return binding.toString();
    }

    @Override
    public int hashCode() {
        return binding.hashCode();
    }

    @Override
    public boolean equals(Object another) {
        return (another instanceof ServiceProvider && binding.equals(another));
    }

    @Override
    public Optional<T> first(ContextualServiceQuery query) {
        return Optional.ofNullable(instance.get());
    }

    @Override
    public List<T> list(ContextualServiceQuery query) {
        return instances.get();
    }

    @Override
    public String id() {
        return binding.id();
    }

    @Override
    public String description() {
        return binding.description();
    }

    @Override
    public boolean isProvider() {
        return binding.isProvider();
    }

    @Override
    public Phase currentActivationPhase() {
        return binding.currentActivationPhase();
    }

    @Override
    public Optional<ServiceProviderBindable<T>> serviceProviderBindable() {
        return Optional.of((ServiceProviderBindable<T>) binding);
    }
}
