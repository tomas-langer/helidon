package io.helidon.inject.runtime;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import io.helidon.inject.service.InjectionContext;
import io.helidon.inject.service.IpId;

/**
 * A context for obtaining injection point values in a {@link io.helidon.inject.service.Descriptor}.
 * This context is pre-filled with the correct providers either based on an {@link io.helidon.inject.api.Application},
 * or based on analysis during activation of a service provider.
 *
 * @see io.helidon.inject.service.InjectionContext
 */
public class HelidonInjectionContext implements InjectionContext {
    private final Map<IpId, Supplier<?>> injectionPlans;

    HelidonInjectionContext(Map<IpId, Supplier<?>> injectionPlans) {
        this.injectionPlans = injectionPlans;
    }

    /**
     * Create an injection context based on a map of providers.
     *
     * @param injectionPlan map of injection ids to provider that satisfies that injection point
     * @return a new injection context
     */
    public static InjectionContext create(Map<IpId, Supplier<?>> injectionPlan) {
        return new HelidonInjectionContext(injectionPlan);
    }

    @Override
    @SuppressWarnings("unchecked") // we have a map, and that cannot have type to instance values
    public <T> T param(IpId paramId) {
        Supplier<?> injectionSupplier = injectionPlans.get(paramId);
        if (injectionSupplier == null) {
            throw new NoSuchElementException("Cannot resolve injection id " + paramId + " for service "
                                                     + paramId.service().fqName()
                                                     + ", this dependency was not declared in "
                                                     + "the service descriptor");
        }

        return (T) injectionSupplier.get();
    }
}
