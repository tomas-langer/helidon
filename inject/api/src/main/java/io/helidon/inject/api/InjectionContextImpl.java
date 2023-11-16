package io.helidon.inject.api;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

class InjectionContextImpl implements InjectionContext {
    private final Map<IpId<?>, Supplier<?>> injectionPlans;

    InjectionContextImpl(Map<IpId<?>, Supplier<?>> injectionPlans) {
        this.injectionPlans = injectionPlans;
    }

    @Override
    @SuppressWarnings("unchecked") // we have a map, and that cannot have type to instance values
    public <T> T param(IpId<T> paramId) {
        Supplier<?> injectionSupplier = injectionPlans.get(paramId);
        if (injectionSupplier == null) {
            throw new NoSuchElementException("Cannot resolve injection id " + paramId + " for service "
                                                     + paramId.serviceType().fqName()
                                                     + ", this dependency was not declared in "
                                                     + "the service descriptor");
        }
        return (T) injectionSupplier.get();
    }
}
