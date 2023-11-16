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
import java.util.Optional;
import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceProvider;

import jakarta.inject.Singleton;

/**
 * A proxy service provider created internally by the framework.
 */
public class VoidServiceProvider extends DescribedServiceProvider<Void> {
    static final TypeName TYPE_NAME = TypeName.create(Void.class);
    public static final VoidServiceProvider INSTANCE = new VoidServiceProvider();
    public static final List<ServiceProvider<?>> LIST_INSTANCE = List.of(INSTANCE);

    private VoidServiceProvider() {
        super(new VoidDescriptor());
    }

    @Override
    public Phase currentActivationPhase() {
        return Phase.ACTIVE;
    }

    @Override
    public Void get() {
        return null;
    }

    @Override
    public Optional<Void> first(ContextualServiceQuery query) {
        return Optional.empty();
    }

    private static class VoidDescriptor implements ServiceDescriptor<Void> {
        static final Set<TypeName> SCOPES = Set.of(TypeName.create(Singleton.class));
        private static final Set<TypeName> CONTRACTS = Set.of(TYPE_NAME);

        @Override
        public TypeName serviceType() {
            return TYPE_NAME;
        }

        @Override
        public Set<TypeName> contracts() {
            return CONTRACTS;
        }

        @Override
        public Set<TypeName> scopes() {
            return SCOPES;
        }
    }
}
