module io.helidon.inject.codegen.jakarta {
    requires io.helidon.inject.codegen;

    exports io.helidon.inject.codegen.jakarta;

    provides io.helidon.inject.codegen.spi.InjectAssignmentProvider
            with io.helidon.inject.codegen.jakarta.JakartaAssignmentProvider;

    provides io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider
            with io.helidon.inject.codegen.jakarta.JakartaExtensionProvider,
                    io.helidon.inject.codegen.jakarta.UnsupportedTypesExtensionProvider;

    provides io.helidon.codegen.spi.AnnotationMapperProvider
            with io.helidon.inject.codegen.jakarta.MapJakartaProvider,
                    io.helidon.inject.codegen.jakarta.MapApplicationScopedProvider;
}