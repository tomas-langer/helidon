/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.integrations.oci.sdk.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.processor.CopyrightHandler;
import io.helidon.common.processor.GeneratedAnnotationHandler;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.ExternalContracts;
import io.helidon.inject.api.ModuleComponent;
import io.helidon.inject.processor.ProcessingEvent;
import io.helidon.inject.processor.spi.InjectionAnnotationProcessorObserver;
import io.helidon.inject.tools.ToolsException;
import io.helidon.inject.tools.TypeNames;

import static io.helidon.inject.processor.GeneralProcessorUtils.isProviderType;
import static io.helidon.inject.runtime.ServiceUtils.DEFAULT_INJECT_WEIGHT;
import static java.util.function.Predicate.not;

/**
 * This processor is an implementation of {@link InjectionAnnotationProcessorObserver}. When on the APT classpath, it will monitor
 * injection processor for all injection points that are
 * using the {@code OCI SDK Services} and translate those injection points into code-generated
 * {@link Activator}s, {@link ModuleComponent}, etc. for those services / components.
 * This process will therefore make the {@code OCI SDK} set of services injectable by your (non-MP-based) Helidon application, and
 * be tailored to exactly what is actually used by your application from the SDK.
 * <p>
 * For example, if your code had this:
 * <pre>
 * {@code
 *   @Inject
 *   com.oracle.bmc.ObjectStorage objStore;
 * }
 * </pre>
 * This would result in code generating the necessary artifacts at compile time that will make {@code ObjectStorage} injectable.
 * <p>
 * All injection points using the same package name as the OCI SDK (e.g., {@code com.oracle.bmc} as shown with ObjectStorage in
 * the case above) will be observed and processed and eventually result in code generation into your
 * {@code target/generated-sources} directory. This is the case for any artifact that is attempted to be injected unless there is
 * found a configuration signaling an exception to avoid the code generation for the activator.
 * <p>
 * The processor will allows exceptions in one of three ways:
 * <ul>
 *     <li>via the code directly here - see the {@link #shouldProcess} method.</li>
 *     <li>via resources on the classpath - the implementation looks for files named {@link #TAG_TYPENAME_EXCEPTIONS} in the same
 *     package name as this class, and will read those resources during initialization. Each line of this file would be a fully
 *     qualified type name to avoid processing that type name.</li>
 *     <li>via {@code -A} directives on the compiler command line. Using the same tag as referenced above. The line can be
 *     comma-delimited, and each token will be treated as a fully qualified type name to signal that the type should be
 *     not be processed.</li>
 * </ul>
 */
public class OciInjectionProcessorObserver implements InjectionAnnotationProcessorObserver {
    static final String OCI_ROOT_PACKAGE_NAME_PREFIX = "com.oracle.bmc.";

    // all generated sources will have this package prefix
    static final String GENERATED_PREFIX = "io.helidon.integrations.generated.";

    // all generated sources will have this class name suffix
    static final String GENERATED_CLIENT_SUFFIX = "__Oci_Client";
    static final String GENERATED_CLIENT_BUILDER_SUFFIX = GENERATED_CLIENT_SUFFIX + "Builder";
    static final String GENERATED_OCI_ROOT_PACKAGE_NAME_PREFIX = GENERATED_PREFIX + OCI_ROOT_PACKAGE_NAME_PREFIX;

    static final String TAG_TEMPLATE_SERVICE_CLIENT_PROVIDER_NAME = "service-client-provider.hbs";
    static final String TAG_TEMPLATE_SERVICE_CLIENT_BUILDER_PROVIDER_NAME = "service-client-builder-provider.hbs";

    // note that these can be used as -A values also
    static final String TAG_TYPENAME_EXCEPTIONS = "codegen-exclusions";
    static final String TAG_NO_DOT_EXCEPTIONS = "builder-name-exceptions";

    static final LazyValue<Set<String>> TYPENAME_EXCEPTIONS = LazyValue
            .create(OciInjectionProcessorObserver::loadTypeNameExceptions);
    static final LazyValue<Set<String>> NO_DOT_EXCEPTIONS = LazyValue
            .create(OciInjectionProcessorObserver::loadNoDotExceptions);

    private static final TypeName PROCESSOR_TYPE = TypeName.create(OciInjectionProcessorObserver.class);

    /**
     * Service loader based constructor.
     *
     * @deprecated this is a Java ServiceLoader implementation and the constructor should not be used directly
     */
    @Deprecated
    public OciInjectionProcessorObserver() {
    }

