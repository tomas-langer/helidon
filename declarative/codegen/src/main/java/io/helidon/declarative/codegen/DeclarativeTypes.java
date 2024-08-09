package io.helidon.declarative.codegen;

import java.util.Set;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;

import static io.helidon.service.codegen.ServiceCodegenTypes.INJECTION_SINGLETON;

public final class DeclarativeTypes {
    public static final TypeName THROWABLE = TypeName.create(Throwable.class);
    public static final Annotation SINGLETON_ANNOTATION = Annotation.create(INJECTION_SINGLETON);
    public static final TypeName SET_OF_THROWABLES = TypeName.builder(TypeName.create(Set.class))
            .addTypeArgument(TypeName.builder(TypeName.create(Class.class))
                                     .addTypeArgument(TypeName.builder(TypeName.create(Throwable.class))
                                                              .wildcard(true)
                                                              .build())
                                     .build())
            .build();

    private DeclarativeTypes() {
    }
}
