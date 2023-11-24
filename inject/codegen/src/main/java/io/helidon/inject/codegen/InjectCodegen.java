package io.helidon.inject.codegen;

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
import java.util.stream.Collectors;

import io.helidon.codegen.ClassCode;
import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenException;
import io.helidon.codegen.CodegenFiler;
import io.helidon.codegen.ModuleInfo;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.codegen.spi.InjectCodegenExtension;
import io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider;

/**
 * Handles processing of all extensions, creates context and writes types.
 */
public class InjectCodegen {
    private static final List<InjectCodegenExtensionProvider> EXTENSIONS =
            HelidonServiceLoader.create(ServiceLoader.load(InjectCodegenExtensionProvider.class,
                                                           InjectCodegen.class.getClassLoader()))
                    .asList();
    private static final Set<String> SUPPORTED_APT_OPTIONS;

    static {
        Set<String> supportedOptions = EXTENSIONS.stream()
                .flatMap(it -> it.supportedOptions().stream())
                .collect(Collectors.toSet());
        supportedOptions.add(CodegenContext.OPTION_INJECT_SCOPE);
        SUPPORTED_APT_OPTIONS = Set.copyOf(supportedOptions);
    }

    private final Map<TypeName, List<InjectCodegenExtension>> typeToExtensions = new HashMap<>();
    private final Map<InjectCodegenExtension, Predicate<TypeName>> extensionPredicates = new IdentityHashMap<>();
    private final Set<TypeName> generatedServiceDescriptors = new HashSet<>();
    private final TypeName generator;
    private final InjectionCodegenContext ctx;
    private final List<InjectCodegenExtension> extensions;
    private final Set<TypeName> supportedAnnotations;
    private final Set<String> supportedPackagePrefixes;
    private final String module;

