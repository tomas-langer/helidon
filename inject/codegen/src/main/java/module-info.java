import io.helidon.inject.codegen.InjectionExtensionProvider;

module io.helidon.inject.codegen {
    requires transitive io.helidon.builder.api;
    requires transitive io.helidon.codegen.classmodel;
    requires transitive io.helidon.codegen;

    exports io.helidon.inject.codegen;
    exports io.helidon.inject.codegen.spi;

    uses io.helidon.inject.codegen.spi.InjectCodegenObserverProvider;
    uses io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider;

    provides io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider
            with InjectionExtensionProvider,
                    io.helidon.inject.codegen.UnsupportedTypesExtensionProvider;

    provides io.helidon.codegen.spi.AnnotationMapperProvider
            with io.helidon.inject.codegen.MapJavaxProvider,
                    io.helidon.inject.codegen.MapApplicationScopedProvider,
                    io.helidon.inject.codegen.MapClassNamedProvider;
}