package io.helidon.inject.configdriven.runtime;

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectTypes;
import io.helidon.inject.service.Descriptor;
import io.helidon.inject.service.InjectionContext;
import io.helidon.inject.service.InterceptionMetadata;

/**
 * A descriptor for config driven registry. Must be public, as other services that inject to it may need to
 * use this type.
 * For most services, such a type is code generated.
 */
public class CbrServiceDescriptor implements Descriptor<ConfigBeanRegistryImpl> {
    /**
     * Singleton instance bound to binder.
     */
    public static final CbrServiceDescriptor INSTANCE = new CbrServiceDescriptor();
    static final String CBR_RUNTIME_ID = "CBR_CONFIG_DRIVEN";
    private static final TypeName TYPE = TypeName.create(ConfigBeanRegistryImpl.class);
    private static final TypeName CBR_TYPE = TypeName.create(ConfigBeanRegistry.class);
    private static final TypeName DESCRIPTOR_TYPE = TypeName.create(CbrServiceDescriptor.class);
    private static final Set<TypeName> CONTRACTS = Set.of(CBR_TYPE);

    @Override
    public String runtimeId() {
        return CBR_RUNTIME_ID;
    }

    @Override
    public TypeName serviceType() {
        return TYPE;
    }

    @Override
    public TypeName descriptorType() {
        return DESCRIPTOR_TYPE;
    }

    @Override
    public Set<TypeName> contracts() {
        return CONTRACTS;
    }

    @Override
    public Object instantiate(InjectionContext ctx, InterceptionMetadata interceptionMetadata) {
        return ConfigBeanRegistry.instance();
    }

    @Override
    public Set<TypeName> scopes() {
        return Set.of(InjectTypes.SINGLETON);
    }
}
