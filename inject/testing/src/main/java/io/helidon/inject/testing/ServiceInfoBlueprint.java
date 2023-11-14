/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

package io.helidon.inject.testing;

import java.util.Optional;
import java.util.Set;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.Weighted;
import io.helidon.common.types.TypeName;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.RunLevel;
import io.helidon.inject.api.ServiceDescriptor;

/**
 * Describes a managed service or injection point.
 *
 * @see io.helidon.inject.api.Services
 * @see io.helidon.inject.api.ServiceInfoCriteria
 */
@Prototype.Blueprint
interface ServiceInfoBlueprint<T> extends ServiceDescriptor<T> {
    /**
     * The managed service implementation type.
     *
     * @return the service type name
     */
    TypeName serviceType();

    /**
     * The managed service assigned Scope's.
     *
     * @return the service scope type name
     */
    @Option.Singular
    Set<TypeName> scopes();

    /**
     * The managed service assigned Qualifier's.
     *
     * @return the service qualifiers
     */
    @Option.Singular
    Set<Qualifier> qualifiers();

    /**
     * The managed services advertised types (i.e., typically its interfaces).
     *
     * @return the service contracts implemented
     * @see io.helidon.inject.api.ExternalContracts
     */
    @Option.Singular
    Set<TypeName> contracts();

    /**
     * The optional {@link io.helidon.inject.api.RunLevel} ascribed to the service.
     *
     * @return the service's run level
     * @see #runLevel()
     */
    Optional<Integer> declaredRunLevel();

    /**
     * Weight that was declared on the type itself.
     *
     * @return the declared weight
     * @see #weight()
     */
    Optional<Double> declaredWeight();

    /**
     * Runtime ID if there is a desire to override the default one.
     * This ID chooses the correct service framework implementation at runtime, and in most cases should be kept default.
     *
     * @return runtime ID to use
     */
    Optional<String> declaredRuntimeId();

    @Override
    default String runtimeId() {
        return declaredRuntimeId().orElseGet(ServiceDescriptor.super::runtimeId);
    }

    @Override
    default double weight() {
        return declaredWeight().orElse(Weighted.DEFAULT_WEIGHT);
    }

    @Override
    default int runLevel() {
        return declaredRunLevel().orElse(RunLevel.NORMAL);
    }
}
