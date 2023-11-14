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

import java.util.List;
import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.Application;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceDependencies;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceSource;

/**
 * Basic {@link Application} implementation. An application is-a service provider also.
 */
class InjectionApplicationServiceProvider extends
                                          ServiceProviderBase<Application, InjectionApplicationServiceProvider,
                                                  InjectionApplicationServiceProvider.AppActivator> {

    private InjectionApplicationServiceProvider(InjectionServices injectionServices,
                                                ServiceSource<Application> descriptor,
                                                Application app) {
        super(injectionServices,
              descriptor,
              new AppActivator(injectionServices, descriptor, app));
    }

    static ServiceProvider<Application> create(InjectionServices injectionServices,
                                               Application app) {

        Set<Qualifier> qualifiers = app.named().map(Qualifier::createNamed).map(Set::of).orElseGet(Set::of);
        ServiceSource<Application> descriptor = new AppServiceDescriptor(app.getClass(), qualifiers);
        return new InjectionApplicationServiceProvider(injectionServices, descriptor, app);
    }

    static final class AppActivator extends ServiceActivatorBase<Application, InjectionApplicationServiceProvider> {
        AppActivator(InjectionServices injectionServices, ServiceSource<Application> descriptor, Application app) {
            super(injectionServices, descriptor);

            super.phase(Phase.ACTIVE);
            super.instance(app);
        }
    }

    private static class AppServiceDescriptor implements ServiceSource<Application> {
        private static final TypeName APP_TYPE = TypeName.create(Application.class);
        private final TypeName appType;
        private final Set<Qualifier> qualifiers;

        private AppServiceDescriptor(Class<?> appClass, Set<Qualifier> qualifiers) {
            this.appType = TypeName.create(appClass);
            this.qualifiers = qualifiers;
        }

        @Override
        public TypeName serviceType() {
            return appType;
        }

        @Override
        public Set<TypeName> contracts() {
            return Set.of(APP_TYPE);
        }

        @Override
        public List<ServiceDependencies> dependencies() {
            return List.of();
        }

        @Override
        public Set<Qualifier> qualifiers() {
            return qualifiers;
        }
    }
}