    static TypeName toGeneratedServiceClientTypeName(TypeName typeName) {
        return TypeName.builder()
                .packageName(GENERATED_PREFIX + typeName.packageName())
                .className(typeName.className() + GENERATED_CLIENT_SUFFIX)
                .build();
    }

    static TypeName toGeneratedServiceClientBuilderTypeName(TypeName typeName) {
        return TypeName.builder()
                .packageName(GENERATED_PREFIX + typeName.packageName())
                .className(typeName.className() + GENERATED_CLIENT_BUILDER_SUFFIX)
                .build();
    }

    static ClassModel toBuilderBody(TypeName ociServiceTypeName,
                                    TypeName generatedOciService,
                                    TypeName generatedOciServiceBuilderTypeName) {
        boolean usesRegion = usesRegion(ociServiceTypeName);

        String maybeDot = maybeDot(ociServiceTypeName);
        String builderSuffix = "Client" + maybeDot + "Builder";

        ClassModel.Builder classModel = ClassModel.builder()
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .copyright(CopyrightHandler.copyright(PROCESSOR_TYPE,
                                                      ociServiceTypeName,
                                                      generatedOciService))
                .addAnnotation(GeneratedAnnotationHandler.create(PROCESSOR_TYPE,
                                                                 ociServiceTypeName,
                                                                 generatedOciService,
                                                                 "1",
                                                                 ""))
                .type(generatedOciServiceBuilderTypeName)
                .addInterface(ipProvider(ociServiceTypeName, builderSuffix))
                .addAnnotation(Annotation.create(TypeNames.JAKARTA_SINGLETON_TYPE))
                .addAnnotation(Annotation.builder()
                                       .typeName(TypeName.create(Weight.class))
                                       .putValue("value", DEFAULT_INJECT_WEIGHT)
                                       .build());

        // fields
        if (usesRegion) {
            TypeName regionProviderType = ipProvider(TypeName.create("com.oracle.bmc.Region"), "");
            classModel.addField(regionProvider -> regionProvider
                    .isFinal(true)
                    .accessModifier(AccessModifier.PRIVATE)
                    .name("regionProvider")
                    .type(regionProviderType));
            // constructor
            classModel.addConstructor(ctor -> ctor
                    .addAnnotation(Annotation.create(TypeNames.JAKARTA_INJECT_TYPE))
                    .addAnnotation(Annotation.create(Deprecated.class))
                    .addParameter(regionProvider -> regionProvider.name("regionProvider")
                            .type(regionProviderType))
                    .addLine("this.regionProvider = regionProvider;"));
        } else {
            // constructor
            classModel.addConstructor(ctor -> ctor
                    .addAnnotation(Annotation.create(TypeNames.JAKARTA_INJECT_TYPE))
                    .addAnnotation(Annotation.create(Deprecated.class)));
        }

        String clientType = "@" + ociServiceTypeName.fqName() + "Client@";

        // method(s)
        classModel.addMethod(first -> first
                .name("first")
                .addAnnotation(Annotation.create(Override.class))
                .returnType(optional(ociServiceTypeName, builderSuffix))
                .addParameter(query -> query.name("query")
                        .type(TypeName.create(ContextualServiceQuery.class)))
                .update(it -> {
                    if (usesRegion) {
                        it.addLine("var builder = " + clientType + ".builder();")
                                .addLine("regionProvider.first(query).ifPresent(builder::region);")
                                .addLine("return Optional.of(builder);");
                    } else {
                        it.addLine("return @java.util.Optional@.of(" + clientType + ".builder());");
                    }
                }));

        return classModel.build();
    }

