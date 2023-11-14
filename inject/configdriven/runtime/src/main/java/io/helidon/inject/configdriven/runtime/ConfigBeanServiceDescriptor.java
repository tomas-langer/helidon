package io.helidon.inject.configdriven.runtime;

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceDescriptor;

class ConfigBeanServiceDescriptor<CB> implements ServiceDescriptor<CB> {
    private final TypeName beanType;
    private final Set<Qualifier> qualifiers;

    ConfigBeanServiceDescriptor(Class<?> beanType, String name) {
        this.beanType = TypeName.create(beanType);
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
}
