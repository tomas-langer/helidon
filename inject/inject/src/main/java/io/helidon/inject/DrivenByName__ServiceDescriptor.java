/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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

package io.helidon.inject;

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.inject.service.Injection;
import io.helidon.inject.service.ServiceDescriptor;

/**
 * Service descriptor to enable injection of String name of a {@link io.helidon.inject.service.Injection.DrivenBy}
 * service.
 *
 * @deprecated not intended for direct use by users, implementation detail of the service registry, must be public,
 *  as it may be used in generated applications
 */
@Deprecated
@SuppressWarnings({"checkstyle:TypeName", "DeprecatedIsStillUsed"}) // matches pattern of generated descriptors
public class DrivenByName__ServiceDescriptor implements ServiceDescriptor<String> {
    /**
     * Singleton instance to be referenced when building applications.
     */
    public static final DrivenByName__ServiceDescriptor INSTANCE = new DrivenByName__ServiceDescriptor();

    private static final TypeName INFO_TYPE = TypeName.create(DrivenByName__ServiceDescriptor.class);
    private static final Set<TypeName> CONTRACTS = Set.of(TypeNames.STRING);

    private DrivenByName__ServiceDescriptor() {
    }

    @Override
    public TypeName serviceType() {
        return INFO_TYPE;
    }

    @Override
    public TypeName infoType() {
        return INFO_TYPE;
    }

    @Override
    public Set<TypeName> contracts() {
        return CONTRACTS;
    }

    @Override
    public TypeName scope() {
        return Injection.Singleton.TYPE_NAME;
    }
}
