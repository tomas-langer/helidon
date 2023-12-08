package io.helidon.inject.codegen.spi;

import io.helidon.codegen.spi.CodegenProvider;
import io.helidon.inject.codegen.InjectionCodegenContext;

/**
 * A {@link java.util.ServiceLoader} provider interface for extensions of code generators for Helidon Inject.
 * The difference between this extension and a general {@link io.helidon.codegen.spi.CodegenExtensionProvider} is that
 * this provider has access to {@link io.helidon.inject.codegen.InjectionCodegenContext}.
 */
public interface InjectCodegenExtensionProvider extends CodegenProvider {
    /**
     * Create a new extension based on the context.
     *
     * @param codegenContext injection code generation context
     * @return a new extension
     */
    InjectCodegenExtension create(InjectionCodegenContext codegenContext);
}
