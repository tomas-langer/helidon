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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectionContext;
import io.helidon.inject.api.InterceptionMetadata;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.IpInfo;
import io.helidon.inject.api.ServiceDependencies;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.runtime.ServiceUtils;

import jakarta.inject.Provider;

/**
 * Serves as an exemplar of what will is normally code generated.
 */
public class HelloInjectionImpl__ServiceDescriptor implements ServiceSource<HelloInjectionWorldImpl> {
    public static final HelloInjectionImpl__ServiceDescriptor INSTANCE = new HelloInjectionImpl__ServiceDescriptor();
    private static final TypeName TYPE_NAME = TypeName.create(HelloInjectionWorldImpl.class);
    private static final Set<TypeName> CONTRACTS = Set.of(TypeName.create(HelloInjectionWorld.class));

    private static final TypeName TYPE_0 = TypeName.create("io.helidon.inject.runtime.testsubjects.InjectionWorld");
    private static final TypeName TYPE_1 = TypeName.create("jakarta.inject.Provider<io.helidon.inject.runtime.testsubjects.InjectionWorld>");
    private static final TypeName TYPE_2 = TypeName.create("java.util.List<jakarta.inject.Provider<io.helidon.inject.runtime.testsubjects.InjectionWorld>>");
    private static final TypeName TYPE_3 = TypeName.create("java.util.List<io.helidon.inject.runtime.testsubjects.InjectionWorld>");
    private static final TypeName TYPE_4 = TypeName.create("java.util.Optional<io.helidon.inject.runtime.testsubjects.InjectionWorld>");

    public static final IpId<InjectionWorld> IP_0 = IpId.<InjectionWorld>builder()
            .elementKind(ElementKind.FIELD)
            .name("world")
            .typeName(TYPE_0)
            .build();

    private static final IpInfo IPI_0 = IpInfo.builder()
            .id(IP_0)
            .field("IP_0")
            .contract(TYPE_0)
            .build();

    public static final IpId<Provider<InjectionWorld>> IP_1 = IpId.<Provider<InjectionWorld>>builder()
            .elementKind(ElementKind.FIELD)
            .name("worldRef")
            .typeName(TYPE_1)
            .build();

    private static final IpInfo IPI_1 = IpInfo.builder()
            .id(IP_1)
            .field("IP_1")
            .contract(TYPE_0)
            .build();

    public static final IpId<List<Provider<InjectionWorld>>> IP_2 = IpId.<List<Provider<InjectionWorld>>>builder()
            .elementKind(ElementKind.FIELD)
            .name("listOfWorldRefs")
            .typeName(TYPE_2)
            .build();

    private static final IpInfo IPI_2 = IpInfo.builder()
            .id(IP_2)
            .field("IP_2")
            .contract(TYPE_0)
            .build();

    public static final IpId<List<InjectionWorld> > IP_3 = IpId.<List<InjectionWorld> >builder()
            .elementKind(ElementKind.FIELD)
            .name("listOfWorlds")
            .typeName(TYPE_3)
            .build();

    private static final IpInfo IPI_3 = IpInfo.builder()
            .id(IP_3)
            .field("IP_3")
            .contract(TYPE_0)
            .build();

    public static final IpId<Optional<InjectionWorld>> IP_4 = IpId.<Optional<InjectionWorld>>builder()
            .elementKind(ElementKind.FIELD)
            .name("redWorld")
            .typeName(TYPE_4)
            .build();

    private static final IpInfo IPI_4 = IpInfo.builder()
            .id(IP_4)
            .field("IP_4")
            .contract(TYPE_0)
            .build();

    public static final IpId<InjectionWorld> IP_5 = IpId.<InjectionWorld>builder()
            .elementKind(ElementKind.METHOD)
            .name("world_1_world")
            .typeName(TYPE_0)
            .build();

    private static final IpInfo IPI_5 = IpInfo.builder()
            .id(IP_5)
            .field("IP_5")
            .contract(TYPE_0)
            .build();
    private static final ServiceDependencies DEPENDENCIES = new ServiceDependencies(TYPE_NAME,
                                                                                    List.of(IPI_0,
                                                                                            IPI_1,
                                                                                            IPI_2,
                                                                                            IPI_3,
                                                                                            IPI_4,
                                                                                            IPI_5));

    public HelloInjectionImpl__ServiceDescriptor() {
    }

    @Override
    public TypeName serviceType() {
        return TYPE_NAME;
    }

    @Override
    public Set<TypeName> contracts() {
        return CONTRACTS;
    }

    @Override
    public double weight() {
        return ServiceUtils.DEFAULT_INJECT_WEIGHT;
    }

    @Override
    public List<ServiceDependencies> dependencies() {
        return List.of(DEPENDENCIES);
    }

    @Override
    public Object instantiate(InjectionContext ctx, InterceptionMetadata interceptionMetadata) {
        return new HelloInjectionWorldImpl();
    }

    @Override
    public void injectFields(InjectionContext ctx, InterceptionMetadata interceptionMetadata, HelloInjectionWorldImpl instance) {
        instance.world = ctx.param(TYPE_NAME, IP_0);
        instance.worldRef = ctx.param(TYPE_NAME, IP_1);
        instance.listOfWorldRefs = ctx.param(TYPE_NAME, IP_2);
        instance.listOfWorlds = ctx.param(TYPE_NAME, IP_3);
        instance.redWorld = ctx.param(TYPE_NAME, IP_4);
    }

    @Override
    public void injectMethods(InjectionContext ctx, HelloInjectionWorldImpl instance) {
        instance.world(ctx.param(TYPE_NAME, IP_5));
    }

    @Override
    public void postConstruct(HelloInjectionWorldImpl instance) {
        instance.postConstruct();
    }

    @Override
    public void preDestroy(HelloInjectionWorldImpl instance) {
        instance.preDestroy();
    }
}
