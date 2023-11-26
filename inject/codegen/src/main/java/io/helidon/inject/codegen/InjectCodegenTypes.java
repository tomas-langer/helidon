package io.helidon.inject.codegen;

import io.helidon.common.types.TypeName;

public final class InjectCodegenTypes {
    public static final TypeName ANNOT_MANAGED_BEAN = TypeName.create("jakarta.annotation.ManagedBean");
    public static final TypeName ANNOT_RESOURCE = TypeName.create("jakarta.annotation.Resource");
    public static final TypeName ANNOT_RESOURCES = TypeName.create("jakarta.annotation.Resources");
    public static final TypeName INJECT_SINGLETON = TypeName.create("jakarta.inject.Singleton");
    public static final TypeName INJECT_PRE_DESTROY = TypeName.create("jakarta.annotation.PreDestroy");
    public static final TypeName INJECT_POST_CONSTRUCT = TypeName.create("jakarta.annotation.PostConstruct");
    public static final TypeName INJECT_INJECT = TypeName.create("jakarta.inject.Inject");
    public static final TypeName INJECT_SCOPE = TypeName.create("jakarta.inject.Scope");
    public static final TypeName INJECT_QUALIFIER = TypeName.create("jakarta.inject.Qualifier");
    public static final TypeName INJECT_PROVIDER = TypeName.create("jakarta.inject.Provider");
    public static final TypeName INJECT_NAMED = TypeName.create("jakarta.inject.Named");
    public static final TypeName JAVAX_INJECT_PROVIDER = TypeName.create("javax.inject.Provider");
    public static final TypeName HELIDON_INTERCEPTED = TypeName.create("io.helidon.inject.api.Intercepted");
    public static final TypeName HELIDON_INTERCEPTED_TRIGGER = TypeName.create("io.helidon.inject.api.InterceptedTrigger");
    public static final TypeName HELIDON_QUALIFIER = TypeName.create("io.helidon.inject.api.Qualifier");
    public static final TypeName HELIDON_IP_ID = TypeName.create("io.helidon.inject.api.IpId");
    public static final TypeName HELIDON_SERVICE_SOURCE = TypeName.create("io.helidon.inject.api.ServiceSource");
    public static final TypeName HELIDON_SERVICE_SOURCE_METHOD = TypeName.create("io.helidon.inject.api.ServiceSource.MethodSignature");
    public static final TypeName HELIDON_SERVICE_PROVIDER = TypeName.create("io.helidon.inject.api.ServiceProvider");
    public static final TypeName HELIDON_INJECTION_POINT_PROVIDER = TypeName.create("io.helidon.inject.api.InjectionPointProvider");
    public static final TypeName HELIDON_CONTRACT = TypeName.create("io.helidon.inject.api.Contract");
    public static final TypeName HELIDON_EXTERNAL_CONTRACTS = TypeName.create("io.helidon.inject.api.ExternalContracts");
    public static final TypeName HELIDON_INJECTION_CONTEXT = TypeName.create("io.helidon.inject.api.InjectionContext");
    public static final TypeName HELIDON_INTERCEPTION_METADATA = TypeName.create("io.helidon.inject.api.InterceptionMetadata");
    public static final TypeName HELIDON_RUN_LEVEL = TypeName.create("io.helidon.inject.api.RunLevel");
    public static final TypeName HELIDON_MODULE_COMPONENT = TypeName.create("io.helidon.inject.api.ModuleComponent");
    public static final TypeName HELIDON_APPLICATION = TypeName.create("io.helidon.inject.api.Application");
    public static final TypeName HELIDON_DESCRIBE = TypeName.create("io.helidon.inject.api.Describe");
    public static final TypeName HELIDON_CLASS_NAMED = TypeName.create("io.helidon.inject.api.ClassNamed");
    public static final TypeName HELIDON_CONTEXT_QUERY = TypeName.create("io.helidon.inject.api.ContextualServiceQuery");

    private InjectCodegenTypes() {
    }
}
