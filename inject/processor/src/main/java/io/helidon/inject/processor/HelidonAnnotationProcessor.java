package io.helidon.inject.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.processor.GeneratedAnnotationHandler;
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
import io.helidon.inject.tools.ModuleInfoDescriptor;
import io.helidon.inject.tools.ModuleInfoItem;

import static java.lang.System.Logger.Level.TRACE;

/**
 * Annotation processor to handle Helidon injection annotations.
 * <p>
 * There is only a single annotation processor registered, which uses Helidon extension mechanism.
 */
public final class HelidonAnnotationProcessor extends AbstractProcessor {
    public static final String OPTION_INJECT_SCOPE = "inject.scope";
    private static final Pattern SCOPE_PATTERN = Pattern.compile("(\\w+).*classes");

    private static final List<HelidonProcessorExtensionProvider> EXTENSIONS =
            HelidonServiceLoader.create(ServiceLoader.load(HelidonProcessorExtensionProvider.class,
                                                           HelidonAnnotationProcessor.class.getClassLoader()))
                    .asList();
    private static final Set<String> SUPPORTED_APT_OPTIONS;
    private static final TypeName GENERATOR = TypeName.create(HelidonAnnotationProcessor.class);

    static {
        Set<String> supportedOptions = EXTENSIONS.stream()
                .flatMap(it -> it.supportedOptions().stream())
                .collect(Collectors.toSet());
        supportedOptions.add(OPTION_INJECT_SCOPE);
        SUPPORTED_APT_OPTIONS = Set.copyOf(supportedOptions);
    }

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
        return SUPPORTED_APT_OPTIONS;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.ctx = ProcessingContext.create(processingEnv, SUPPORTED_APT_OPTIONS.toArray(new String[0]));

        String scope = guessScope(processingEnv, ctx);

        this.iCtx = new InjectionProcessingContextImpl(ctx, scope);
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

    private String guessScope(ProcessingEnvironment processingEnv, ProcessingContext ctx) {
        String scopeFromOptions = ctx.options().option(OPTION_INJECT_SCOPE, null);
        if (scopeFromOptions != null) {
            return scopeFromOptions;
        }
        try {
            URI resourceUri = processingEnv.getFiler()
                    .getResource(StandardLocation.CLASS_OUTPUT, "does.not.exist", "DefinitelyDoesNotExist")
                    .toUri();

            // should be something like:
            // file:///projects/helidon_4/inject/tests/resources-inject/target/test-classes/does/not/exist/DefinitlyDoesNotExist
            String resourceUriString = resourceUri.toString();
            if (!resourceUriString.endsWith("/does/not/exist/DefinitelyDoesNotExist")) {
                // cannot guess, not ending in expected string, assume production scope
                return "";
            }
            // full URI
            resourceUriString = resourceUriString
                    .substring(0, resourceUriString.length() - "/does/not/exist/DefinitelyDoesNotExist".length());
            // file:///projects/helidon_4/inject/tests/resources-inject/target/test-classes
            int lastSlash = resourceUriString.lastIndexOf('/');
            if (lastSlash < 0) {
                // cannot guess, no path, assume production scope
                return "";
            }
            resourceUriString = resourceUriString.substring(lastSlash + 1);
            // test-classes
            Matcher matcher = SCOPE_PATTERN.matcher(resourceUriString);
            if (matcher.matches()) {
                return matcher.group(1);
            }
            // not matched, either production (just "classes"), or could not match - assume production scope
            return "";
        } catch (IOException e) {
            // we assume production scope
            return "";
        }
    }

    private boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (logger.isLoggable(TRACE)) {
            logger.log(TRACE, "Process annotations: " + annotations + ", processing over: " + roundEnv.processingOver());
        }

