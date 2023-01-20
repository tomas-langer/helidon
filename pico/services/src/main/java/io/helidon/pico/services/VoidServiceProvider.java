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

package io.helidon.pico.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.pico.ContextualServiceQuery;
import io.helidon.pico.DefaultServiceInfo;
import io.helidon.pico.ServiceProvider;

import jakarta.inject.Singleton;

/**
 * A proxy service provider created internally by the framework.
 */
class VoidServiceProvider extends AbstractServiceProvider<Void> {
    static final VoidServiceProvider INSTANCE = new VoidServiceProvider() {};
    static final List<ServiceProvider<?>> LIST_INSTANCE = List.of(INSTANCE);

    private VoidServiceProvider() {
        serviceInfo(DefaultServiceInfo.builder()
                .serviceTypeName(getServiceTypeName())
                .addContractsImplemented(getServiceTypeName())
                .activatorTypeName(VoidServiceProvider.class.getName())
                .addScopeTypeName(Singleton.class.getName())
                .declaredWeight(DEFAULT_WEIGHT)
                .build());
    }

    public static String getServiceTypeName() {
        return Void.class.getName();
    }

    @Override
    protected Void createServiceProvider(
            Map<String, Object> resolvedDeps) {
        return null;
    }

    @Override
    public Optional<Void> first(
            ContextualServiceQuery query) {
        return Optional.empty();
    }

}