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

package io.helidon.inject.runtime.testsubjects;

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectTypes;
import io.helidon.inject.service.Descriptor;
import io.helidon.inject.service.InjectionContext;
import io.helidon.inject.service.InterceptionMetadata;

import static io.helidon.inject.runtime.ServiceUtils.DEFAULT_INJECT_WEIGHT;

public class InjectionWorldImpl__ServiceDescriptor implements Descriptor<InjectionWorldImpl> {
    public static final InjectionWorldImpl__ServiceDescriptor INSTANCE = new InjectionWorldImpl__ServiceDescriptor();
    private static final TypeName TYPE_NAME = TypeName.create(InjectionWorldImpl.class);
    private static final Set<TypeName> CONTRACTS = Set.of(TypeName.create(InjectionWorld.class));

    InjectionWorldImpl__ServiceDescriptor() {
    }

    @Override
    public TypeName serviceType() {
        return TYPE_NAME;
    }

    @Override
    public Object instantiate(InjectionContext ctx, InterceptionMetadata interceptionMetadata) {
        return new InjectionWorldImpl();
    }

    @Override
    public Set<TypeName> contracts() {
        return CONTRACTS;
    }

    @Override
    public double weight() {
        return DEFAULT_INJECT_WEIGHT;
    }

    @Override
    public Set<TypeName> scopes() {
        return Set.of(InjectTypes.SINGLETON);
    }
}
