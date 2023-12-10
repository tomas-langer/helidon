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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.common.Weighted;
import io.helidon.common.types.TypeName;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.service.Ip;
import io.helidon.inject.service.ModuleComponent;

/**
 * Public helpers around shared services usages.
 */
public final class ServiceUtils {
    /**
     * Default weight for any <i>internal</i> Injection service component. It is defined to be
     * {@link io.helidon.common.Weighted#DEFAULT_WEIGHT} {@code - 1} in order to allow any other service implementation to
     * naturally have a higher weight (since it will use the {@code DEFAULT_WEIGHT} unless explicitly overridden.
     */
    public static final double DEFAULT_INJECT_WEIGHT = Weighted.DEFAULT_WEIGHT - 1;

    private static final TypeName MODULE_COMPONENT = TypeName.create(ModuleComponent.class);
    private static final TypeName APPLICATION = TypeName.create(ModuleComponent.class);

    private ServiceUtils() {
    }

    /**
     * Determines if the service provider is valid to receive injections.
     *
     * @param sp the service provider
     * @return true if the service provider can receive injection
     */
    public static boolean isQualifiedInjectionTarget(ServiceProvider<?> sp) {
        Set<TypeName> contractsImplemented = sp.contracts();
        List<Ip> dependencies = sp.dependencies();

        return (!dependencies.isEmpty())
                || (!contractsImplemented.isEmpty()
                    && !contractsImplemented.contains(MODULE_COMPONENT)
                    && !contractsImplemented.contains(APPLICATION));
    }

    /**
     * Provides a {@link ServiceProvider#description()}, falling back to {@link #toString()} on the passed
     * provider argument.
     *
     * @param provider the provider
     * @return the description
     */
    public static String toDescription(Object provider) {
        if (provider instanceof Optional) {
            provider = ((Optional<?>) provider).orElse(null);
        }

        if (provider instanceof ServiceProvider) {
            return ((ServiceProvider<?>) provider).description();
        }
        return String.valueOf(provider);
    }

    /**
     * Provides a {@link ServiceProvider#description()}, falling back to {@link #toString()} on the passed
     * provider argument.
     *
     * @param coll the collection of providers
     * @return the description
     */
    public static List<String> toDescriptions(Collection<?> coll) {
        return coll.stream().map(ServiceUtils::toDescription).collect(Collectors.toList());
    }

}
