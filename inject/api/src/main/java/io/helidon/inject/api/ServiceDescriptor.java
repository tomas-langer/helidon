package io.helidon.inject.api;

import java.util.List;

public interface ServiceDescriptor<T> {
    /**
     * Id used by the basic Helidon injection.
     */
    String INJECTION_RUNTIME_ID = "INJECTION";

    /**
     * Id of runtime responsible for this service, such as
     * injection, or config driven.
     *
     * @return type of the runtime
     */
    default String runtimeId() {
        return INJECTION_RUNTIME_ID;
    }

    /**
     * Type of the service this descriptor describes.
     *
     * @return service type
     */
    Class<T> serviceType();

    /**
     * List of dependencies required by this service. Each dependency is a point of injection of one instance into
     * constructor or method parameter, or into a field.
     *
     * @return required dependencies
     */
    List<IpInfo<?>> dependencies();

    /**
     * Create a new service instance.
     *
     * @param ctx injection context with all injection points data
     * @return a new instance
     */
    T instantiate(InjectionContext ctx);

    /**
     * Inject methods.
     *
     * @param ctx      injection context
     * @param instance instance to update
     */

    void injectMethods(InjectionContext ctx, T instance);

    /**
     * Inject fields.
     *
     * @param ctx      injection context
     * @param instance instance to update
     */

    void injectFields(InjectionContext ctx, T instance);

    /**
     * Invoke {@link jakarta.annotation.PostConstruct} annotated method(s).
     */
    default void postConstruct(T instance) {
    }

    /**
     * Invoke {@link jakarta.annotation.PreDestroy} annotated method(s).
     */
    default void preDestroy(T instance) {
    }
}
