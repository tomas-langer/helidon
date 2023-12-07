module io.helidon.inject.codegen.javax {
    requires io.helidon.inject.codegen;

    exports io.helidon.inject.codegen.javax;

    provides io.helidon.inject.codegen.spi.InjectAssignmentProvider
            with io.helidon.inject.codegen.javax.JavaxAssignmentProvider;

    provides io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider
            with io.helidon.inject.codegen.javax.JavaxExtensionProvider,
                    io.helidon.inject.codegen.javax.UnsupportedTypesExtensionProvider;

    provides io.helidon.codegen.spi.AnnotationMapperProvider
            with io.helidon.inject.codegen.javax.MapJavaxProvider,
                    io.helidon.inject.codegen.javax.MapApplicationScopedProvider;
}