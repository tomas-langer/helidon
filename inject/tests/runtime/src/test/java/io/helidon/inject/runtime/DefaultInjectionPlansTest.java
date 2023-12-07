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

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.helidon.common.types.TypeName;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.inject.api.Bootstrap;
import io.helidon.inject.api.InjectTypes;
import io.helidon.inject.api.InjectionPointProvider;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.service.Descriptor;
import io.helidon.inject.service.ModuleComponent;
import io.helidon.inject.service.Qualifier;
import io.helidon.inject.service.ServiceBinder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

class DefaultInjectionPlansTest {
    static final FakeInjectionPointProviderActivator sp1 = new FakeInjectionPointProviderActivator();
    static final FakeRegularActivator sp2 = new FakeRegularActivator();

    Config config = Config.builder(
                    ConfigSources.create(
                            Map.of("inject.permits-dynamic", "true"), "config-1"))
            .disableEnvironmentVariablesSource()
            .disableSystemPropertiesSource()
            .build();

    @BeforeEach
    void init() {
        InjectionServices.globalBootstrap(Bootstrap.builder().config(config).build());
    }

    @AfterEach
    void tearDown() {
        SimpleInjectionTestingSupport.resetAll();
    }

    /**
     * Also exercised in examples/inject.
     */
    @Test
    void testInjectionPointResolversFor() {
        InjectionServices injectionServices = InjectionServices.injectionServices().orElseThrow();
        DefaultServices services = (DefaultServices) InjectionServices.realizedServices();
        services.bind(injectionServices, new FakeModuleComponent(), true);

        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder()
                .addQualifier(Qualifier.createNamed("whatever"))
                .addContract(Closeable.class)
                .build();
        List<String> result = DefaultInjectionPlans.injectionPointProvidersFor(services, criteria).stream()
                .map(ServiceProvider::description).toList();
        assertThat(result,
                   contains(sp1.serviceType().classNameWithEnclosingNames() + ":INIT"));
    }

    static class FakeModuleComponent implements ModuleComponent {
        @Override
        public String name() {
            return "fake";
        }

        @Override
        public void configure(ServiceBinder binder) {
            binder.bind(sp1);
            binder.bind(sp2);
        }
    }

    static class FakeInjectionPointProviderActivator implements Descriptor<Closeable> {
        private static final TypeName SERVICE_TYPE = TypeName.create(FakeInjectionPointProviderActivator.class);
        private static final TypeName CLOSEABLE = TypeName.create(Closeable.class);
        private static final TypeName IP_PROVIDER = TypeName.create(InjectionPointProvider.class);
        private static final TypeName PROVIDER = TypeName.create(Supplier.class);
        private static final Set<TypeName> CONTRACTS = Set.of(CLOSEABLE, IP_PROVIDER, PROVIDER);

        FakeInjectionPointProviderActivator() {
        }

        @Override
        public TypeName serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        public Set<TypeName> contracts() {
            return CONTRACTS;
        }

        @Override
        public Set<TypeName> scopes() {
            return Set.of(InjectTypes.SINGLETON);
        }
    }

    static class FakeRegularActivator implements Descriptor<Closeable> {
        private static final TypeName SERVICE_TYPE = TypeName.create(FakeRegularActivator.class);
        private static final TypeName CLOSEABLE = TypeName.create(Closeable.class);
        private static final TypeName PROVIDER = TypeName.create(Supplier.class);
        private static final Set<TypeName> CONTRACTS = Set.of(CLOSEABLE, PROVIDER);

        @Override
        public TypeName serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        public Set<TypeName> contracts() {
            return CONTRACTS;
        }

        @Override
        public Set<TypeName> scopes() {
            return Set.of(InjectTypes.SINGLETON);
        }
    }

}
