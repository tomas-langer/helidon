package io.helidon.inject.codegen.javax;

import java.util.Set;

import io.helidon.codegen.Option;
import io.helidon.common.types.TypeName;
import io.helidon.inject.codegen.InjectOptions;
import io.helidon.inject.codegen.InjectionCodegenContext;
import io.helidon.inject.codegen.spi.InjectCodegenExtension;
import io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider;

public class UnsupportedTypesExtensionProvider implements InjectCodegenExtensionProvider {
    private static final Set<TypeName> TYPES = Set.of(
            JavaxTypes.ANNOT_MANAGED_BEAN,
            JavaxTypes.ANNOT_RESOURCE,
            JavaxTypes.ANNOT_RESOURCES,
            CdiTypes.APPLICATION_SCOPED,
            CdiTypes.BEFORE_DESTROYED,
            CdiTypes.CONVERSATION_SCOPED,
            CdiTypes.DEPENDENT,
            CdiTypes.DESTROYED,
            CdiTypes.INITIALIZED,
            CdiTypes.NORMAL_SCOPE,
            CdiTypes.REQUEST_SCOPED,
            CdiTypes.SESSION_SCOPED,
            CdiTypes.ACTIVATE_REQUEST_CONTEXT,
            CdiTypes.OBSERVES,
            CdiTypes.OBSERVES_ASYNC,
            CdiTypes.ALTERNATIVE,
            CdiTypes.DISPOSES,
            CdiTypes.INTERCEPTED,
            CdiTypes.MODEL,
            CdiTypes.PRODUCES,
            CdiTypes.SPECIALIZES,
            CdiTypes.STEREOTYPE,
            CdiTypes.TRANSIENT_REFERENCE,
            CdiTypes.TYPED,
            CdiTypes.VETOED,
            CdiTypes.NONBINDING
    );

    @Override
    public Set<Option<?>> supportedOptions() {
        return Set.of(MapApplicationScopedProvider.MAP_APPLICATION_TO_SINGLETON_SCOPE,
                      InjectOptions.IGNORE_UNSUPPORTED_ANNOTATIONS);
    }

    @Override
    public Set<TypeName> supportedAnnotations() {
        return TYPES;
    }

    @Override
    public InjectCodegenExtension create(InjectionCodegenContext codegenContext) {
        return new UnsupportedTypesExtension(codegenContext);
    }
}
