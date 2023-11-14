package io.helidon.inject.api;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import io.helidon.common.types.TypeName;

class InjectionContextImpl implements InjectionContext {
    private final Map<TypeName, Map<IpId<?>, Supplier<?>>> injectionPlans;

    InjectionContextImpl(Map<TypeName, Map<IpId<?>, Supplier<?>>> injectionPlans) {
        this.injectionPlans = injectionPlans;
    }

    @Override
    @SuppressWarnings("unchecked") // we have a map, and that cannot have type to instance values
    public <T> T param(TypeName serviceType, IpId<T> paramId) {
        Map<IpId<?>, Supplier<?>> plan = injectionPlans.get(serviceType);
        if (plan == null) {
            throw new NoSuchElementException("Cannot resolve injection id " + paramId + " for service " + serviceType.fqName()
                                                     + ", this service dependencies are not available");
        }
        Supplier<?> supplier = plan.get(paramId);
        if (supplier == null) {
            throw new NoSuchElementException("Cannot resolve injection id " + paramId + " for service " + serviceType.fqName()
                                                     + ", this dependency was not declared in "
                                                     + "the service descriptor");
        }
        return (T) supplier.get();
    }
}
