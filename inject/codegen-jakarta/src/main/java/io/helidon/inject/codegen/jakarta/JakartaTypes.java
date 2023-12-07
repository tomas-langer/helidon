package io.helidon.inject.codegen.jakarta;

import io.helidon.common.types.TypeName;

public class JakartaTypes {
    public static final TypeName ANNOT_MANAGED_BEAN = TypeName.create("jakarta.annotation.ManagedBean");
    public static final TypeName ANNOT_RESOURCE = TypeName.create("jakarta.annotation.Resource");
    public static final TypeName ANNOT_RESOURCES = TypeName.create("jakarta.annotation.Resources");
    public static final TypeName INJECT_SINGLETON = TypeName.create("jakarta.inject.Singleton");
    public static final TypeName INJECT_PRE_DESTROY = TypeName.create("jakarta.annotation.PreDestroy");
    public static final TypeName INJECT_POST_CONSTRUCT = TypeName.create("jakarta.annotation.PostConstruct");
    public static final TypeName INJECT_INJECT = TypeName.create("jakarta.inject.Inject");
    public static final TypeName INJECT_SCOPE = TypeName.create("jakarta.inject.Scope");
    public static final TypeName INJECT_QUALIFIER = TypeName.create("jakarta.inject.Qualifier");
    public static final TypeName INJECT_NAMED = TypeName.create("jakarta.inject.Named");
    public static final TypeName INJECT_PROVIDER = TypeName.create("jakarta.inject.Provider");
}
