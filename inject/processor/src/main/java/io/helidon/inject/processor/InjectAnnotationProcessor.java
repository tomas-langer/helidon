package io.helidon.inject.processor;

import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.types.TypeName;
import io.helidon.inject.processor.spi.InjectProcessorExtensionProvider;

/**
 * Annotation processor to handle Helidon injection annotations.
 */
public class InjectAnnotationProcessor extends AbstractProcessor {
    private static final List<InjectProcessorExtensionProvider> EXTENSIONS =
            HelidonServiceLoader.create(ServiceLoader.load(InjectProcessorExtensionProvider.class,
                                                           InjectAnnotationProcessor.class.getClassLoader()))
                    .asList();
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> result = new HashSet<>();

        for (InjectProcessorExtensionProvider extension : EXTENSIONS) {
            extension.supportedTypes()
                    .stream()
                    .map(TypeName::fqName)
                    .forEach(result::add);
        }

        return result;
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> result = new HashSet<>();

        for (InjectProcessorExtensionProvider extension : EXTENSIONS) {
            result.addAll(extension.supportedOptions());
        }

        return result;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
    }
}
