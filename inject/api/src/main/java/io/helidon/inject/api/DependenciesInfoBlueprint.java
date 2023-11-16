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

package io.helidon.inject.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.types.TypeName;

/**
 * Represents a per {@link ServiceInfo} mapping of {@link DependencyInfo}'s. These are typically assigned to a
 * {@link ServiceProvider} via compile-time code generation within the Injection framework.
 */
@Prototype.Blueprint
interface DependenciesInfoBlueprint {
    /**
     * Represents the set of dependencies for each {@link ServiceInfo}.
     *
     * @return map from the service info to its dependencies
     */
    @Option.Singular("serviceInfoDependency")
    Map<ServiceInfoCriteria, Set<DependencyInfo>> serviceInfoDependencies();

    /**
     * Optionally, the service type name aggregating {@link #allDependencies()}.
     *
     * @return the optional service type name for which these dependencies belong
     */
    Optional<TypeName> fromServiceTypeName();

    /**
     * Represents a flattened set of all dependencies.
     *
     * @return the flattened set of all dependencies
     */
    default Set<DependencyInfo> allDependencies() {
        Set<DependencyInfo> all = new TreeSet<>(DependencyInfoComparator.instance());
        serviceInfoDependencies().values()
                .forEach(all::addAll);
        return all;
    }
}
