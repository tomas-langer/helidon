module io.helidon.inject.configdriven.codegen {
    requires io.helidon.inject.codegen;

    exports io.helidon.inject.configdriven.codegen;

    provides io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider
            with io.helidon.inject.configdriven.codegen.ConfigDrivenCodegenProvider;

    provides io.helidon.codegen.spi.AnnotationMapperProvider
            with io.helidon.inject.configdriven.codegen.MapConfigDrivenProvider;
}