package io.helidon.inject.codegen;

import io.helidon.common.types.TypeName;

public final class InjectCodegenTypes {
    public static final TypeName INJECT_POINT = TypeName.create("io.helidon.inject.service.Inject.Point");
    public static final TypeName INJECT_SINGLETON = TypeName.create("io.helidon.inject.service.Inject.Singleton");
    public static final TypeName INJECT_NAMED = TypeName.create("io.helidon.inject.service.Inject.Named");
    public static final TypeName INJECT_CLASS_NAMED = TypeName.create("io.helidon.inject.service.Inject.ClassNamed");
    public static final TypeName INJECT_QUALIFIER = TypeName.create("io.helidon.inject.service.Inject.Qualifier");
    public static final TypeName INJECT_POST_CONSTRUCT = TypeName.create("io.helidon.inject.service.Inject.PostConstruct");
    public static final TypeName INJECT_PRE_DESTROY = TypeName.create("io.helidon.inject.service.Inject.PreDestroy");
    public static final TypeName INJECT_CONTRACT = TypeName.create("io.helidon.inject.service.Inject.Contract");
    public static final TypeName INJECT_EXTERNAL_CONTRACTS = TypeName.create("io.helidon.inject.service.Inject"
                                                                                     + ".ExternalContracts");
    public static final TypeName INJECT_SERVICE = TypeName.create("io.helidon.inject.service.Inject.Service");
    public static final TypeName RUN_LEVEL = TypeName.create("io.helidon.inject.service.Inject.RunLevel");

    public static final TypeName SERVICE_INFO = TypeName.create("io.helidon.inject.service.ServiceInfo");
    public static final TypeName SERVICE_DESCRIPTOR = TypeName.create("io.helidon.inject.service.Descriptor");
    public static final TypeName INVOKER = TypeName.create("io.helidon.inject.service.Invoker");
    public static final TypeName INTERCEPTED_TRIGGER = TypeName.create("io.helidon.inject.service.InterceptedTrigger");
    public static final TypeName QUALIFIER = TypeName.create("io.helidon.inject.service.Qualifier");
    public static final TypeName IP_ID = TypeName.create("io.helidon.inject.service.IpId");

    public static final TypeName SERVICE_SOURCE_METHOD = TypeName.create("io.helidon.inject.service.Descriptor.MethodSignature");
    public static final TypeName SERVICE_PROVIDER = TypeName.create("io.helidon.inject.api.ServiceProvider");
    public static final TypeName INJECTION_POINT_PROVIDER = TypeName.create("io.helidon.inject.api.InjectionPointProvider");
    public static final TypeName INJECTION_CONTEXT = TypeName.create("io.helidon.inject.service.InjectionContext");
    public static final TypeName INTERCEPTION_METADATA = TypeName.create("io.helidon.inject.service.InterceptionMetadata");
    public static final TypeName MODULE_COMPONENT = TypeName.create("io.helidon.inject.service.ModuleComponent");
    public static final TypeName APPLICATION = TypeName.create("io.helidon.inject.api.Application");
    public static final TypeName CONTEXT_QUERY = TypeName.create("io.helidon.inject.api.ContextualServiceQuery");
    public static final TypeName SERVICE_BINDER = TypeName.create("io.helidon.inject.service.ServiceBinder");

    private InjectCodegenTypes() {
    }
}
