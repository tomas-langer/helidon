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
import io.helidon.inject.processor.spi.InjectProcessorExtensionProvider;

/**
 * Annotation processor to handle Helidon injection annotations.
 */
public class InjectAnnotationProcessor extends AbstractProcessor {
    private static final List<InjectProcessorExtensionProvider> EXTENSIONS =
            HelidonServiceLoader.create(ServiceLoader.load(InjectProcessorExtensionProvider.class))
                    .asList();
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> result = new HashSet<>();



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
