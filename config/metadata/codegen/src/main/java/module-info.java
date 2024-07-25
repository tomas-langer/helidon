module io.helidon.config.metadata.codegen {
    requires io.helidon.codegen;
    requires io.helidon.metadata.hjson;
    requires java.compiler;

    exports io.helidon.config.metadata.codegen;

    provides io.helidon.codegen.spi.CodegenExtensionProvider
            with io.helidon.config.metadata.codegen.ConfigMetadataCodegenProvider;
}