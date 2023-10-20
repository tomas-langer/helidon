package io.helidon.inject.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.processor.TypeFactory;
import io.helidon.common.processor.TypeInfoFactory;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.processor.spi.InjectProcessorExtension;
import io.helidon.inject.processor.spi.InjectProcessorExtensionProvider;

import static java.lang.System.Logger.Level.TRACE;

/**
 * Annotation processor to handle Helidon injection annotations.
 * <p>
 * There is only a single annotation processor registered, which uses Helidon extension mechanism.
 */
public final class InjectAnnotationProcessor extends AbstractProcessor {
    private static final List<InjectProcessorExtensionProvider> EXTENSIONS =
            HelidonServiceLoader.create(ServiceLoader.load(InjectProcessorExtensionProvider.class,
                                                           InjectAnnotationProcessor.class.getClassLoader()))
                    .asList();

    private final System.Logger logger = System.getLogger(getClass().getName());
    private final Map<TypeName, List<InjectProcessorExtension>> typeToExtensions = new HashMap<>();
    private final Map<InjectProcessorExtension, Predicate<TypeName>> extensionPredicates = new IdentityHashMap<>();
    private ProcessingContext ctx;
    private List<InjectProcessorExtension> extensions;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> result = new HashSet<>();

        ctx.mapperSupportedAnnotations()
                .stream()
                .map(TypeName::fqName)
                .forEach(result::add);

        result.addAll(ctx.mapperSupportedAnnotationPackages());

        for (InjectProcessorExtensionProvider extension : EXTENSIONS) {
            extension.supportedTypes()
                    .stream()
                    .map(TypeName::fqName)
                    .forEach(result::add);
            ctx.mapperSupportedAnnotationPackages()
                    .stream()
                    .map(it -> it.endsWith(".*") ? it : it + ".*")
                    .forEach(result::add);
        }

        if (logger.isLoggable(TRACE)) {
            logger.log(TRACE, "Supported annotation types: " + result);
        }

        result.add("jakarta.*");

        return result;
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> result = new HashSet<>();

        for (InjectProcessorExtensionProvider extension : EXTENSIONS) {
            result.addAll(extension.supportedOptions());
        }
        result.addAll(ctx.mapperSupportedOptions());

        if (logger.isLoggable(TRACE)) {
            logger.log(TRACE, "Supported options: " + result);
        }

        return result;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.ctx = ProcessingContext.create(processingEnv);
        this.extensions = EXTENSIONS.stream()
                .map(it -> {

                    InjectProcessorExtension extension = it.create(processingEnv, ctx.options());

                    for (TypeName typeName : it.supportedTypes()) {
                        typeToExtensions.computeIfAbsent(typeName, key -> new ArrayList<>())
                                .add(extension);
                    }
                    Collection<String> packages = it.supportedAnnotationPackages();
                    if (!packages.isEmpty()) {
                        extensionPredicates.put(extension, discoveryPredicate(packages));
                    }

                    return extension;
                })
                .toList();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (logger.isLoggable(TRACE)) {
            logger.log(TRACE, "Process annotations: " + annotations + ", processing over: " + roundEnv.processingOver());
        }

        if (roundEnv.processingOver()) {
            extensions.forEach(it -> it.processingOver(roundEnv));
            return true;
        }

        if (annotations.isEmpty()) {
            // no annotations, ask each processor to handle this case
            extensions.forEach(it -> it.process(roundEnv));
            return true;
        }

        // we must discover all types that should be handled, create TypeInfo and only then check if these should be processed
        // as we may replace annotations, elements, and whole types.

        // first collect all types
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
                default -> logger.log(TRACE, "Ignoring annotated element, not supported: " + element + ", kind: " + kind);
                }
            }
        }

        // type info list will contain all mapped annotations, so this is the state we can do annotation processing on
        List<TypeInfoAndAnnotations> annotatedTypes = new ArrayList<>();
        types.forEach((type, element) -> {
            Optional<TypeInfo> typeInfo = TypeInfoFactory.create(ctx, element);
            if (typeInfo.isPresent()) {
                TypeInfo theTypeInfo = typeInfo.get();
                annotatedTypes.add(new TypeInfoAndAnnotations(theTypeInfo, annotations(theTypeInfo)));
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                                                         "Could not create TypeInfo for annotated type.",
                                                         element);
            }
        });

        return false;
