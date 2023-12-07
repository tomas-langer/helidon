package io.helidon.inject.service;

/**
 * All data needed for creating an instance
 * of a service, or for invoking methods that use
 * context. The context contains only the services needed
 * for the specific location.
 */
public interface InjectionContext {
    /**
     * Obtain a parameter for a specific id.
     * The ID must be known in advance and provided through {@link ServiceInfo}.
     *
     * @param paramId parameter ID
     * @param <T>     type of the parameter, for convenience, the result is cast to this type
     * @return value for the parameter, this may be null
     */
    <T> T param(IpId paramId);
}
