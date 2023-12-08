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

package io.helidon.inject.tests.helidon.inject;

import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.Services;
import io.helidon.inject.testing.InjectionTestingSupport;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.helidon.common.testing.junit5.OptionalMatcher.optionalPresent;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

/*
 All code generation should be done as part of main source processing, we can just start service registry and test
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InjectionTest {
    private static Services services;
    private static LifecycleReceiver lifecycleReceiver;
    private static InjectionServices injectionServices;

    @BeforeAll
    static void initRegistry() {
        injectionServices = InjectionTestingSupport.testableServices();
        services = injectionServices.services();
    }

    @AfterAll
    static void tearDownRegistry() {
        injectionServices.shutdown();
        if (lifecycleReceiver != null) {
            assertThat("Pre destroy of a singleton should have been called", lifecycleReceiver.preDestroyCalled(), is(true));
        }

        InjectionTestingSupport.resetAll();
    }

    @Test
    @Order(0)
    void testSingleton() {
        ServiceProvider<SingletonService> provider = services.lookup(SingletonService.class);

        assertThat(provider, notNullValue());

        SingletonService first = provider.get();
        assertThat(first, notNullValue());

        SingletonService second = provider.get();
        // singleton should always yield the same instance
        assertThat(first, sameInstance(second));
    }

    @Test
    @Order(1)
    void testLifecycle() {
        ServiceProvider<LifecycleReceiver> provider = services.lookup(LifecycleReceiver.class);

        assertThat(provider, notNullValue());

        lifecycleReceiver = provider.get();
        assertThat(lifecycleReceiver.postConstructCalled(), is(true));
    }

    @Test
    @Order(2)
    void testNonSingleton() {
        ServiceProvider<NonSingletonService> provider = services.lookup(NonSingletonService.class);

        assertThat(provider, notNullValue());

        NonSingletonService first = provider.get();
        assertThat(first, notNullValue());

        NonSingletonService second = provider.get();
        // non-singleton should always yield different instance
        assertThat(first, not(sameInstance(second)));

        SingletonService firstSingleton = first.singletonService();
        SingletonService secondSingleton = second.singletonService();
        // singleton should always yield the same instance
        assertThat(firstSingleton, sameInstance(secondSingleton));
    }

    @Test
    @Order(3)
    void testNamed() {
        ServiceProvider<NamedReceiver> provider = services.lookup(NamedReceiver.class);

        assertThat(provider, notNullValue());

        NamedReceiver instance = provider.get();
        assertThat(instance.named(), notNullValue());
        assertThat(instance.named().name(), is("named"));
    }

    @Test
    @Order(4)
    void testQualified() {
        ServiceProvider<QualifiedReceiver> provider = services.lookup(QualifiedReceiver.class);

        assertThat(provider, notNullValue());

        QualifiedReceiver instance = provider.get();
        assertThat(instance.qualified(), notNullValue());
        assertThat(instance.qualified().qualifier(), is("qualified"));
    }

    @Test
    @Order(4)
    void testProvider() {
        ServiceProvider<ProviderReceiver> provider = services.lookup(ProviderReceiver.class);

        assertThat(provider, notNullValue());

        ProviderReceiver instance = provider.get();
        assertThat(instance.nonSingletonService(), notNullValue());
        assertThat(instance.listOfServices(), not(empty()));
        assertThat(instance.optionalService(), optionalPresent());
        assertThat(instance.contract(), notNullValue());

        NonSingletonService first = instance.nonSingletonService();
        NonSingletonService second = instance.nonSingletonService();
        assertThat(first, not(sameInstance(second)));
    }
}