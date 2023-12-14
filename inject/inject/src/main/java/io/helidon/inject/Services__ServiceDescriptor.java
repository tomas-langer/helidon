package io.helidon.inject;

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.service.ServiceDescriptor;

/**
 * Service descriptor to enable injection of {@link io.helidon.inject.Services}.
 */
public class Services__ServiceDescriptor implements ServiceDescriptor<Services> {
    /**
     * Singleton instance to be referenced when building applications.
     */
    public static final Services__ServiceDescriptor INSTANCE = new Services__ServiceDescriptor();

    private static final TypeName SERVICES = TypeName.create(Services.class);
    private static final TypeName INFO_TYPE = TypeName.create(Services__ServiceDescriptor.class);

    @Override
    public TypeName serviceType() {
        return SERVICES;
    }

    @Override
    public TypeName infoType() {
        return INFO_TYPE;
    }

    @Override
    public Set<TypeName> scopes() {
        return Set.of(InjectTypes.SINGLETON);
    }
}
