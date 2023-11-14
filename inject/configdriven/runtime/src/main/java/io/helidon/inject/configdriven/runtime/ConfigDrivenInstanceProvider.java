package io.helidon.inject.configdriven.runtime;

import java.util.Optional;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectionPointInfo;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderInjectionException;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.runtime.ServiceProviderBase;
import io.helidon.inject.spi.InjectionResolver;

class ConfigDrivenInstanceProvider<T, CB>
        extends ServiceProviderBase<T, ConfigDrivenInstanceProvider<T, CB>, ConfigDrivenInstanceActivator<T, CB>>
        implements InjectionResolver {
    private static final System.Logger LOGGER = System.getLogger(ConfigDrivenInstanceProvider.class.getName());
    private final CB beanInstance;
    private final String instanceId;
    private final ConfigDrivenServiceProvider<T, CB> root;

    ConfigDrivenInstanceProvider(InjectionServices injectionServices,
                                 ServiceSource<T> descriptor,
                                 ConfigDrivenServiceProvider<T, CB> root,
                                 String name,
                                 CB instance) {
        super(injectionServices,
              descriptor,
              new ConfigDrivenInstanceActivator<>(injectionServices,
                                                  descriptor,
                                                  instance,
                                                  TypeName.create(root.configBeanType())));

        this.beanInstance = instance;
        this.instanceId = name;
        this.root = root;
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
    public Optional<Object> resolve(InjectionPointInfo ipInfo,
                                    InjectionServices injectionServices,
                                    ServiceProvider<?> serviceProvider,
                                    boolean resolveIps) {

        ServiceInfoCriteria dep = ipInfo.dependencyToServiceInfo();
        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder()
                .addContractImplemented(root.configBeanType())
                .build();
        if (!dep.matchesContracts(criteria)) {
            return Optional.empty();    // we are being injected with neither a config bean nor a service that matches ourselves
        }

        // if we are here then we are asking for a config bean for ourselves, or a slave/managed instance
        if (!dep.qualifiers().isEmpty()) {
            throw new ServiceProviderInjectionException("cannot use qualifiers while injecting config beans for self", this);
        }

        return Optional.of(beanInstance);
    }

    @Override
    protected String id(boolean fq) {
        return super.id(fq) + "{" + instanceId + "}";
    }

    void activate() {
        super.getActivator().activate(InjectionServices.createActivationRequestDefault());
    }
}