        if (roundEnv.processingOver()) {
            extensions.forEach(it -> it.processingOver(roundEnv));

            if (!generatedServiceDescriptors.isEmpty()) {
                Filer filer = ctx.aptEnv().getFiler();
                Optional<ModuleInfoDescriptor> moduleDescriptor = findModule(filer);
                // generate module
                String moduleName = moduleDescriptor.map(ModuleInfoDescriptor::name).orElse(this.module);
                String packageName = topLevelPackage(generatedServiceDescriptors);
                boolean hasModule = moduleName != null && !"unnamed module".equals(moduleName);
                if (!hasModule) {
                    moduleName = "unknown/" + packageName + (iCtx.scope().isProduction() ? "" : "/" + iCtx.scope().name());
                }
                ClassCode moduleComponent = ModuleComponentHandler.createClassModel(iCtx.scope(),
                                                                                    generatedServiceDescriptors,
                                                                                    moduleName,
                                                                                    packageName);

                if (hasModule && moduleDescriptor.isPresent()) {
                    // check if we have `providers ModuleComponent with OurModuleComponent`
                    if (moduleDescriptor.get()
                            .items()
                            .stream()
                            .filter(ModuleInfoItem::provides)
                            .noneMatch(it -> {
                                if (it.withOrTo().contains(moduleComponent.newType().fqName())
                                        || it.withOrTo().contains(moduleComponent.newType().className())) {
                                    return it.target().equals("io.helidon.inject.api.ModuleComponent")
                                            || it.target().equals("ModuleComponent");
                                }
                                return false;
                            })) {
                        throw new IllegalStateException("Please add \"provides io.helidon.inject.api.ModuleComponent"
                                                                + " with " + moduleComponent.newType().fqName() + ";\" "
                                                                + "to your mdoule-info.java");
                    }
                }

                ClassModel classModel = moduleComponent.classModel().build();
                try {

                    TypeName moduleType = moduleComponent.newType();
                    Element[] originatingElements = toElements(moduleComponent.originatingTypes());

                    generatedServiceDescriptors.add(moduleType);
                    JavaFileObject sourceFile = filer
                            .createSourceFile(moduleType.declaredName(),
                                              originatingElements);
                    try (PrintWriter pw = new PrintWriter(sourceFile.openWriter())) {
                        classModel.write(pw, "    ");
                    }

                    if (!hasModule) {
                        // only create meta-inf/services if we are not a JPMS module
                        try {
                            // if the user creates this file on their own, we do not generate the output
                            FileObject resource = filer.getResource(StandardLocation.CLASS_OUTPUT,
                                                                    "",
                                                                    "META-INF/services/io.helidon.inject.api.ModuleComponent");
                            try (InputStream ignored = resource.openInputStream()) {
                            }
                        } catch (IOException ignored) {
                            FileObject resource = filer
                                    .createResource(StandardLocation.CLASS_OUTPUT,
                                                    "",
                                                    "META-INF/services/io.helidon.inject.api.ModuleComponent",
                                                    originatingElements);
                            try (PrintWriter pw = new PrintWriter(resource.openWriter())) {
                                pw.print("# ");
                                pw.println(GeneratedAnnotationHandler.create(GENERATOR,
                                                                             GENERATOR,
                                                                             TypeName.create("MetaInfServicesModuleComponent"),
                                                                             "1",
                                                                             ""));
                                pw.println(moduleType.declaredName());
                            }
                        }
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

        Filer filer = ctx.aptEnv().getFiler();

        // generate all code
        var builders = iCtx.descriptorClassModels();
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

        builders = iCtx.otherClassModels();
        for (var classCode : builders) {
            ClassModel classModel = classCode.classModel().build();
            try {
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

    private Optional<ModuleInfoDescriptor> findModule(Filer filer) {
        // expected is source location
        try {
            FileObject resource = filer.getResource(StandardLocation.SOURCE_PATH, "", "module-info.java");
            return Optional.of(ModuleInfoDescriptor.create(resource.openInputStream()));
        } catch (IOException ignored) {
            // it is not in sources, let's see if it got generated
        }
        // generated
        try {
            FileObject resource = filer.getResource(StandardLocation.SOURCE_OUTPUT, "", "module-info.java");
            return Optional.of(ModuleInfoDescriptor.create(resource.openInputStream()));
        } catch (IOException ignored) {
            // not in generated source either
        }
        // we do not see a module info
        return Optional.empty();
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
