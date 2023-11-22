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

import java.util.Objects;
import java.util.Optional;

import io.helidon.inject.api.ActivationResult;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.DeActivationRequest;
import io.helidon.inject.api.InjectionException;
import io.helidon.inject.api.InjectionServiceProviderException;
import io.helidon.inject.api.Injector;
import io.helidon.inject.api.InjectorOptions;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceSource;

/**
 * Default reference implementation for the {@link Injector}.
 */
class DefaultInjector implements Injector {

    private final DefaultInjectionServices injectionServices;

    DefaultInjector(DefaultInjectionServices injectionServices) {
        this.injectionServices = injectionServices;
    }

    @Override
    public ActivationResult activateInject(ServiceSource<?> serviceSource,
                                           InjectorOptions opts) throws InjectionServiceProviderException {
        Objects.requireNonNull(serviceSource);
        Objects.requireNonNull(opts);

        if (opts.strategy() != Strategy.ANY && opts.strategy() != Strategy.ACTIVATOR) {
            return handleError(ActivationResult.builder(), opts, "only " + Strategy.ACTIVATOR + " strategy is supported", null);
        }

        Activator<?> activator = ServiceBinderDefault.serviceActivator(injectionServices, serviceSource);

        activator.serviceProvider();

        return activator.activate(opts.activationRequest());
    }

    @Override
    public ActivationResult deactivate(ServiceProvider<?> serviceProvider,
                                       InjectorOptions opts) throws InjectionServiceProviderException {
        Objects.requireNonNull(serviceProvider);
        Objects.requireNonNull(opts);

        ActivationResult.Builder resultBuilder = ActivationResult.builder();

        if (opts.strategy() != Strategy.ANY && opts.strategy() != Strategy.ACTIVATOR) {
            return handleError(resultBuilder, opts, "only " + Strategy.ACTIVATOR + " strategy is supported", serviceProvider);
        }

        resultBuilder.serviceProvider(serviceProvider);

        // each service must have an activator, as that is how we bind services to the registry
        Activator<?> activator = injectionServices.services(true)
                .flatMap(defaultServices -> defaultServices.activator(serviceProvider))
                .or(() -> serviceProvider instanceof Activator<?> act ? Optional.of(act) : Optional.empty())
                .orElseThrow(() -> new InjectionServiceProviderException("Service activator not available.",
                                                                         serviceProvider));

        return activator.deactivate(DeActivationRequest.builder()
                                            .throwIfError(opts.activationRequest().throwIfError())
                                            .build());
    }

    private ActivationResult handleError(ActivationResult.Builder resultBuilder,
                                         InjectorOptions opts,
                                         String message,
                                         ServiceProvider<?> serviceProvider) {
        InjectionException e = (serviceProvider == null)
                ? new InjectionException(message) : new InjectionServiceProviderException(message, serviceProvider);
        resultBuilder.error(e);
        if (opts.activationRequest().throwIfError()) {
            throw e;
        }
        return resultBuilder.build();
    }

}
