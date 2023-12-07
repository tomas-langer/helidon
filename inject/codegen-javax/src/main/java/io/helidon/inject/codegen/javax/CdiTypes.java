package io.helidon.inject.codegen.javax;

import io.helidon.common.types.TypeName;

final class CdiTypes {
    static final TypeName APPLICATION_SCOPED = TypeName.create("javax.enterprise.context.ApplicationScoped");

    static final TypeName ACTIVATE_REQUEST_CONTEXT = TypeName.create("javax.enterprise.context.control.ActivateRequestContext");

    static final TypeName ALTERNATIVE = TypeName.create("javax.enterprise.inject.Alternative");

    static final TypeName BEFORE_DESTROYED = TypeName.create("javax.enterprise.context.BeforeDestroyed");

    static final TypeName CONVERSATION_SCOPED = TypeName.create("javax.enterprise.context.ConversationScoped");

    static final TypeName DEPENDENT = TypeName.create("javax.enterprise.context.Dependent");

    static final TypeName DESTROYED = TypeName.create("javax.enterprise.context.Destroyed");

    static final TypeName DISPOSES = TypeName.create("javax.enterprise.inject.Disposes");

    static final TypeName INITIALIZED = TypeName.create("javax.enterprise.context.Initialized");

    static final TypeName INTERCEPTED = TypeName.create("javax.enterprise.inject.Intercepted");

    static final TypeName MODEL = TypeName.create("javax.enterprise.inject.Model");

    static final TypeName NONBINDING = TypeName.create("javax.enterprise.util.Nonbinding");

    static final TypeName NORMAL_SCOPE = TypeName.create("javax.enterprise.context.NormalScope");

    static final TypeName OBSERVES = TypeName.create("javax.enterprise.event.Observes");

    static final TypeName OBSERVES_ASYNC = TypeName.create("javax.enterprise.event.ObservesAsync");

    static final TypeName PRODUCES = TypeName.create("javax.enterprise.inject.Produces");

    static final TypeName REQUEST_SCOPED = TypeName.create("javax.enterprise.context.RequestScoped");

    static final TypeName SESSION_SCOPED = TypeName.create("javax.enterprise.context.SessionScoped");

    static final TypeName SPECIALIZES = TypeName.create("javax.enterprise.inject.Specializes");

    static final TypeName STEREOTYPE = TypeName.create("javax.enterprise.inject.Stereotype");

    static final TypeName TRANSIENT_REFERENCE = TypeName.create("javax.enterprise.inject.TransientReference");

    static final TypeName TYPED = TypeName.create("javax.enterprise.inject.Typed");

    static final TypeName VETOED = TypeName.create("javax.enterprise.inject.Vetoed");

    private CdiTypes() {
    }
}
