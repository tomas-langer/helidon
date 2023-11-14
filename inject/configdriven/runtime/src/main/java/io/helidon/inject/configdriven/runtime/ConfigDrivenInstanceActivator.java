package io.helidon.inject.configdriven.runtime;

import java.util.Map;
import java.util.function.Supplier;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.IpInfo;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.api.Services;
import io.helidon.inject.runtime.ServiceActivatorBase;

class ConfigDrivenInstanceActivator<T, CB> extends ServiceActivatorBase<T, ConfigDrivenInstanceProvider<T, CB>> {
    private final CB instance;
    private final TypeName cbType;

    ConfigDrivenInstanceActivator(InjectionServices injectionServices,
                                  ServiceSource<T> descriptor,
                                  CB instance,
                                  TypeName cbType) {
        super(injectionServices, descriptor);

        this.instance = instance;
        this.cbType = cbType;
    }

    @Override
    protected void prepareDependency(Services services, Map<IpId<?>, Supplier<?>> injectionPlan, IpInfo dependency) {
        // it the type is this bean's type and it does not have any additional qualifier,
        // inject instance

        if (dependency.contract().equals(cbType) && dependency.qualifiers().isEmpty()) {
            // we are injecting the config bean that drives this instance
            injectionPlan.put(dependency.id(), () -> instance);
            return;
        }

        super.prepareDependency(services, injectionPlan, dependency);
    }

    @Override
    public String toString() {
        return "Config Driven Instance activator for: " + descriptor().serviceType().fqName() + "[" + phase() + "]";
    }
}
