package io.helidon.inject.processor;

import java.io.IOException;
import java.io.PrintWriter;
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
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.processor.TypeFactory;
import io.helidon.common.processor.TypeInfoFactory;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.processor.spi.HelidonProcessorExtension;
import io.helidon.inject.processor.spi.HelidonProcessorExtensionProvider;

import static java.lang.System.Logger.Level.TRACE;

/**
 * Annotation processor to handle Helidon injection annotations.
 * <p>
 * There is only a single annotation processor registered, which uses Helidon extension mechanism.
 */
public final class HelidonAnnotationProcessor extends AbstractProcessor {
    private static final List<HelidonProcessorExtensionProvider> EXTENSIONS =
            HelidonServiceLoader.create(ServiceLoader.load(HelidonProcessorExtensionProvider.class,
                                                           HelidonAnnotationProcessor.class.getClassLoader()))
                    .asList();

    private final System.Logger logger = System.getLogger(getClass().getName());
    private final Map<TypeName, List<HelidonProcessorExtension>> typeToExtensions = new HashMap<>();
    private final Map<HelidonProcessorExtension, Predicate<TypeName>> extensionPredicates = new IdentityHashMap<>();
    private final Set<TypeName> generatedServiceDescriptors = new HashSet<>();

    private String module;
    private ProcessingContext ctx;
    private List<HelidonProcessorExtension> extensions;
    private InjectionProcessingContextImpl iCtx;

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

        for (HelidonProcessorExtensionProvider extension : EXTENSIONS) {
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

        for (HelidonProcessorExtensionProvider extension : EXTENSIONS) {
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
        this.iCtx = new InjectionProcessingContextImpl(ctx);
        this.extensions = EXTENSIONS.stream()
                .map(it -> {

                    HelidonProcessorExtension extension = it.create(iCtx);

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
        Thread thread = Thread.currentThread();
        ClassLoader previousClassloader = thread.getContextClassLoader();
        thread.setContextClassLoader(HelidonAnnotationProcessor.class.getClassLoader());

        // we want everything to execute in the classloader of this type, so service loaders
        // use the classpath of the annotation processor, and not some "random" classloader, such as a maven one
        try {
            return doProcess(annotations, roundEnv);
        } finally {
            thread.setContextClassLoader(previousClassloader);
        }
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

    private boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (logger.isLoggable(TRACE)) {
            logger.log(TRACE, "Process annotations: " + annotations + ", processing over: " + roundEnv.processingOver());
        }

        if (roundEnv.processingOver()) {
            extensions.forEach(it -> it.processingOver(roundEnv));

            if (!generatedServiceDescriptors.isEmpty()) {
                // generate module
                String moduleName = this.module;
                String packageName = topLevelPackage(generatedServiceDescriptors);
                if (moduleName == null) {
                    moduleName = "unknown/" + packageName;
                }
                ClassCode moduleComponent = ModuleComponentHandler.createClassModel(generatedServiceDescriptors,
                                                                                    moduleName,
                                                                                    packageName);

                ClassModel classModel = moduleComponent.classModel().build();
                try {
                    generatedServiceDescriptors.add(moduleComponent.newType());
                    JavaFileObject sourceFile = ctx.aptEnv()
                            .getFiler()
                            .createSourceFile(moduleComponent.newType().declaredName(),
                                              toElements(moduleComponent.originatingTypes()));
                    try (PrintWriter pw = new PrintWriter(sourceFile.openWriter())) {
                        classModel.write(pw, "    ");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
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

                if (this.module == null) {
                    this.module = theTypeInfo.module().orElse(null);
                }
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                                                         "Could not create TypeInfo for annotated type.",
                                                         element);
            }
        });

        // and now for each extension, we discover types that contain annotations supported by that extension
        // and create a new round context for each extension

        boolean response = true;

        // for each extension, create a RoundContext with just the stuff it wants
        for (HelidonProcessorExtension extension : extensions) {
            RoundContext rCtx = createRoundContext(roundEnv, annotatedTypes, extension);

            boolean extResponse = extension.process(rCtx);

            if (!extResponse) {
                response = false;
            }
        }

        // generate all code
        var builders = iCtx.classModels();

        Filer filer = ctx.aptEnv().getFiler();

        for (var classCode : builders) {
            ClassModel classModel = classCode.classModel().build();
            try {
                generatedServiceDescriptors.add(classCode.newType());
                JavaFileObject sourceFile = filer.createSourceFile(classCode.newType().declaredName(),
                                                                   toElements(classCode.originatingTypes()));
                try (PrintWriter pw = new PrintWriter(sourceFile.openWriter())) {
                    classModel.write(pw, "    ");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        builders.clear();

        return response;
    }

    private String topLevelPackage(Set<TypeName> typeNames) {
        String thePackage = typeNames.iterator().next().packageName();

        for (TypeName typeName : typeNames) {
            String nextPackage = typeName.packageName();
            if (nextPackage.length() < thePackage.length()) {
                thePackage = nextPackage;
            }
        }

        return thePackage;
    }

    private Element[] toElements(TypeName[] typeNames) {
        if (typeNames.length == 0) {
            return new Element[0];
        }
        List<Element> elements = new ArrayList<>();

        for (TypeName type : typeNames) {
            TypeElement typeElement = ctx.aptEnv().getElementUtils().getTypeElement(type.declaredName());
            if (typeElement != null) {
                elements.add(typeElement);
            }
        }

        return elements.toArray(new Element[0]);
    }

    private RoundContext createRoundContext(RoundEnvironment roundEnv,
                                            List<TypeInfoAndAnnotations> annotatedTypes,
                                            HelidonProcessorExtension extension) {
        Set<TypeName> extAnnots = new HashSet<>();
        Map<TypeName, List<TypeInfo>> extAnnotToType = new HashMap<>();
        Map<TypeName, TypeInfo> extTypes = new HashMap<>();

        for (TypeInfoAndAnnotations annotatedType : annotatedTypes) {
            for (TypeName typeName : annotatedType.annotations()) {
                boolean added = false;
                List<HelidonProcessorExtension> validExts = this.typeToExtensions.get(typeName);
                if (validExts != null) {
                    for (HelidonProcessorExtension validExt : validExts) {
                        if (validExt == extension) {
                            extAnnots.add(typeName);
                            extAnnotToType.computeIfAbsent(typeName, key -> new ArrayList<>())
                                    .add(annotatedType.typeInfo());
                            extTypes.put(annotatedType.typeInfo().typeName(), annotatedType.typeInfo);
                            added = true;
                        }
                    }
                }
                if (!added) {
                    Predicate<TypeName> predicate = this.extensionPredicates.get(extension);
                    if (predicate != null && predicate.test(typeName)) {
                        extAnnots.add(typeName);
                        extAnnotToType.computeIfAbsent(typeName, key -> new ArrayList<>())
                                .add(annotatedType.typeInfo());
                        extTypes.put(annotatedType.typeInfo().typeName(), annotatedType.typeInfo);
                    }
                }
            }
        }

        return RoundContext.create(roundEnv, Set.copyOf(extAnnots), Map.copyOf(extAnnotToType), List.copyOf(extTypes.values()));
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
}
