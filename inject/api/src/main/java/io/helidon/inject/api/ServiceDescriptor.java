package io.helidon.inject.api;

import java.util.List;
import java.util.Set;

import io.helidon.common.Weighted;
import io.helidon.common.types.TypeName;

/**
 * Service metadata.
 * Also serves as a unique identification of a service in the service registry.
 *
 * @param <T> type of the service implementation this descriptor describes
 */
@FunctionalInterface
public interface ServiceDescriptor<T> extends Weighted {
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
    TypeName serviceType();

    /**
     * Type of the descriptor (usually generated).
     *
     * @return descriptor type
     */
    default TypeName descriptorType() {
        return TypeName.create(getClass());
    }

    /**
     * Set of contracts the described service implements.
     *
     * @return set of contracts
     */
    default Set<TypeName> contracts() {
        return Set.of();
    }

    /**
     * List of dependencies required by this service. Each dependency is a point of injection of one instance into
     * constructor or method parameter, or into a field.
     *
     * @return required dependencies
     */
    default List<IpId> dependencies() {
        return List.of();
    }

    /**
     * Service qualifiers.
     *
     * @return qualifiers
     */
    default Set<Qualifier> qualifiers() {
        return Set.of();
    }

    /**
     * Run level of this service.
     *
     * @return run level
     */
    default int runLevel() {
        return 100;
    }

    /**
     * Set of scopes of this service.
     *
     * @return scopes
     */
    default Set<TypeName> scopes() {
        return Set.of();
    }

    /**
     * Returns {@code true} for abstract classes and interfaces,
     * returns {@code false} by default.
     *
     * @return whether this descriptor describes an abstract class or interface
     */
    default boolean isAbstract() {
        return false;
    }
}
