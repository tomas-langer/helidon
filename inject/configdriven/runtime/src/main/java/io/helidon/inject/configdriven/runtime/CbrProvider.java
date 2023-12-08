/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.inject.configdriven.runtime;

import java.util.Optional;

import io.helidon.common.config.Config;
import io.helidon.inject.api.ActivationPhaseReceiver;
import io.helidon.inject.api.Event;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.runtime.ServiceProviderBase;
import io.helidon.inject.service.Descriptor;

class CbrProvider extends ServiceProviderBase<ConfigBeanRegistryImpl>
        implements ServiceProvider<ConfigBeanRegistryImpl>,
                   ActivationPhaseReceiver {
    private static final ServiceInfoCriteria CONFIG_CRITERIA = ServiceInfoCriteria.builder()
            .addContract(Config.class)
            .build();
    private final InjectionServices injectionServices;

    protected CbrProvider(InjectionServices injectionServices,
                          Descriptor<ConfigBeanRegistryImpl> serviceSource) {
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
