package io.helidon.inject.configdriven.runtime;

import java.util.Optional;

import io.helidon.common.config.Config;
import io.helidon.inject.api.ActivationPhaseReceiver;
import io.helidon.inject.api.Event;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.runtime.ServiceProviderBase;

class CbrProvider extends ServiceProviderBase<ConfigBeanRegistryImpl>
        implements ServiceProvider<ConfigBeanRegistryImpl>,
                   ActivationPhaseReceiver {
    private static final ServiceInfoCriteria CONFIG_CRITERIA = ServiceInfoCriteria.builder()
            .addContract(Config.class)
            .build();
    private final InjectionServices injectionServices;

    protected CbrProvider(InjectionServices injectionServices,
                          ServiceSource<ConfigBeanRegistryImpl> serviceSource) {
        super(injectionServices, serviceSource);
        this.injectionServices = injectionServices;
    }

    @Override
    public void onPhaseEvent(Event event,
                             Phase phase) {

        if (phase == Phase.POST_BIND_ALL_MODULES) {
            Optional<ServiceProvider<?>> configProvider = injectionServices.services()
                    .lookupFirst(CONFIG_CRITERIA, false);

            ConfigBeanRegistryImpl cbr = ConfigBeanRegistryImpl.CONFIG_BEAN_REGISTRY.get();
            if (configProvider.isPresent() && InjectionServices.terminalActivationPhase() == Phase.ACTIVE) {
                cbr.initialize((Config) configProvider.get().get());
            } else {
                cbr.initialize();
            }
        }
    }
}