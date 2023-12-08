package io.helidon.inject.configdriven.runtime;

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectTypes;
import io.helidon.inject.service.ServiceInfo;

/**
 * Service descriptor for {@link io.helidon.inject.configdriven.runtime.ConfigBeanRegistry}.
 */
public final class ConfigBeanRegistryDescriptor implements ServiceInfo<ConfigBeanRegistry> {
    /**
     * Singleton instance of this descriptor.
     */
    public static final ConfigBeanRegistryDescriptor INSTANCE = new ConfigBeanRegistryDescriptor();

    private static final TypeName TYPE = TypeName.create(ConfigBeanRegistryDescriptor.class);
    private static final TypeName CBR_TYPE = TypeName.create(ConfigBeanRegistry.class);
    private static final Set<TypeName> CONTRACTS = Set.of(CBR_TYPE);

    private ConfigBeanRegistryDescriptor() {
    }

    @Override
    public TypeName serviceType() {
        return CBR_TYPE;
    }

    @Override
    public TypeName descriptorType() {
        return TYPE;
    }

    @Override
    public Set<TypeName> contracts() {
        return CONTRACTS;
    }

    @Override
    public Set<TypeName> scopes() {
        return Set.of(InjectTypes.SINGLETON);
    }
}