    static ClassModel toBody(TypeName ociServiceTypeName,
                             TypeName generatedOciService) {
        ClassModel.Builder classModel = ClassModel.builder()
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .copyright(CopyrightHandler.copyright(PROCESSOR_TYPE,
                                                      ociServiceTypeName,
                                                      generatedOciService))
                .addAnnotation(GeneratedAnnotationHandler.create(PROCESSOR_TYPE,
                                                                 ociServiceTypeName,
                                                                 generatedOciService,
                                                                 "1",
                                                                 ""))
                .type(generatedOciService)
                .addInterface(ipProvider(ociServiceTypeName, "Client"))
                .addAnnotation(Annotation.create(TypeNames.JAKARTA_SINGLETON_TYPE))
                .addAnnotation(Annotation.builder()
                                       .typeName(TypeName.create(Weight.class))
                                       .putValue("value", DEFAULT_INJECT_WEIGHT)
                                       .build())
                .addAnnotation(Annotation.builder()
                                       .typeName(TypeName.create(ExternalContracts.class))
                                       .putValue("value", ociServiceTypeName)
                                       .build());

        TypeName authProviderType = ipProvider(TypeName.create("com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider"), "");
        TypeName builderProviderType = ipProvider(ociServiceTypeName, "Client" + maybeDot(ociServiceTypeName) + "Builder");

        // fields
        classModel.addField(authProvider -> authProvider
                .isFinal(true)
                .accessModifier(AccessModifier.PRIVATE)
                .name("authProvider")
                .type(authProviderType));

        classModel.addField(builderProvider -> builderProvider
                .isFinal(true)
                .accessModifier(AccessModifier.PRIVATE)
                .name("builderProvider")
                .type(builderProviderType));

        // constructor
        classModel.addConstructor(ctor -> ctor
                .addAnnotation(Annotation.create(TypeNames.JAKARTA_INJECT_TYPE))
                .addAnnotation(Annotation.create(Deprecated.class))
                .addParameter(authProvider -> authProvider.name("authProvider")
                        .type(authProviderType))
                .addParameter(builderProvider -> builderProvider.name("builderProvider")
                        .type(builderProviderType))
                .addLine("this.authProvider = authProvider;")
                .addLine("this.builderProvider = builderProvider;"));

        // method(s)
        classModel.addMethod(first -> first
                .name("first")
                .addAnnotation(Annotation.create(Override.class))
                .returnType(optional(ociServiceTypeName, "Client"))
                .addParameter(query -> query.name("query")
                        .type(TypeName.create(ContextualServiceQuery.class)))
                .addLine(
                        "return @java.util.Optional@.of(builderProvider.first(query).orElseThrow().build(authProvider.first"
                                + "(query).orElseThrow()));"));

        return classModel.build();
    }

    static String maybeDot(TypeName ociServiceTypeName) {
        return NO_DOT_EXCEPTIONS.get().contains(ociServiceTypeName.name()) ? "" : ".";
    }

    static boolean usesRegion(TypeName ociServiceTypeName) {
        // it turns out that the same exceptions used for dotting the builder also applies to whether it uses region
        return !NO_DOT_EXCEPTIONS.get().contains(ociServiceTypeName.name());
    }

    static String loadTemplate(String name) {
        String path = "io/helidon/integrations/oci/sdk/processor/templates/" + name;
        try {
            InputStream in = OciInjectionProcessorObserver.class.getClassLoader().getResourceAsStream(path);
            if (in == null) {
                throw new IOException("Could not find template " + path + " on classpath.");
            }
            try (in) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new ToolsException(e.getMessage(), e);
        }
    }

    static Set<String> loadTypeNameExceptions() {
        return loadSet(TAG_TYPENAME_EXCEPTIONS);
    }

    static Set<String> loadNoDotExceptions() {
        return loadSet(TAG_NO_DOT_EXCEPTIONS);
    }

    static void layerInManualOptions(ProcessingEnvironment processingEnv) {
        Map<String, String> opts = processingEnv.getOptions();
        TYPENAME_EXCEPTIONS.get().addAll(splitToSet(opts.get(TAG_TYPENAME_EXCEPTIONS)));
        NO_DOT_EXCEPTIONS.get().addAll(splitToSet(opts.get(TAG_NO_DOT_EXCEPTIONS)));
    }

    static Set<String> splitToSet(String val) {
        if (val == null) {
            return Set.of();
        }
        List<String> list = Arrays.stream(val.split(","))
                .map(String::trim)
                .filter(not(String::isEmpty))
                .filter(not(s -> s.startsWith("#")))
                .toList();
        return new LinkedHashSet<>(list);
    }

