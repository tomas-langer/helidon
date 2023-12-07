package io.helidon.inject.configdriven.runtime;

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectTypes;
import io.helidon.inject.service.Qualifier;
import io.helidon.inject.service.ServiceInfo;

class ConfigBeanServiceDescriptor<CB> implements ServiceInfo<CB> {
    private final TypeName beanType;
    private final Set<Qualifier> qualifiers;

    ConfigBeanServiceDescriptor(TypeName beanType, String name) {
        this.beanType = beanType;
        this.qualifiers = Set.of(Qualifier.createNamed(name));
    }

    @Override
    public TypeName serviceType() {
        return beanType;
    }

    @Override
    public Set<Qualifier> qualifiers() {
        return qualifiers;
    }

    @Override
    public Set<TypeName> scopes() {
        return Set.of(InjectTypes.SINGLETON);
    }
}
