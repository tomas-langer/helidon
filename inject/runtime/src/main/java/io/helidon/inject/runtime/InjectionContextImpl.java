package io.helidon.inject.runtime;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import io.helidon.inject.api.InjectionContext;
import io.helidon.inject.api.IpId;

class InjectionContextImpl implements InjectionContext {
    private final Map<IpId<?>, Supplier<?>> injectionPlan;

    InjectionContextImpl(Map<IpId<?>, Supplier<?>> injectionPlan) {
        this.injectionPlan = injectionPlan;
    }

    @SuppressWarnings("unchecked") // we have a map, and that cannot have type to instance values
    @Override
    public <T> T param(IpId<T> paramId) {
        Supplier<?> supplier = injectionPlan.get(paramId);
        if (supplier == null) {
            throw new NoSuchElementException("Cannot resolve injection id " + paramId + ", this dependency was not declared in "
                                                     + "the service descriptor");
        }
        return (T) supplier.get();
    }
}
