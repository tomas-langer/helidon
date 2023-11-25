package io.helidon.inject.codegen;

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.codegen.spi.InjectCodegenExtension;
import io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider;

public class UnsupportedTypesExtensionProvider implements InjectCodegenExtensionProvider {
    private static final Set<TypeName> TYPES = Set.of(
            InjectCodegenTypes.ANNOT_MANAGED_BEAN,
            InjectCodegenTypes.ANNOT_RESOURCE,
            InjectCodegenTypes.ANNOT_RESOURCES,
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
    public Set<String> supportedOptions() {
        return Set.of(InjectOptions.MAP_APPLICATION_TO_SINGLETON_SCOPE,
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