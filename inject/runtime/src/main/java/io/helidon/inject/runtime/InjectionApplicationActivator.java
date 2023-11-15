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

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.Application;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceSource;

/**
 * Basic {@link Application} implementation. An application is-a service provider also.
 */
class InjectionApplicationActivator extends ServiceProviderBase<Application> {

    private InjectionApplicationActivator(InjectionServices injectionServices,
                                          ServiceSource<Application> descriptor) {
        super(injectionServices, descriptor);
    }

    static InjectionApplicationActivator create(InjectionServices injectionServices,
                                                Application app) {

        Set<Qualifier> qualifiers = Set.of(Qualifier.createNamed(app.name()));
        ServiceSource<Application> descriptor = new AppServiceDescriptor(app.getClass(), qualifiers);
        InjectionApplicationActivator activator = new InjectionApplicationActivator(injectionServices,
                                                                                    descriptor);

        activator.state(Phase.ACTIVE, app);
        return activator;
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
        public Set<Qualifier> qualifiers() {
            return qualifiers;
        }
    }
}
