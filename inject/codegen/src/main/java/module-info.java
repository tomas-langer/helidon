module io.helidon.inject.codegen {
    requires transitive io.helidon.builder.api;
    requires transitive io.helidon.codegen.classmodel;
    requires transitive io.helidon.codegen;

    exports io.helidon.inject.codegen;
    exports io.helidon.inject.codegen.spi;

    uses io.helidon.inject.codegen.spi.InjectCodegenObserverProvider;
    uses io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider;
    uses io.helidon.inject.codegen.spi.InjectAssignmentProvider;

    provides io.helidon.codegen.spi.CodegenExtensionProvider
            with io.helidon.inject.codegen.InjectCodegenProvider;

    provides io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider
            with io.helidon.inject.codegen.InjectionExtensionProvider;

    provides io.helidon.codegen.spi.AnnotationMapperProvider
            with io.helidon.inject.codegen.MapClassNamedProvider;
}