    private InjectCodegen(CodegenContext ctx, TypeName generator) {
        this.ctx = InjectionCodegenContext.create(ctx);
        this.generator = generator;
        this.module = ctx.options().option(InjectOptions.MODULE_NAME).orElse(null);

        this.extensions = EXTENSIONS.stream()
                .map(it -> {
                    InjectCodegenExtension extension = it.create(this.ctx);

                    for (TypeName typeName : it.supportedAnnotations()) {
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

        // handle supported annotations and package prefixes
        Set<String> packagePrefixes = new HashSet<>();
        Set<TypeName> annotations = new HashSet<>(ctx.mapperSupportedAnnotations());

        for (InjectCodegenExtensionProvider extension : EXTENSIONS) {
            annotations.addAll(extension.supportedAnnotations());

            ctx.mapperSupportedAnnotationPackages()
                    .stream()
                    .map(InjectCodegen::toPackagePrefix)
                    .forEach(packagePrefixes::add);
        }
        ctx.mapperSupportedAnnotationPackages()
                .stream()
                .map(InjectCodegen::toPackagePrefix)
                .forEach(packagePrefixes::add);
        packagePrefixes.add("jakarta.");

        this.supportedAnnotations = Set.copyOf(annotations);
        this.supportedPackagePrefixes = Set.copyOf(packagePrefixes);
    }

    public static InjectCodegen create(CodegenContext ctx, TypeName generator) {
        return new InjectCodegen(ctx, generator);
    }

    public static Set<String> supportedOptions() {
        return SUPPORTED_APT_OPTIONS;
    }

    public void process(List<TypeInfo> allTypes) {
        if (allTypes.isEmpty()) {
            extensions.forEach(it -> it.process(createRoundContext(List.of(), it)));
            return;
        }

        // type info list will contain all mapped annotations, so this is the state we can do annotation processing on
        List<TypeInfoAndAnnotations> annotatedTypes = annotatedTypes(allTypes);

        // and now for each extension, we discover types that contain annotations supported by that extension
        // and create a new round context for each extension

        // for each extension, create a RoundContext with just the stuff it wants
        for (InjectCodegenExtension extension : extensions) {
            extension.process(createRoundContext(annotatedTypes, extension));
        }

        writeNewTypes();
    }

    public void processingOver() {
        // do processing over in each extension
        extensions.forEach(InjectCodegenExtension::processingOver);

        // if there was any type generated, write it out (will not trigger next round)
        writeNewTypes();

        if (!generatedServiceDescriptors.isEmpty()) {
            generateModuleComponent();
        }
    }

    public Set<TypeName> supportedAnnotations() {
        return supportedAnnotations;
    }

    public Set<String> supportedAnnotationPackages() {
        return supportedPackagePrefixes;
    }

    private static String toPackagePrefix(String configured) {
        if (configured.endsWith(".*")) {
            return configured.substring(0, configured.length() - 1);
        }
        if (configured.endsWith(".")) {
            return configured;
        }
        return configured + ".";
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

    private void generateModuleComponent() {
        // and write the module component
        Optional<ModuleInfo> currentModule = ctx.module();

        // generate module
        String moduleName = this.module == null ? currentModule.map(ModuleInfo::name).orElse(null) : module;
        String packageName = topLevelPackage(generatedServiceDescriptors);
        boolean hasModule = moduleName != null && !"unnamed module".equals(moduleName);
        if (!hasModule) {
            moduleName = "unknown/" + packageName + (ctx.scope().isProduction() ? "" : "/" + ctx.scope().name());
        }
        ClassCode moduleComponent = ModuleComponentHandler.createClassModel(ctx.scope(),
                                                                            generatedServiceDescriptors,
                                                                            moduleName,
                                                                            packageName);
        // first generate the module component, then validate the module-info

        CodegenFiler filer = ctx.filer();

        ClassModel classModel = moduleComponent.classModel().build();
        filer.writeSourceFile(classModel, moduleComponent.originatingElements());

        if (!hasModule) {
            // only create meta-inf/services if we are not a JPMS module

            try {
                filer.services(generator,
                               InjectCodegenTypes.HELIDON_MODULE_COMPONENT,
                               List.of(classModel.typeName()),
                               moduleComponent.originatingElements());
            } catch (Exception e) {
                // ignore this exception, as the resource probably exists and was done by the user
            }
        }

        if (hasModule && currentModule.isPresent()) {
            // check if we have `provider ModuleComponent with OurModuleComponent`
            ModuleInfo moduleInfo = currentModule.get();
            List<TypeName> typeNames = moduleInfo.provides()
                    .get(InjectCodegenTypes.HELIDON_MODULE_COMPONENT);
            boolean found = false;
            if (typeNames != null) {
                TypeName moduleComponentType = moduleComponent.newType();
                found = typeNames.stream()
                        .anyMatch(moduleComponentType::equals);
            }

            if (!found) {
                throw new CodegenException("Please add \"provides " + InjectCodegenTypes.HELIDON_MODULE_COMPONENT.fqName()
                                                   + "\" with " + moduleComponent.newType().fqName() + ";\" "
                                                   + "to your module-info.java");
            }
        }
    }

    private void writeNewTypes() {
        // after each round, write all generated types
        CodegenFiler filer = ctx.filer();

        // generate all code
        var builders = ctx.descriptors();
        for (var classCode : builders) {
            ClassModel classModel = classCode.classModel().build();
            generatedServiceDescriptors.add(classCode.newType());
            filer.writeSourceFile(classModel, classCode.originatingElements());
        }
        builders.clear();

        builders = ctx.types();
        for (var classCode : builders) {
            ClassModel classModel = classCode.classModel().build();
            filer.writeSourceFile(classModel, classCode.originatingElements());
        }
        builders.clear();
    }

    private List<TypeInfoAndAnnotations> annotatedTypes(List<TypeInfo> allTypes) {
        List<TypeInfoAndAnnotations> result = new ArrayList<>();

        for (TypeInfo typeInfo : allTypes) {
            result.add(new TypeInfoAndAnnotations(typeInfo, annotations(typeInfo)));
        }
        return result;
    }

    private RoundContext createRoundContext(List<TypeInfoAndAnnotations> annotatedTypes, InjectCodegenExtension extension) {
        Set<TypeName> extAnnots = new HashSet<>();
        Map<TypeName, List<TypeInfo>> extAnnotToType = new HashMap<>();
        Map<TypeName, TypeInfo> extTypes = new HashMap<>();

        for (TypeInfoAndAnnotations annotatedType : annotatedTypes) {
            for (TypeName typeName : annotatedType.annotations()) {
                boolean added = false;
                List<InjectCodegenExtension> validExts = this.typeToExtensions.get(typeName);
                if (validExts != null) {
                    for (InjectCodegenExtension validExt : validExts) {
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

        return new RoundContextImpl(
                Set.copyOf(extAnnots),
                Map.copyOf(extAnnotToType),
                List.copyOf(extTypes.values()));
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

    private record TypeInfoAndAnnotations(TypeInfo typeInfo, Set<TypeName> annotations) {
    }
}
