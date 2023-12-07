package io.helidon.inject.configdriven.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderInjectionException;
import io.helidon.inject.api.Services;
import io.helidon.inject.runtime.ServiceProviderBase;
import io.helidon.inject.service.Descriptor;
import io.helidon.inject.service.InjectionContext;
import io.helidon.inject.service.IpId;
import io.helidon.inject.service.Qualifier;
import io.helidon.inject.spi.InjectionResolver;

class ConfigDrivenInstanceProvider<T, CB>
        extends ServiceProviderBase<T>
        implements InjectionResolver {

    private final CB beanInstance;
    private final String instanceId;
    private final ConfigDrivenServiceProvider<T, CB> root;
    private final TypeName configBeanType;
    private final Set<Qualifier> qualifiers;

    ConfigDrivenInstanceProvider(InjectionServices injectionServices,
                                 Descriptor<T> descriptor,
                                 ConfigDrivenServiceProvider<T, CB> root,
                                 String name,
                                 CB instance) {
        super(injectionServices, descriptor);

        this.configBeanType = root.configBeanType();
        this.beanInstance = instance;
        this.instanceId = name;
        this.root = root;
        this.qualifiers = Set.of(Qualifier.createNamed(name));
    }

    @Override
    public boolean isRootProvider() {
        return false;
    }

    @Override
    public Optional<ServiceProvider<?>> rootProvider() {
        return Optional.of(root);
    }

    // note that all responsibilities to resolve is delegated to the root provider
    @Override
    public Optional<Object> resolve(IpId ipInfo,
                                    InjectionServices injectionServices,
                                    ServiceProvider<?> serviceProvider,
                                    boolean resolveIps) {

        ServiceInfoCriteria dep = ServiceInfoCriteria.create(ipInfo);
        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder()
                .addContract(configBeanType)
                .build();
        if (!dep.matchesContracts(criteria)) {
            return Optional.empty();    // we are being injected with neither a config bean nor a service that matches ourselves
        }

        // if we are here then we are asking for a config bean for ourselves, or a managed instance
        if (!dep.qualifiers().isEmpty()) {
            throw new ServiceProviderInjectionException("cannot use qualifiers while injecting config beans for self", this);
        }

        return Optional.of(beanInstance);
    }

    @Override
    public String toString() {
        return "Config Driven Instance for: " + descriptor().serviceType().fqName() + "{" + instanceId
                + "}[" + currentActivationPhase() + "]";
    }

    @Override
    public Set<Qualifier> qualifiers() {
        return qualifiers;
    }

    @Override
    protected String id(boolean fq) {
        return super.id(fq) + "{" + instanceId + "}";
    }

    @Override
    protected void prepareDependency(Services services, Map<IpId, Supplier<?>> injectionPlan, IpId dependency) {
        // it the type is this bean's type and it does not have any additional qualifier,
        // inject instance

        if (dependency.contract().equals(configBeanType) && dependency.qualifiers().isEmpty()) {
            // we are injecting the config bean that drives this instance
            injectionPlan.put(dependency, () -> beanInstance);
            return;
        }

        super.prepareDependency(services, injectionPlan, dependency);
    }

    @Override
    protected void injectionContext(InjectionContext injectionContext) {
        super.injectionContext(injectionContext);
    }

    CB beanInstance() {
        return beanInstance;
    }

    void activate() {
        super.activate(InjectionServices.createActivationRequestDefault());
    }


}