//        adsfadfasd
//                tady
//
//        boolean response = true;
//        // for each extension, create a RoundContext with just the stuff it wants
//        for (InjectProcessorExtension extension : extensions) {
//            Set<TypeName> extensionAnnotations = new HashSet<>();
//            Map<TypeName, TypeElement> extensionElements = new HashMap<>();
//
//            for (TypeName roundAnnotation : roundAnnotations) {
//                boolean added = false;
//                List<InjectProcessorExtension> validExts = this.typeToExtensions.get(roundAnnotation);
//                if (validExts != null) {
//                    for (InjectProcessorExtension validExt : validExts) {
//                        if (validExt == extension) {
//                            extensionAnnotations.add(roundAnnotation);
//                            extensionElements.put(roundAnnotation, annotationElements.get(roundAnnotation));
//                            added = true;
//                        }
//                    }
//                }
//                if (!added) {
//                    Predicate<TypeName> predicate = this.extensionPredicates.get(extension);
//                    if (predicate != null && predicate.test(roundAnnotation)) {
//                        extensionAnnotations.add(roundAnnotation);
//                        extensionElements.put(roundAnnotation, annotationElements.get(roundAnnotation));
//                    }
//                }
//            }
//
//            RoundContext rCtx = RoundContext.create(roundEnv, Set.copyOf(extensionAnnotations), extensionElements);
//            boolean extResponse = extension.process(rCtx);
//            if (!extResponse) {
//                response = false;
//            }
//        }
//        return response;
//
//        Map<TypeName, TypeElement> annotationElements = new HashMap<>();
//        // we do have annotations, call each extension with the desired annotations
//        Set<TypeName> roundAnnotations = annotations.stream()
//                .map(it -> {
//                    TypeName typeName = TypeFactory.createTypeName(it)
//                            .orElseThrow(() -> new IllegalArgumentException("Received annotation that cannot be "
//                                                                                    + "mapped to a type: " + it));
//
//                    annotationElements.put(typeName, it);
//                    return typeName;
//                })
//                .collect(Collectors.toSet());

    }

    private Set<TypeName> annotations(TypeInfo theTypeInfo) {
        Set<TypeName> result = new HashSet<>();

        // on type
        theTypeInfo.annotations()
                .stream()
                .map(Annotation::typeName)
                .forEach(result::add);

        // on fields, methods etc.
        theTypeInfo.elementInfo()
                .stream()
                .map(TypedElementInfo::annotations)
                .flatMap(List::stream)
                .map(Annotation::typeName)
                .forEach(result::add);

        // on parameters
        theTypeInfo.elementInfo()
                .stream()
                .map(TypedElementInfo::parameterArguments)
                .flatMap(List::stream)
                .map(TypedElementInfo::annotations)
                .flatMap(List::stream)
                .map(Annotation::typeName)
                .forEach(result::add);

        return result;
    }

    private static Predicate<TypeName> discoveryPredicate(Collection<String> packages) {
        List<String> prefixes = packages.stream()
                .map(it -> it.endsWith(".*") ? it.substring(0, it.length() - 2) : it)
                .toList();
        return typeName -> {
            String packageName = typeName.packageName();
            for (String prefix : prefixes) {
                if (packageName.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        };
    }

    private void addType(Map<TypeName, TypeElement> types,
                         Element typeElement,
                         Element processedElement,
                         TypeElement annotation) {
        Optional<TypeName> typeName = TypeFactory.createTypeName(typeElement);
        if (typeName.isPresent()) {
            types.putIfAbsent(typeName.get(), (TypeElement) typeElement);
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                                                     "Could not create TypeName for annotated type."
                                                             + " Annotation: " + annotation,
                                                     processedElement);
        }
    }

    private record TypeInfoAndAnnotations(TypeInfo typeInfo, Set<TypeName> annotations) {

    }

    private record TypeAndElement(TypeName typeName, Element element) {
    }
}
