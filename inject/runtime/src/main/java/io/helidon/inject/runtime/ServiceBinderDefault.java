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

package io.helidon.inject.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceBinder;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderBindable;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.spi.ActivatorProvider;

/**
 * The default implementation for {@link ServiceBinder}.
 */
public class ServiceBinderDefault implements ServiceBinder {
    private static final Map<String, ActivatorProvider> ACTIVATOR_PROVIDERS;

    static {
        Map<String, ActivatorProvider> activators = new HashMap<>();

        HelidonServiceLoader.builder(ServiceLoader.load(ActivatorProvider.class))
                .addService(new InjectActivatorProvider())
                .build()
                .asList()
                .forEach(it -> activators.putIfAbsent(it.id(), it));
        ACTIVATOR_PROVIDERS = Map.copyOf(activators);
    }

    private final InjectionServices injectionServices;
    private final ServiceBinder serviceRegistry;
    private final boolean trusted;

    private ServiceBinderDefault(InjectionServices injectionServices,
                                 boolean trusted) {
        this.injectionServices = injectionServices;
        this.serviceRegistry = (ServiceBinder) injectionServices.services();
        this.trusted = trusted;
    }

    /**
     * Creates an instance of the default services binder.
     *
     * @param injectionServices the services registry instance
     * @param moduleName the module name
     * @param trusted are we in trusted mode (typically only set during early initialization sequence)
     * @return the newly created service binder
     */
    public static ServiceBinderDefault create(InjectionServices injectionServices,
                                              String moduleName,
                                              boolean trusted) {
        Objects.requireNonNull(injectionServices);
        Objects.requireNonNull(moduleName);
        return new ServiceBinderDefault(injectionServices, trusted);
    }

    @Override
    public void bind(ServiceSource<?> serviceDescriptor) {
        bind(serviceActivator(injectionServices, serviceDescriptor));
    }

    @Override
    public void bind(Activator<?> serviceActivator) {
        if (!trusted) {
            DefaultServices.assertPermitsDynamic(injectionServices.config());
        }

        serviceRegistry.bind(serviceActivator);
    }

    static Activator<?> serviceActivator(InjectionServices injectionServices, ServiceSource<?> serviceSource) {
        ActivatorProvider activatorProvider = ACTIVATOR_PROVIDERS.get(serviceSource.runtimeId());
        if (activatorProvider == null) {
            throw new IllegalStateException("Expected an activator provider for runtime id: " + serviceSource.runtimeId()
                                                    + ", available activator providers: " + ACTIVATOR_PROVIDERS.keySet());
        }
        return activatorProvider.activator(injectionServices, serviceSource);
    }

    /**
     * Returns the root provider of the service provider passed.
     *
     * @param sp the service provider
     * @return the root provider of the service provider, falling back to the service provider passed
     */
    public static ServiceProvider<?> toRootProvider(ServiceProvider<?> sp) {
        Optional<? extends ServiceProviderBindable<?>> bindable = sp.serviceProviderBindable();
        if (bindable.isPresent()) {
            sp = bindable.get().rootProvider().orElse(sp);
        }
        if (sp instanceof ServiceProviderBindable<?> spb) {
            return spb.rootProvider().orElse(sp);
        }
        return sp;
    }

}
