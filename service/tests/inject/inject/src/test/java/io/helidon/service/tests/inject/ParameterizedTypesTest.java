/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.service.tests.inject;

import io.helidon.service.inject.InjectConfig;
import io.helidon.service.inject.InjectRegistryManager;
import io.helidon.service.inject.api.InjectRegistry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParameterizedTypesTest {
    private static InjectRegistryManager registryManager;
    private static InjectRegistry registry;

    @BeforeAll
    public static void initRegistry() {
        var injectConfig = InjectConfig.builder()
                .addServiceDescriptor(ParameterizedTypes_GenericProducerString__ServiceDescriptor.INSTANCE)
                .addServiceDescriptor(ParameterizedTypes_GenericProducerInt__ServiceDescriptor.INSTANCE)
                .addServiceDescriptor(ParameterizedTypes_GenericContractReceiver__ServiceDescriptor.INSTANCE)
                .discoverServices(false)
                .discoverServicesFromServiceLoader(false)
                .build();
        registryManager = InjectRegistryManager.create(injectConfig);
        registry = registryManager.registry();
    }

    @AfterAll
    public static void tearDownRegistry() {
        registryManager.shutdown();
    }

    @Test
    void testReceiver() {
        var receiver = registry.get(ParameterizedTypes.GenericContractReceiver.class);

        assertThat(receiver.getString(), is("Hello-42"));
    }
}