    static Set<String> loadSet(String name) {
        // note that we need to keep this mutable for later when we process the env options passed manually in
        Set<String> result = new LinkedHashSet<>();

        name = OciInjectionProcessorObserver.class.getPackageName().replace(".", "/") + "/" + name + ".txt";
        try {
            Enumeration<URL> resources = OciInjectionProcessorObserver.class.getClassLoader().getResources(name);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (
                        InputStream in = url.openStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                ) {
                    reader.lines()
                            .map(String::trim)
                            .filter(not(String::isEmpty))
                            .filter(not(s -> s.startsWith("#")))
                            .forEach(result::add);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return result;
    }

    static boolean shouldProcess(TypedElementInfo element,
                                 ProcessingEnvironment processingEnv) {
        if (!element.hasAnnotation(TypeNames.JAKARTA_INJECT_TYPE)) {
            return false;
        }

        return switch (element.elementTypeKind()) {
            case FIELD -> shouldProcess(element.typeName(), processingEnv);
            case METHOD, CONSTRUCTOR -> element.parameterArguments().stream()
                    .anyMatch(it -> shouldProcess(it.typeName(), processingEnv));
            default -> false;
        };
    }

    static boolean shouldProcess(TypeName typeName,
                                 ProcessingEnvironment processingEnv) {
        if (!typeName.typeArguments().isEmpty()
                && isProviderType(typeName) || typeName.isOptional()) {
            typeName = typeName.typeArguments().get(0);
        }

        String name = typeName.resolvedName();
        if (!name.startsWith(OCI_ROOT_PACKAGE_NAME_PREFIX)
                || name.endsWith(".Builder")
                || name.endsWith("Client")
                || name.endsWith("ClientBuilder")
                || TYPENAME_EXCEPTIONS.get().contains(name)) {
            return false;
        }

        if (processingEnv != null) {
            // check to see if we already generated it before, and if so we can skip creating it again
            String generatedTypeName = toGeneratedServiceClientTypeName(typeName).name();
            TypeElement typeElement = processingEnv.getElementUtils()
                    .getTypeElement(generatedTypeName);
            return (typeElement == null);
        }

        return true;
    }

    @Override
    public void onProcessingEvent(ProcessingEvent event) {
        ProcessingEnvironment processingEnv = event.processingEnvironment().orElseThrow();
        layerInManualOptions(processingEnv);
        event.elementsOfInterest().stream()
                .filter(it -> shouldProcess(it, processingEnv))
                .forEach(it -> process(it, processingEnv));
    }

    private static TypeName optional(TypeName typeName, String suffix) {
        return TypeName.builder(io.helidon.common.types.TypeNames.OPTIONAL)
                .addTypeArgument(TypeName.create(typeName.fqName() + suffix))
                .build();
    }

    private static TypeName ipProvider(TypeName provided, String suffix) {
        return TypeName.builder(TypeNames.INJECTION_POINT_PROVIDER_TYPE)
                .addTypeArgument(TypeName.create(provided.fqName() + suffix))
                .build();
    }

    private void process(TypedElementInfo element,
                         ProcessingEnvironment processingEnv) {
        switch (element.elementTypeKind()) {
        case FIELD -> process(element.typeName(), processingEnv);
        case METHOD, CONSTRUCTOR -> element.parameterArguments().stream()
                .filter(it -> shouldProcess(it.typeName(), processingEnv))
                .forEach(it -> process(it.typeName(), processingEnv));
        default -> {
        }
        }
    }

    private void process(TypeName ociServiceTypeName,
                         ProcessingEnvironment processingEnv) {
        if (isProviderType(ociServiceTypeName)
                || ociServiceTypeName.isOptional()) {
            ociServiceTypeName = ociServiceTypeName.typeArguments().get(0);
        }
        assert (!ociServiceTypeName.generic()) : ociServiceTypeName.name();
        assert (ociServiceTypeName.name().startsWith(OCI_ROOT_PACKAGE_NAME_PREFIX)) : ociServiceTypeName.name();

        TypeName generatedOciServiceClientTypeName = toGeneratedServiceClientTypeName(ociServiceTypeName);
        ClassModel serviceClient = toBody(ociServiceTypeName,
                                          generatedOciServiceClientTypeName);
        codegen(generatedOciServiceClientTypeName, serviceClient, processingEnv);

        TypeName generatedOciServiceClientBuilderTypeName = toGeneratedServiceClientBuilderTypeName(ociServiceTypeName);
        ClassModel serviceClientBuilder = toBuilderBody(ociServiceTypeName,
                                                        generatedOciServiceClientTypeName,
                                                        generatedOciServiceClientBuilderTypeName);
        codegen(generatedOciServiceClientBuilderTypeName, serviceClientBuilder, processingEnv);
    }

    private void codegen(TypeName typeName,
                         ClassModel classModel,
                         ProcessingEnvironment processingEnv) {
        Filer filer = processingEnv.getFiler();
        try {
            JavaFileObject javaSrc = filer.createSourceFile(typeName.name());
            try (Writer os = javaSrc.openWriter()) {
                classModel.write(os, "    ");
            }
        } catch (FilerException x) {
            processingEnv.getMessager().printWarning("Failed to write java file: " + x);
        } catch (Exception x) {
            System.getLogger(getClass().getName()).log(System.Logger.Level.ERROR, "Failed to write java file: " + x, x);
            processingEnv.getMessager().printError("Failed to write java file: " + x);
        }
    }

}
