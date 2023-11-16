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

package io.helidon.inject.tools;

import io.helidon.common.types.TypeName;

/**
 * Type name constants.
 * <p>
 * This should always be used instead of dependency on the annotation and other class types.
 */
public final class TypeNames {
    /**
     * Package prefix {@value}.
     */
    public static final String PREFIX_JAKARTA = "jakarta.";
    /**
     * Package prefix {@value}.
     */
    public static final String PREFIX_JAVAX = "javax.";

    /**
     * Injection {@value} type.
     */
    public static final String INJECT_APPLICATION = "io.helidon.inject.api.Application";

    /**
     * Injection {@value} annotation.
     *
     * @deprecated use {@link #CONTRACT_TYPE} instead
     */
    @Deprecated(forRemoval = true, since = "4.1.0")
    public static final String CONTRACT = "io.helidon.inject.api.Contract";
    /**
     * Injection contract annotation.
     */
    public static final TypeName CONTRACT_TYPE = TypeName.create(CONTRACT);
    /**
     * Injection {@value} annotation.
     */
    public static final String EXTERNAL_CONTRACTS = "io.helidon.inject.api.ExternalContracts";
    /**
     * Injection external contract annotation.
     */
    public static final TypeName EXTERNAL_CONTRACTS_TYPE = TypeName.create(EXTERNAL_CONTRACTS);
    /**
     * Injection {@value} annotation.
     */
    public static final String INTERCEPTED = "io.helidon.inject.api.Intercepted";
    /**
     * Injection {@value #INTERCEPTED} annotation type.
     */
    public static final TypeName INTERCEPTED_TYPE = TypeName.create(INTERCEPTED);
    /**
     * Injection {@value} type.
     */
    public static final String INJECT_MODULE = "io.helidon.inject.api.ModuleComponent";

    /**
     * Injection {@value} annotation.
     */
    public static final String CONFIGURED_BY = "io.helidon.inject.configdriven.api.ConfiguredBy";

