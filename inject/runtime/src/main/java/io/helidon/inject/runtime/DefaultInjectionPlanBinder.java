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

import java.util.Optional;

import io.helidon.inject.api.IpId;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceInjectionPlanBinder;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderBindable;

class DefaultInjectionPlanBinder implements ServiceInjectionPlanBinder, ServiceInjectionPlanBinder.Binder {

    private final DefaultServices services;

    DefaultInjectionPlanBinder(DefaultServices services) {
        this.services = services;
    }

    @Override
    public Binder bindTo(ServiceDescriptor<?> untrustedSp) {
        // don't trust what we get, but instead lookup the service provider that we carry in our services registry
        ServiceProvider<?> serviceProvider = services.serviceProvider(untrustedSp);
        Optional<? extends ServiceProviderBindable<?>> bindable = serviceProvider.serviceProviderBindable();
        Optional<Binder> binder = (bindable.isPresent()) ? bindable.get().injectionPlanBinder() : Optional.empty();
        if (binder.isEmpty()) {
            // basically this means this service will not support compile-time injection
            DefaultInjectionServices.LOGGER.log(System.Logger.Level.WARNING,
                                "service provider is not capable of being bound to injection points: " + serviceProvider);
            return this;
        } else {
            if (DefaultInjectionServices.LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
                DefaultInjectionServices.LOGGER.log(System.Logger.Level.DEBUG, "binding injection plan to " + binder.get());
            }
        }

        return binder.get();
    }

    @Override
    public Binder bindNull(IpId ipIdentity) {
        // NOP
        return this;
    }

    @Override
    public Binder bind(IpId id, boolean useProvider, ServiceDescriptor<?> descriptor) {
        return this;
    }

    @Override
    public Binder bindOptional(IpId id, boolean useProvider, ServiceDescriptor<?>... descriptor) {
        return this;
    }

    @Override
    public Binder bindMany(IpId id, boolean useProvider, ServiceDescriptor<?>... serviceProviders) {
        return this;
    }

    @Override
    public Binder runtimeBind(IpId id, boolean useProvider, Class<?> serviceType) {
        return this;
    }

    @Override
    public Binder runtimeBindOptional(IpId id, boolean useProvider, Class<?> serviceType) {
        return this;
    }

    @Override
    public Binder runtimeBindMany(IpId id, boolean useProvider, Class<?> serviceType) {
        return this;
    }

    @Override
    public Binder runtimeBindNullable(IpId id, boolean useProvider, Class<?> serviceType) {
        return this;
    }

    @Override
    public void commit() {
        // NOP
    }

}
