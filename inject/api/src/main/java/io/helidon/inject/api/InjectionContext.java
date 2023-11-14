package io.helidon.inject.api;

import java.util.Map;
import java.util.function.Supplier;

import io.helidon.common.types.TypeName;

/**
 * All data needed for creating an instance
 * of a service, or for invoking methods that use
 * context. The context contains only the services needed
 * for the specific location.
 */
public interface InjectionContext {
    static InjectionContext create(Map<TypeName, Map<IpId<?>, Supplier<?>>> injectionPlans) {
        return new InjectionContextImpl(injectionPlans);
    }

    /**
     * Obtain a parameter for a specific id.
     * The ID must be known in advance and provided through {@link io.helidon.inject.api.ServiceDescriptor}.
     *
     * @param serviceType service type
     * @param paramId     parameter ID
     * @param <T>         type of the parameter
     * @return value for the parameter, this may be null
     */
    <T> T param(TypeName serviceType, IpId<T> paramId);
}
