package io.helidon.inject.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import io.helidon.codegen.CodegenEvent;
import io.helidon.codegen.apt.AptContext;
import io.helidon.codegen.apt.AptTypeFactory;
import io.helidon.codegen.apt.AptTypeInfoFactory;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.inject.codegen.InjectCodegen;

import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;

/**
 * Annotation processor to handle Helidon injection annotations.
 * <p>
 * There is only a single annotation processor registered, which uses Helidon extension mechanism.
 *
 * @deprecated this class is not part of Helidon public API, it is only public because we need that for
 *         {@link java.util.ServiceLoader}
 */
@Deprecated
public final class HelidonAnnotationProcessor extends AbstractProcessor {
    private static final TypeName GENERATOR = TypeName.create(HelidonAnnotationProcessor.class);

    private InjectCodegen codegen;
    private AptContext ctx;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Stream.concat(codegen.supportedAnnotations()
                                     .stream()
                                     .map(TypeName::fqName),
                             codegen.supportedAnnotationPackages()
                                     .stream()
                                     .map(it -> it + "*"))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getSupportedOptions() {
        return InjectCodegen.supportedOptions();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.ctx = AptContext.create(processingEnv, InjectCodegen.supportedOptions().toArray(new String[0]));
        this.codegen = InjectCodegen.create(ctx, GENERATOR);

    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Thread thread = Thread.currentThread();
        ClassLoader previousClassloader = thread.getContextClassLoader();
        thread.setContextClassLoader(HelidonAnnotationProcessor.class.getClassLoader());

        // we want everything to execute in the classloader of this type, so service loaders
        // use the classpath of the annotation processor, and not some "random" classloader, such as a maven one
        try {
            doProcess(annotations, roundEnv);
            return true;
        } finally {
            thread.setContextClassLoader(previousClassloader);
        }
    }

    private void doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ctx.logger().log(TRACE, "Process annotations: " + annotations + ", processing over: " + roundEnv.processingOver());

        if (roundEnv.processingOver()) {
            codegen.processingOver();
            return;
        }

        if (annotations.isEmpty()) {
            // no annotations, no types, still call the codegen, maybe it has something to do
            codegen.process(List.of());
            return;
        }

        List<TypeInfo> allTypes = discoverTypes(annotations, roundEnv);
        codegen.process(allTypes);
    }

    private List<TypeInfo> discoverTypes(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // we must discover all types that should be handled, create TypeInfo and only then check if these should be processed
        // as we may replace annotations, elements, and whole types.

        // first collect all types (group by type name, so we do not have duplicity)
        Map<TypeName, TypeElement> types = new HashMap<>();

        for (TypeElement annotation : annotations) {
            Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elementsAnnotatedWith) {
                ElementKind kind = element.getKind();
                switch (kind) {
                case ENUM, INTERFACE, CLASS, ANNOTATION_TYPE, RECORD -> addType(types, element, element, annotation);
                case ENUM_CONSTANT, CONSTRUCTOR, METHOD, FIELD, STATIC_INIT, INSTANCE_INIT, RECORD_COMPONENT ->
                        addType(types, element.getEnclosingElement(), element, annotation);
                case PARAMETER -> addType(types, element.getEnclosingElement().getEnclosingElement(), element, annotation);
                default -> ctx.logger().log(TRACE, "Ignoring annotated element, not supported: " + element + ", kind: " + kind);
                }
            }
        }

        return types.values()
                .stream()
                .flatMap(element -> {
                    Optional<TypeInfo> typeInfo = AptTypeInfoFactory.create(ctx, element);

                    if (typeInfo.isEmpty()) {
                        ctx.logger().log(CodegenEvent.builder()
                                                 .level(WARNING)
                                                 .message("Could not create TypeInfo for annotated type.")
                                                 .addObject(element)
                                                 .build());
                    }
                    return typeInfo.stream();
                })
                .toList();
    }

    private void addType(Map<TypeName, TypeElement> types,
                         Element typeElement,
                         Element processedElement,
                         TypeElement annotation) {
        Optional<TypeName> typeName = AptTypeFactory.createTypeName(typeElement);
        if (typeName.isPresent()) {
            types.putIfAbsent(typeName.get(), (TypeElement) typeElement);
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                                                     "Could not create TypeName for annotated type."
                                                             + " Annotation: " + annotation,
                                                     processedElement);
        }
    }
}