    /**
     * Injection class name {@value} for {@code InjectionPointProvider}.
     */
    public static final String INJECTION_POINT_PROVIDER = "io.helidon.inject.api.InjectionPointProvider";
    /**
     * Injection service provider type.
     */
    public static final TypeName SERVICE_PROVIDER_TYPE = TypeName.create("io.helidon.inject.api.ServiceProvider");
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_APPLICATION_SCOPED = "jakarta.enterprise.context.ApplicationScoped";
    /**
     * Jakarta {@value #JAKARTA_APPLICATION_SCOPED} annotation type.
     */
    public static final TypeName JAKARTA_APPLICATION_SCOPED_TYPE = TypeName.create(JAKARTA_APPLICATION_SCOPED);
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_INJECT = "jakarta.inject.Inject";
    /**
     * Jakarta {@value #JAKARTA_INJECT} annotation type.
     */
    public static final TypeName JAKARTA_INJECT_TYPE = TypeName.create(JAKARTA_INJECT);
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_MANAGED_BEAN = "jakarta.annotation.ManagedBean";
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_POST_CONSTRUCT = "jakarta.annotation.PostConstruct";
    /**
     * Jakarta {@value #JAKARTA_POST_CONSTRUCT} annotation type.
     */
    public static final TypeName JAKARTA_POST_CONSTRUCT_TYPE = TypeName.create(JAKARTA_POST_CONSTRUCT);
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_PRE_DESTROY = "jakarta.annotation.PreDestroy";
    /**
     * Jakarta {@value #JAKARTA_PRE_DESTROY} annotation type.
     */
    public static final TypeName JAKARTA_PRE_DESTROY_TYPE = TypeName.create(JAKARTA_PRE_DESTROY);
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_PRIORITY = "jakarta.annotation.Priority";
    /**
     * Jakarta {@value} type.
     */
    public static final String JAKARTA_PROVIDER = "jakarta.inject.Provider";
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_QUALIFIER = "jakarta.inject.Qualifier";
    /**
     * Jakarta {@value} annotation.
     */
    public static final TypeName JAKARTA_QUALIFIER_TYPE = TypeName.create(JAKARTA_QUALIFIER);
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_RESOURCE = "jakarta.annotation.Resource";
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_RESOURCES = "jakarta.annotation.Resources";
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_SCOPE = "jakarta.inject.Scope";
    /**
     * Jakarta {@value #JAKARTA_SCOPE} annotation type.
     */
    public static final TypeName JAKARTA_SCOPE_TYPE = TypeName.create(JAKARTA_SCOPE);
    /**
     * Jakarta {@value} annotation.
     */
    public static final String JAKARTA_SINGLETON = "jakarta.inject.Singleton";
    /**
     * Jakarta {@value #JAKARTA_SINGLETON} annotation type.
     */
    public static final TypeName JAKARTA_SINGLETON_TYPE = TypeName.create(JAKARTA_SINGLETON);
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_ACTIVATE_REQUEST_CONTEXT = "jakarta.enterprise.context.control.ActivateRequestContext";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_ALTERNATIVE = "jakarta.enterprise.inject.Alternative";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_BEFORE_DESTROYED = "jakarta.enterprise.context.BeforeDestroyed";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_CONVERSATION_SCOPED = "jakarta.enterprise.context.ConversationScoped";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_DEPENDENT = "jakarta.enterprise.context.Dependent";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_DESTROYED = "jakarta.enterprise.context.Destroyed";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_DISPOSES = "jakarta.enterprise.inject.Disposes";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_INITIALIZED = "jakarta.enterprise.context.Initialized";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_INTERCEPTED = "jakarta.enterprise.inject.Intercepted";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_MODEL = "jakarta.enterprise.inject.Model";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_NONBINDING = "jakarta.enterprise.util.Nonbinding";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_NORMAL_SCOPE = "jakarta.enterprise.context.NormalScope";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_OBSERVES = "jakarta.enterprise.event.Observes";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_OBSERVES_ASYNC = "jakarta.enterprise.event.ObservesAsync";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_PRODUCES = "jakarta.enterprise.inject.Produces";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_REQUEST_SCOPED = "jakarta.enterprise.context.RequestScoped";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_SESSION_SCOPED = "jakarta.enterprise.context.SessionScoped";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_SPECIALIZES = "jakarta.enterprise.inject.Specializes";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_STEREOTYPE = "jakarta.enterprise.inject.Stereotype";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_TRANSIENT_REFERENCE = "jakarta.enterprise.inject.TransientReference";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_TYPED = "jakarta.enterprise.inject.Typed";
    /**
     * Jakarta CDI {@value} annotation.
     */
    public static final String JAKARTA_CDI_VETOED = "jakarta.enterprise.inject.Vetoed";
    /**
     * Jakarta legacy {@value} annotation.
     */
    public static final String JAVAX_APPLICATION_SCOPED = "javax.enterprise.context.ApplicationScoped";
    /**
     * Jakarta legacy {@value} annotation.
     */
    public static final String JAVAX_INJECT = "javax.inject.Inject";
    /**
     * Jakarta legacy {@value} annotation.
     */
    public static final String JAVAX_QUALIFIER = "javax.inject.Qualifier";
    /**
     * Jakarta legacy {@value} type.
     */
    public static final String JAVAX_PROVIDER = "javax.inject.Provider";
    public static final TypeName SERVICE_DESCRIPTOR_TYPE = TypeName.create("io.helidon.inject.api.ServiceDescriptor");
    public static final TypeName SERVICE_SOURCE_TYPE = TypeName.create("io.helidon.inject.api.ServiceSource");
    public static final TypeName INJECTION_CONTEXT = TypeName.create("io.helidon.inject.api.InjectionContext");
    public static final TypeName INJECTION_PARAMETER_ID = TypeName.create("io.helidon.inject.api.IpId");
    public static final TypeName INJECTION_PARAMETER_INFO = TypeName.create("io.helidon.inject.api.IpInfo");
    public static final TypeName RUN_LEVEL_TYPE = TypeName.create("io.helidon.inject.api.RunLevel");
    public static final TypeName HELIDON_QUALIFIER = TypeName.create("io.helidon.inject.api.Qualifier");
    public static final TypeName INTERCEPTED_TRIGGER = TypeName.create("io.helidon.inject.api.InterceptedTrigger");
    public static final TypeName ANNOTATION_RETENTION = TypeName.create("java.lang.annotation.Retention");
    public static final TypeName INTERCEPTION_METADATA = TypeName.create("io.helidon.inject.api.InterceptionMetadata");
    public static final TypeName APPLICATION = TypeName.create("io.helidon.inject.api.Application");
    public static final TypeName NAMED = TypeName.create("jakarta.inject.Named");

    private TypeNames() {
    }
}
