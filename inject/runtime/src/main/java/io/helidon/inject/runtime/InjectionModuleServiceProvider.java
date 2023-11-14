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
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ModuleComponent;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceSource;

/**
 * Basic {@link ModuleComponent} implementation. A ModuleComponent is-a service provider also.
 */
class InjectionModuleServiceProvider extends
                                     ServiceProviderBase<ModuleComponent, InjectionModuleServiceProvider,
                                             InjectionModuleServiceProvider.ModuleActivator> {

    InjectionModuleServiceProvider(InjectionServices injectionServices,
                                   ServiceSource<ModuleComponent> descriptor,
                                   ModuleComponent module) {
        super(injectionServices,
              descriptor,
              new ModuleActivator(injectionServices, descriptor, module));
    }

    static ServiceProvider<ModuleComponent> create(InjectionServices injectionServices,
                                                   ModuleComponent module,
                                                   String moduleName) {

        Set<Qualifier> qualifiers = Set.of(Qualifier.createNamed(moduleName));
        ServiceSource<ModuleComponent> descriptor = new ModuleServiceDescriptor(module.getClass(), qualifiers);
        return new InjectionModuleServiceProvider(injectionServices,
                                                  descriptor,
                                                  module);
    }

    static final class ModuleActivator extends ServiceActivatorBase<ModuleComponent, InjectionModuleServiceProvider> {
        ModuleActivator(InjectionServices injectionServices,
                        ServiceSource<ModuleComponent> descriptor,
                        ModuleComponent module) {
            super(injectionServices, descriptor);

            super.phase(Phase.ACTIVE);
            super.instance(module);
        }
    }

    private static class ModuleServiceDescriptor implements ServiceSource<ModuleComponent> {
        private static final TypeName MODULE_TYPE = TypeName.create(ModuleComponent.class);

        private final TypeName moduleType;
        private final Set<Qualifier> qualifiers;

        private ModuleServiceDescriptor(Class<?> moduleClass, Set<Qualifier> qualifiers) {
            this.moduleType = TypeName.create(moduleClass);
            this.qualifiers = qualifiers;
        }

        @Override
        public TypeName serviceType() {
            return moduleType;
        }

        @Override
        public Set<TypeName> contracts() {
            return Set.of(MODULE_TYPE);
        }

        @Override
        public Set<Qualifier> qualifiers() {
            return qualifiers;
        }
    }
}
