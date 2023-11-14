package io.helidon.inject.configdriven.runtime;

import java.util.Map;
import java.util.function.Supplier;

import io.helidon.inject.api.ActivationPhaseReceiver;
import io.helidon.inject.api.ActivationRequest;
import io.helidon.inject.api.ActivationResult;
import io.helidon.inject.api.Event;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.IpInfo;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.api.Services;
import io.helidon.inject.runtime.ServiceActivatorBase;

class ConfigDrivenServiceActivator<T, CB> extends ServiceActivatorBase<T, ConfigDrivenServiceProvider<T, CB>>
        implements ActivationPhaseReceiver {

    ConfigDrivenServiceActivator(InjectionServices injectionServices,
                                 ServiceSource<T> descriptor) {
        super(injectionServices, descriptor);
    }

    @Override
    public void onPhaseEvent(Event event,
                             Phase phase) {
        if (phase == Phase.POST_BIND_ALL_MODULES) {
            ActivationResult.Builder res = ActivationResult.builder();

            if (Phase.INIT == phase()) {
                stateTransitionStart(res, Phase.PENDING);
            }

            // one of the configured services need to "tickle" the bean registry to initialize
            ConfigBeanRegistryImpl cbr = ConfigBeanRegistryImpl.CONFIG_BEAN_REGISTRY.get();
            if (cbr != null) {
                cbr.initialize();
            }
        } else if (phase == Phase.FINAL_RESOLVE) {
            // post-initialize ourselves
            if (provider().drivesActivation()) {
                activate(InjectionServices.createActivationRequestDefault());
            }

            provider().resolveConfigDrivenServices();
        } else if (phase == Phase.SERVICES_READY) {
            provider().activateConfigDrivenServices();
        }
    }

    @Override
    public String toString() {
        return "Config Driven Service activator for: " + descriptor().serviceType().fqName() + "[" + phase() + "]";
    }

    @Override
    protected void prepareDependency(Services services, Map<IpId<?>, Supplier<?>> injectionPlan, IpInfo dependency) {
        // do nothing, config driven root service CANNOT be instantiated, as it does not have
        // a config bean to inject
    }

    @Override
    protected void construct(ActivationRequest req, ActivationResult.Builder res) {
        // do nothing, config driven root service CANNOT be instantiated, as it does not have a config bean

        if (!(provider().hasManagedServices() && provider().drivesActivation())) {
            stateTransitionStart(res, Phase.PENDING);
        } else {
            stateTransitionStart(res, Phase.CONSTRUCTING);
        }

    }
}
