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

package io.helidon.inject.configdriven.runtime;

import java.util.Optional;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.service.ServiceInfo;

class ConfigBeanServiceProvider<CB> implements ServiceProvider<CB> {
    private final ConfigBeanServiceDescriptor<CB> serviceDescriptor;
    private final CB instance;
    private final String id;

    ConfigBeanServiceProvider(TypeName beanType, CB instance, String id) {
        this.instance = instance;
        this.id = id;
        this.serviceDescriptor = new ConfigBeanServiceDescriptor<>(beanType, id);
    }

    @Override
    public Optional<CB> first(ContextualServiceQuery query) {
        return Optional.of(instance);
    }

    @Override
    public String id() {
        return ServiceProvider.super.id() + "{" + id + "}";
    }

    @Override
    public String description() {
        return serviceInfo().serviceType().classNameWithEnclosingNames() + "{" + id + "}:ACTIVE";
    }

    @Override
    public ServiceInfo<CB> serviceInfo() {
        return serviceDescriptor;
    }

    @Override
    public Phase currentActivationPhase() {
        return Phase.ACTIVE;
    }
}
