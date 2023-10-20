package io.helidon.common.processor;

import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.processor.spi.AnnotationMapper;
import io.helidon.common.processor.spi.AnnotationMapperProvider;
import io.helidon.common.processor.spi.ElementMapper;
import io.helidon.common.processor.spi.ElementMapperProvider;
import io.helidon.common.processor.spi.ProcessingProvider;
import io.helidon.common.processor.spi.TypeMapper;
import io.helidon.common.processor.spi.TypeMapperProvider;
import io.helidon.common.types.TypeName;

class ProcessingContextImpl implements ProcessingContext {
    private final ProcessingEnvironment aptEnv;
    private final AptOptions aptOptions;
    private final List<ElementMapper> elementMappers;
    private final List<TypeMapper> typeMappers;
    private final List<AnnotationMapper> annotationMappers;
    private final Set<String> supportedOptions;
    private final Set<String> supportedPackages;
    private final Set<TypeName> supportedAnnotations;

    ProcessingContextImpl(ProcessingEnvironment aptEnv) {
        this.aptEnv = aptEnv;
        this.aptOptions = AptOptions.create(aptEnv);

        Set<String> supportedOptions = new HashSet<>();
        Set<String> supportedPackages = new HashSet<>();
        Set<TypeName> supportedAnnotations = new HashSet<>();

        this.annotationMappers = HelidonServiceLoader.create(
                        ServiceLoader.load(AnnotationMapperProvider.class,
                                           TypeInfoFactory.class.getClassLoader()))
                .stream()
                .peek(it -> addSupported(it, supportedOptions, supportedPackages, supportedAnnotations))
                .map(it -> it.create(aptEnv, aptOptions))
                .toList();

        this.elementMappers = HelidonServiceLoader.create(
                        ServiceLoader.load(ElementMapperProvider.class,
                                           TypeInfoFactory.class.getClassLoader()))
                .stream()
                .peek(it -> addSupported(it, supportedOptions, supportedPackages, supportedAnnotations))
                .map(it -> it.create(aptEnv, aptOptions))
                .toList();

        this.typeMappers = HelidonServiceLoader.create(
                        ServiceLoader.load(TypeMapperProvider.class,
                                           TypeInfoFactory.class.getClassLoader()))
                .stream()
                .peek(it -> addSupported(it, supportedOptions, supportedPackages, supportedAnnotations))
                .map(it -> it.create(aptEnv, aptOptions))
                .toList();

        this.supportedOptions = Set.copyOf(supportedOptions);
        this.supportedPackages = Set.copyOf(supportedPackages);
        this.supportedAnnotations = Set.copyOf(supportedAnnotations);
    }

    @Override
    public ProcessingEnvironment aptEnv() {
        return aptEnv;
    }

    @Override
    public AptOptions options() {
        return aptOptions;
    }

    @Override
    public List<ElementMapper> elementMappers() {
        return elementMappers;
    }

    @Override
    public List<TypeMapper> typeMappers() {
        return typeMappers;
    }

    @Override
    public List<AnnotationMapper> annotationMappers() {
        return annotationMappers;
    }

    @Override
    public Set<TypeName> mapperSupportedAnnotations() {
        return supportedAnnotations;
    }

    @Override
    public Set<String> mapperSupportedAnnotationPackages() {
        return supportedPackages;
    }

    @Override
    public Set<String> mapperSupportedOptions() {
        return supportedOptions;
    }

    private static void addSupported(ProcessingProvider<?> provider,
                                     Set<String> supportedOptions,
                                     Set<String> supportedPackages,
                                     Set<TypeName> supportedAnnotations) {
        supportedOptions.addAll(provider.supportedOptions());
        supportedAnnotations.addAll(provider.supportedTypes());
        provider.supportedAnnotationPackages()
                .stream()
                .map(it -> it.endsWith(".*") ? it : it + ".*")
                .forEach(supportedPackages::add);
    }
}
