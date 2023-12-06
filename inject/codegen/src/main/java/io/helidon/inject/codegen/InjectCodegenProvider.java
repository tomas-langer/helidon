package io.helidon.inject.codegen;

import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.Option;
import io.helidon.codegen.spi.CodegenExtension;
import io.helidon.codegen.spi.CodegenExtensionProvider;
import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.types.TypeName;
import io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider;

public class InjectCodegenProvider implements CodegenExtensionProvider {
    private static final List<InjectCodegenExtensionProvider> EXTENSIONS =
            HelidonServiceLoader.create(ServiceLoader.load(InjectCodegenExtensionProvider.class,
                                                           InjectCodegen.class.getClassLoader()))
                    .asList();
    private static final Set<Option<?>> SUPPORTED_OPTIONS = EXTENSIONS.stream()
            .flatMap(it -> it.supportedOptions().stream())
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<TypeName> SUPPORTED_ANNOTATIONS = EXTENSIONS.stream()
            .flatMap(it -> it.supportedAnnotations().stream())
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> SUPPORTED_ANNOTATION_PACKAGES =
            Stream.concat(EXTENSIONS.stream()
                                  .flatMap(it -> it.supportedAnnotationPackages()
                                          .stream()),
                          Stream.of("jakarta."))
                    .collect(Collectors.toUnmodifiableSet());

    @Override
    public Set<Option<?>> supportedOptions() {
        return SUPPORTED_OPTIONS;
    }

    @Override
    public Set<TypeName> supportedAnnotations() {
        return SUPPORTED_ANNOTATIONS;
    }

    @Override
    public Set<String> supportedAnnotationPackages() {
        return SUPPORTED_ANNOTATION_PACKAGES;
    }

    @Override
    public CodegenExtension create(CodegenContext ctx, TypeName generatorType) {
        return InjectCodegen.create(ctx, generatorType, EXTENSIONS);
    }

}
