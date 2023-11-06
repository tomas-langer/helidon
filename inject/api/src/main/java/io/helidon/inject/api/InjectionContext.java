package io.helidon.inject.api;

/**
 * All data needed for creating an instance
 * of a service, or for invoking methods that use
 * context. The context contains only the services needed
 * for the specific location.
 */
public interface InjectionContext {
    static InjectionContext empty() {
        return EmptyInjectionContext.EMPTY;
    }

    /**
     * Obtain a parameter for a specific id.
     * The ID must be known in advance and provided through {@link io.helidon.inject.api.ServiceDescriptor}.
     *
     * @param paramId parameter ID
     * @return value for the parameter, this may be null
     * @param <T> type of the parameter
     */
    <T> T param(IpId<T> paramId);
}
