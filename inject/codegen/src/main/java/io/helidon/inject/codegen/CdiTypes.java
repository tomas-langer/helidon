package io.helidon.inject.codegen;

import io.helidon.common.types.TypeName;

final class CdiTypes {
    static final TypeName APPLICATION_SCOPED = TypeName.create("jakarta.enterprise.context.ApplicationScoped");

    static final TypeName ACTIVATE_REQUEST_CONTEXT = TypeName.create("jakarta.enterprise.context.control.ActivateRequestContext");

    static final TypeName ALTERNATIVE = TypeName.create("jakarta.enterprise.inject.Alternative");

    static final TypeName BEFORE_DESTROYED = TypeName.create("jakarta.enterprise.context.BeforeDestroyed");

    static final TypeName CONVERSATION_SCOPED = TypeName.create("jakarta.enterprise.context.ConversationScoped");

    static final TypeName DEPENDENT = TypeName.create("jakarta.enterprise.context.Dependent");

    static final TypeName DESTROYED = TypeName.create("jakarta.enterprise.context.Destroyed");

    static final TypeName DISPOSES = TypeName.create("jakarta.enterprise.inject.Disposes");

    static final TypeName INITIALIZED = TypeName.create("jakarta.enterprise.context.Initialized");

    static final TypeName INTERCEPTED = TypeName.create("jakarta.enterprise.inject.Intercepted");

    static final TypeName MODEL = TypeName.create("jakarta.enterprise.inject.Model");

    static final TypeName NONBINDING = TypeName.create("jakarta.enterprise.util.Nonbinding");

    static final TypeName NORMAL_SCOPE = TypeName.create("jakarta.enterprise.context.NormalScope");

    static final TypeName OBSERVES = TypeName.create("jakarta.enterprise.event.Observes");

    static final TypeName OBSERVES_ASYNC = TypeName.create("jakarta.enterprise.event.ObservesAsync");

    static final TypeName PRODUCES = TypeName.create("jakarta.enterprise.inject.Produces");

    static final TypeName REQUEST_SCOPED = TypeName.create("jakarta.enterprise.context.RequestScoped");

    static final TypeName SESSION_SCOPED = TypeName.create("jakarta.enterprise.context.SessionScoped");

    static final TypeName SPECIALIZES = TypeName.create("jakarta.enterprise.inject.Specializes");

    static final TypeName STEREOTYPE = TypeName.create("jakarta.enterprise.inject.Stereotype");

    static final TypeName TRANSIENT_REFERENCE = TypeName.create("jakarta.enterprise.inject.TransientReference");

    static final TypeName TYPED = TypeName.create("jakarta.enterprise.inject.Typed");

    static final TypeName VETOED = TypeName.create("jakarta.enterprise.inject.Vetoed");

    private CdiTypes() {
    }
}
