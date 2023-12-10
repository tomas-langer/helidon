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

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import io.helidon.inject.service.InjectionContext;
import io.helidon.inject.service.Ip;

/**
 * A context for obtaining injection point values in a {@link io.helidon.inject.service.Descriptor}.
 * This context is pre-filled with the correct providers either based on an {@link io.helidon.inject.api.Application},
 * or based on analysis during activation of a service provider.
 *
 * @see io.helidon.inject.service.InjectionContext
 */
public class HelidonInjectionContext implements InjectionContext {
    private final Map<Ip, Supplier<?>> injectionPlans;

    HelidonInjectionContext(Map<Ip, Supplier<?>> injectionPlans) {
        this.injectionPlans = injectionPlans;
    }

    /**
     * Create an injection context based on a map of providers.
     *
     * @param injectionPlan map of injection ids to provider that satisfies that injection point
     * @return a new injection context
     */
    public static InjectionContext create(Map<Ip, Supplier<?>> injectionPlan) {
        return new HelidonInjectionContext(injectionPlan);
    }

    @Override
    @SuppressWarnings("unchecked") // we have a map, and that cannot have type to instance values
    public <T> T param(Ip paramId) {
        Supplier<?> injectionSupplier = injectionPlans.get(paramId);
        if (injectionSupplier == null) {
            throw new NoSuchElementException("Cannot resolve injection id " + paramId + " for service "
                                                     + paramId.service().fqName()
                                                     + ", this dependency was not declared in "
                                                     + "the service descriptor");
        }

        return (T) injectionSupplier.get();
    }
}
