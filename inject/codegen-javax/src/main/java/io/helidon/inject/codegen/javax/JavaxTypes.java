package io.helidon.inject.codegen.javax;

import io.helidon.common.types.TypeName;

final class JavaxTypes {
    static final TypeName ANNOT_MANAGED_BEAN = TypeName.create("javax.annotation.ManagedBean");
    static final TypeName ANNOT_RESOURCE = TypeName.create("javax.annotation.Resource");
    static final TypeName ANNOT_RESOURCES = TypeName.create("javax.annotation.Resources");
    static final TypeName INJECT_SINGLETON = TypeName.create("javax.inject.Singleton");
    static final TypeName INJECT_PRE_DESTROY = TypeName.create("javax.annotation.PreDestroy");
    static final TypeName INJECT_POST_CONSTRUCT = TypeName.create("javax.annotation.PostConstruct");
    static final TypeName INJECT_INJECT = TypeName.create("javax.inject.Inject");
    static final TypeName INJECT_SCOPE = TypeName.create("javax.inject.Scope");
    static final TypeName INJECT_QUALIFIER = TypeName.create("javax.inject.Qualifier");
    static final TypeName INJECT_PROVIDER = TypeName.create("javax.inject.Provider");
    static final TypeName INJECT_NAMED = TypeName.create("javax.inject.Named");

    private JavaxTypes() {
    }
}
