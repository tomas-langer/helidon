package io.helidon.inject.codegen.javax;

import io.helidon.common.types.TypeName;

public class JavaxTypes {
    public static final TypeName ANNOT_MANAGED_BEAN = TypeName.create("javax.annotation.ManagedBean");
    public static final TypeName ANNOT_RESOURCE = TypeName.create("javax.annotation.Resource");
    public static final TypeName ANNOT_RESOURCES = TypeName.create("javax.annotation.Resources");
    public static final TypeName INJECT_SINGLETON = TypeName.create("javax.inject.Singleton");
    public static final TypeName INJECT_PRE_DESTROY = TypeName.create("javax.annotation.PreDestroy");
    public static final TypeName INJECT_POST_CONSTRUCT = TypeName.create("javax.annotation.PostConstruct");
    public static final TypeName INJECT_INJECT = TypeName.create("javax.inject.Inject");
    public static final TypeName INJECT_SCOPE = TypeName.create("javax.inject.Scope");
    public static final TypeName INJECT_QUALIFIER = TypeName.create("javax.inject.Qualifier");
    public static final TypeName INJECT_PROVIDER = TypeName.create("javax.inject.Provider");
    public static final TypeName INJECT_NAMED = TypeName.create("javax.inject.Named");
}
