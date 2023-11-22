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

package io.helidon.inject.tools;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.common.Weight;
import io.helidon.common.processor.CopyrightHandler;
import io.helidon.common.processor.GeneratedAnnotationHandler;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.processor.classmodel.Javadoc;
import io.helidon.common.processor.classmodel.Method;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.inject.api.Application;
import io.helidon.inject.api.InjectTypes;
import io.helidon.inject.api.InjectionPointProvider;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceInjectionPlanBinder;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.Services;
import io.helidon.inject.spi.InjectionResolver;
import io.helidon.inject.tools.spi.ApplicationCreator;

import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import static io.helidon.inject.runtime.ServiceUtils.DEFAULT_INJECT_WEIGHT;
import static io.helidon.inject.runtime.ServiceUtils.isQualifiedInjectionTarget;

/**
 * The default implementation for {@link ApplicationCreator}.
 */
@Singleton
@Weight(DEFAULT_INJECT_WEIGHT)
public class ApplicationCreatorDefault extends AbstractCreator implements ApplicationCreator {
    /**
     * The application class name.
     */
    public static final String APPLICATION_NAME = "HelidonInjection__Application";
    public static final TypeName IP_PROVIDER_TYPE = TypeName.create(InjectionPointProvider.class);
    private static final TypeName CREATOR = TypeName.create(ApplicationCreatorDefault.class);
    private static final TypeName BINDER_TYPE = TypeName.create(ServiceInjectionPlanBinder.class);

    /**
     * Service loader based constructor.
     *
     * @deprecated this is a Java ServiceLoader implementation and the constructor should not be used directly
     */
    @Deprecated
    public ApplicationCreatorDefault() {
    }

    /**
     * Will uppercase the first letter of the provided name.
     *
     * @param name the name
     * @return the mame with the first letter capitalized
     */
    public static String upperFirstChar(String name) {
        if (name.isEmpty()) {
            return name;
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    static boolean isAllowListedProviderName(ApplicationCreatorConfigOptions configOptions,
                                             TypeName typeName) {
        PermittedProviderType opt = configOptions.permittedProviderTypes();
        if (PermittedProviderType.ALL == opt) {
            return true;
        } else if (PermittedProviderType.NONE == opt) {
            return false;
        } else {
            return configOptions.permittedProviderNames().contains(typeName.name());
        }
    }

    static ServiceInfoCriteria toServiceInfoCriteria(TypeName typeName) {
        return ServiceInfoCriteria.builder()
                .serviceTypeName(typeName)
                .build();
    }

    static ServiceProvider<?> toServiceProvider(TypeName typeName,
                                                Services services) {
        return services.lookupFirst(toServiceInfoCriteria(typeName), true).orElseThrow();
    }

    static boolean isProvider(TypeName typeName,
                              Services services) {
        ServiceProvider<?> sp = toServiceProvider(typeName, services);
        return sp.isProvider();
    }

    static boolean isAllowListedProviderQualifierTypeName(ApplicationCreatorConfigOptions configOptions,
                                                          TypeName typeName,
                                                          Services services) {
        Set<TypeName> permittedTypeNames = configOptions.permittedProviderQualifierTypeNames();
        if (permittedTypeNames.isEmpty()) {
            return false;
        }

        ServiceProvider<?> sp = toServiceProvider(typeName, services);
        Set<TypeName> spQualifierTypeNames = sp.qualifiers().stream()
                .map(Annotation::typeName)
                .collect(Collectors.toSet());
        spQualifierTypeNames.retainAll(permittedTypeNames);
        return !spQualifierTypeNames.isEmpty();
    }

    static TypeName toApplicationTypeName(ApplicationCreatorRequest req) {
        ApplicationCreatorCodeGen codeGen = Objects.requireNonNull(req.codeGen());
        String packageName = codeGen.packageName().orElse(null);
        if (packageName == null) {
            packageName = "inject";
        }
        String className = Objects.requireNonNull(codeGen.className().orElse(null));
        return TypeName.builder()
                .packageName(packageName)
                .className(className)
                .build();
    }

    static String toApplicationName(ApplicationCreatorRequest req) {
        if (req.moduleName().isPresent()) {
            return req.moduleName().get();
        }

        return "unknown/" + req.codeGen().packageName().orElse("inject");
    }

    /**
     * Generates the source and class file for {@link Application} using the current classpath.
     *
     * @param req the request
     * @return the response for application creation
     */
    @Override
    public ApplicationCreatorResponse createApplication(ApplicationCreatorRequest req) {
        ApplicationCreatorResponse.Builder builder = ApplicationCreatorResponse.builder();

        if (req.serviceTypeNames() == null) {
            return handleError(req, new ToolsException("ServiceTypeNames is required to be passed"), builder);
        }

        if (req.codeGen() == null) {
            return handleError(req, new ToolsException("CodeGenPaths are required"), builder);
        }

        List<TypeName> providersInUseThatAreAllowed = providersNotAllowed(req);
        if (!providersInUseThatAreAllowed.isEmpty()) {
            return handleError(req,
                               new ToolsException("There are dynamic " + Provider.class.getSimpleName()
                                                          + "s being used that are not allow-listed: "
                                                          + providersInUseThatAreAllowed
                                                          + "; see the documentation for examples of allow-listing."), builder);
        }

        try {
            return codegen(req, builder);
        } catch (ToolsException te) {
            return handleError(req, te, builder);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return handleError(req, new ToolsException("Failed during create", t), builder);
        }
    }

    @SuppressWarnings("rawtypes")
    List<TypeName> providersNotAllowed(ApplicationCreatorRequest req) {
        InjectionServices injectionServices = InjectionServices.injectionServices().orElseThrow();
        Services services = injectionServices.services();

        List<ServiceProvider<Provider>> providers = services.lookupAll(Provider.class);
        if (providers.isEmpty()) {
            return List.of();
        }

        List<TypeName> providersInUseThatAreNotAllowed = new ArrayList<>();
        for (TypeName typeName : req.serviceTypeNames()) {
            if (!isAllowListedProviderName(req.configOptions(), typeName)
                    && isProvider(typeName, services)
                    && !isAllowListedProviderQualifierTypeName(req.configOptions(), typeName, services)) {
                providersInUseThatAreNotAllowed.add(typeName);
            }
        }
        return providersInUseThatAreNotAllowed;
    }

    ApplicationCreatorResponse codegen(ApplicationCreatorRequest req,
                                       ApplicationCreatorResponse.Builder builder) {

        TypeName applicationType = toApplicationTypeName(req);
        builder.addServiceType(applicationType);

        ClassModel.Builder classModel = ClassModel.builder()
                .copyright(CopyrightHandler.copyright(CREATOR,
                                                      CREATOR,
                                                      applicationType))
                .description("Generated Application to provide explicit bindings for known services.")
                .type(applicationType)
                .addAnnotation(GeneratedAnnotationHandler.create(CREATOR,
                                                                 CREATOR,
                                                                 applicationType,
                                                                 "1",
                                                                 ""))
                .addInterface(TypeNames.APPLICATION);

        // deprecated default constructor - application should always be service loaded
        classModel.addConstructor(ctr -> ctr.javadoc(Javadoc.builder()
                                                             .addLine(
                                                                     "Constructor only for use by {@link java.util"
                                                                             + ".ServiceLoader}.")
                                                             .addTag("deprecated", "to be used by Java Service Loader only")
                                                             .build())
                .addAnnotation(Annotations.DEPRECATED));

        // public String name()
        String applicationName = toApplicationName(req);
        classModel.addMethod(nameMethod -> nameMethod
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(io.helidon.common.types.TypeNames.STRING)
                .name("name")
                .addLine("return \"" + applicationName + "\";"));

        InjectionServices services = req.services()
                .orElseGet(() -> InjectionServices.injectionServices().orElseThrow());

        // public void configure(ServiceInjectionPlanBinder binder)
        classModel.addMethod(configureMethod -> configureMethod
                .addAnnotation(Annotations.OVERRIDE)
                .name("configure")
                .addParameter(binderParam -> binderParam
                        .name("binder")
                        .type(BINDER_TYPE))
                .update(it -> createConfigureMethodBody(services, req.serviceTypeNames(), it)));

        StringWriter sw = new StringWriter();
        try {
            classModel.build().write(sw, "    ");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create class model for Application class", e);
        }
        String body = sw.toString();

        if (req.codeGenPaths().isPresent()
                && req.codeGenPaths().get().generatedSourcesPath().isPresent()) {
            codegen(req, applicationType, body);
        }

        GeneralCodeGenDetail codeGenDetail = GeneralCodeGenDetail.builder()
                .serviceTypeName(applicationType)
                .body(body)
                .build();
        ApplicationCreatorCodeGen codeGenResponse = ApplicationCreatorCodeGen.builder()
                .packageName(applicationType.packageName())
                .className(applicationType.className())
                .classPrefixName(req.codeGen().classPrefixName())
                .build();
        return builder
                .applicationCodeGen(codeGenResponse)
                .putServiceTypeDetail(applicationType, codeGenDetail)
                .moduleName(req.moduleName())
                .build();
    }

    BindingPlan bindingPlan(InjectionServices services,
                            TypeName serviceTypeName) {

        ServiceInfoCriteria si = toServiceInfoCriteria(serviceTypeName);
        ServiceProvider<?> sp = services.services().lookupFirst(si);
        TypeName serviceDescriptorType = sp.descriptorType();

        if (!isQualifiedInjectionTarget(sp)) {
            return new BindingPlan(serviceDescriptorType, Set.of());
        }

        List<IpId> dependencies = sp.dependencies();
        if (dependencies.isEmpty()) {
            return new BindingPlan(serviceDescriptorType, Set.of());
        }

        Set<Binding> bindings = new LinkedHashSet<>();
        for (IpId dependency : dependencies) {
            // type of the result that satisfies the injection point (full generic type)
            TypeName ipType = dependency.typeName();

            InjectionPlan iPlan = injectionPlan(services, sp, dependency);
            List<ServiceProvider<?>> qualified = iPlan.qualifiedProviders();
            List<?> unqualified = iPlan.unqualifiedProviders();

            BindingType type = bindingType(ipType);
            boolean isProvider = isProvider(ipType);

            if (qualified.isEmpty() && !unqualified.isEmpty()) {
                Object target = unqualified.getFirst();
                TypeName targetType;
                if (target instanceof Class<?> aClass) {
                    targetType = TypeName.create(aClass);
                } else if (target instanceof TypeName tn) {
                    targetType = tn;
                } else {
                    // the actual class name may be some generated type, we are interested in the contract required
                    targetType = dependency.contract();
                }
                bindings.add(new Binding(BindingTime.RUNTIME, type, dependency, isProvider, List.of(targetType)));
            } else {
                bindings.add(new Binding(BindingTime.BUILD, type, dependency, isProvider, qualified.stream()
                        .map(ServiceProvider::descriptorType)
                        .toList()));
            }
        }

        return new BindingPlan(serviceDescriptorType, bindings);
    }

    /**
     * Perform the file creation and javac it.
     *
     * @param req                 the request
     * @param applicationTypeName the application type name
     * @param body                the source code / body to generate
     */
    void codegen(ApplicationCreatorRequest req,
                 TypeName applicationTypeName,
                 String body) {
        CodeGenFiler filer = createDirectCodeGenFiler(req.codeGenPaths().orElse(null), req.analysisOnly());
        Path applicationJavaFilePath = filer.codegenJavaFilerOut(applicationTypeName, body).orElse(null);

        String outputDirectory = req.codeGenPaths().isEmpty()
                ? null : req.codeGenPaths().get().outputPath().orElse(null);
        if (outputDirectory != null) {
            File outDir = new File(outputDirectory);

            // setup meta-inf services
            codegenMetaInfServices(filer,
                                   req.codeGenPaths().orElse(null),
                                   Map.of(TypeNames.INJECT_APPLICATION, List.of(applicationTypeName.name())));

            // compile, but only if we generated the source file
            if (applicationJavaFilePath != null) {
                CompilerOptions opts = req.compilerOptions().orElse(null);
                JavaC.Builder compilerBuilder = JavaC.builder()
                        .outputDirectory(outDir)
                        .logger(logger())
                        .messager(req.messager().orElseThrow());
                if (opts != null) {
                    compilerBuilder
                            .classpath(opts.classpath())
                            .modulepath(opts.modulepath())
                            .sourcepath(opts.sourcepath())
                            .source(opts.source())
                            .target(opts.target())
                            .commandLineArgs(opts.commandLineArguments());
                }
                JavaC compiler = compilerBuilder.build();
                JavaC.Result result = compiler.compile(applicationJavaFilePath.toFile());
                ToolsException e = result.maybeGenerateError();
                if (e != null) {
                    throw new ToolsException("Failed to compile: " + applicationJavaFilePath, e);
                }
            }
        }
    }

    void codegenMetaInfServices(CodeGenFiler filer,
                                CodeGenPaths paths,
                                Map<String, List<String>> metaInfServices) {
        filer.codegenMetaInfServices(paths, metaInfServices);
    }

    ApplicationCreatorResponse handleError(ApplicationCreatorRequest request,
                                           ToolsException e,
                                           ApplicationCreatorResponse.Builder builder) {
        if (request.throwIfError()) {
            throw e;
        }

        return builder.error(e).success(false).build();
    }

    private boolean isProvider(TypeName ipType) {
        if (ipType.isOptional() || ipType.isList() && !ipType.typeArguments().isEmpty()) {
            return isProvider(ipType.typeArguments().getFirst());
        }
        return InjectTypes.JAKARTA_PROVIDER.equals(ipType)
                || InjectTypes.JAVAX_PROVIDER.equals(ipType)
                || InjectTypes.INJECTION_POINT_PROVIDER.equals(ipType);
    }

    private BindingType bindingType(TypeName ipType) {
        if (ipType.isList()) {
            return BindingType.MANY;
        }
        if (ipType.isOptional()) {
            return BindingType.OPTIONAL;
        }
        return BindingType.SINGLE;
    }

    private InjectionPlan injectionPlan(InjectionServices services,
                                        ServiceProvider<?> self,
                                        IpId dependency) {
        ServiceInfoCriteria dependencyTo = dependency.toCriteria();
        if (self.contracts().containsAll(dependencyTo.contracts()) && self.qualifiers().equals(dependencyTo.qualifiers())) {
            // criteria must have a single contract for each injection point
            // if this service implements the contracts actually required, we must look for services with lower weight
            // but only if we also have the same qualifiers
            dependencyTo = ServiceInfoCriteria.builder(dependencyTo)
                    .weight(self.weight())
                    .build();
        }

        if (self instanceof InjectionResolver ir) {
            Optional<Object> resolved = ir.resolve(dependency, services, self, false);
            Object target = resolved instanceof Optional<?> opt
                    ? opt.orElse(null)
                    : resolved;
            if (target != null) {
                return new InjectionPlan(toUnqualified(target).toList(), toQualified(target).toList());
            }
        }

        List<ServiceProvider<?>> qualifiedProviders = services.services().lookupAll(dependencyTo, false);
        List<ServiceProvider<?>> unqualifiedProviders = List.of();
        if (qualifiedProviders.isEmpty()) {
            if (dependency.typeName().isOptional()) {
                return new InjectionPlan(List.of(), List.of());
            } else {
                unqualifiedProviders = injectionPointProvidersFor(services.services(), dependency);
            }
        }

        // remove current service provider from matches
        qualifiedProviders = qualifiedProviders.stream()
                .filter(it -> !it.descriptor().equals(self.descriptor()))
                .toList();

        unqualifiedProviders = unqualifiedProviders.stream()
                .filter(it -> !it.descriptor().equals(self.descriptor()))
                .toList();

        // the list now contains all providers that match the processed injection points
        return new InjectionPlan(unqualifiedProviders, qualifiedProviders);
    }

    private Stream<ServiceProvider<?>> toQualified(Object target) {
        if (target instanceof Collection<?> collection) {
            return collection.stream()
                    .flatMap(this::toQualified);
        }
        return (target instanceof ServiceProvider<?> sp)
                ? Stream.of(sp)
                : Stream.of();
    }

    private Stream<?> toUnqualified(Object target) {
        if (target instanceof Collection<?> collection) {
            return collection.stream()
                    .flatMap(this::toUnqualified);
        }
        return (target instanceof ServiceProvider<?>)
                ? Stream.of()
                : Stream.of(target);
    }

    private List<ServiceProvider<?>> injectionPointProvidersFor(Services services, IpId ipoint) {
        if (ipoint.qualifiers().isEmpty()) {
            return List.of();
        }
        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder(ipoint.toCriteria())
                .qualifiers(Set.of())
                .addContract(IP_PROVIDER_TYPE)
                .build();
        return services.lookupAll(criteria);
    }


    private void createConfigureMethodBody(InjectionServices services, List<TypeName> serviceTypes, Method.Builder method) {
        // first collect required dependencies by descriptor

        Map<TypeName, Set<Binding>> injectionPlan = new LinkedHashMap<>();
        for (TypeName serviceType : serviceTypes) {
            BindingPlan plan = bindingPlan(services, serviceType);
            if (!plan.bindings.isEmpty()) {
                injectionPlan.put(plan.descriptorType(), plan.bindings());
            }
        }

        boolean supportNulls = false;
        // we group all bindings by descriptor they belong to
        injectionPlan.forEach((descriptorType, bindings) -> {
            method.addLine("binder.bindTo(@" + descriptorType.fqName() + "@.INSTANCE)")
                    .increasePadding();

            for (Binding binding : bindings) {
                String ipId = "@" + binding.ipInfo().descriptor().fqName() + "@." + binding.ipInfo().field();

                switch (binding.time()) {
                case RUNTIME -> {
                    switch (binding.type()) {
                    case SINGLE -> {
                        if (supportNulls) {
                            method.add(".runtimeBindNullable");
                        } else {
                            method.add(".runtimeBind");
                        }
                    }
                    case OPTIONAL -> method.add(".runtimeBindOptional");
                    case MANY -> method.add(".runtimeBindMany");
                    }
                    // such ass .runtimeBind(MyDescriptor.IP_ID_0, false, ConfigBean.class)
                    method.add("(")
                            .add(ipId + ", ")
                            .add(binding.useProvider + ", ")
                            .addLine("@" + binding.typeNames().getFirst().fqName() + "@.class)");
                }
                case BUILD -> {
                    switch (binding.type()) {
                    case SINGLE -> {
                        if (binding.typeNames().isEmpty()) {
                            if (supportNulls) {
                                method.addLine(".bindNull(" + ipId + ")");
                            } else {
                                throw new ToolsException("Injection point requires a value, but no provider discovered: " + binding.ipInfo());
                            }
                        } else {
                            method.add(".bind(")
                                    .add(ipId + ",")
                                    .add(binding.useProvider() + ", ")
                                    .addLine("@" + binding.typeNames.getFirst().fqName() + "@.INSTANCE)");
                        }
                    }
                    case OPTIONAL -> {
                        method.add(".bindOptional(" + ipId + ", ")
                                .add(String.valueOf(binding.useProvider()));
                        if (binding.typeNames.isEmpty()) {
                            method.addLine(")");
                        } else {
                            method.addLine(", @" + binding.typeNames.getFirst().fqName() + "@.INSTANCE)");
                        }
                    }
                    case MANY -> {
                        method.add(".bindMany(" + ipId + ", ")
                                .add(String.valueOf(binding.useProvider()));
                        if (binding.typeNames.isEmpty()) {
                            method.addLine(")");
                        } else {
                            method.add(", ")
                                    .add(binding.typeNames.stream()
                                                     .map(targetDescriptor -> "@" + targetDescriptor.fqName()
                                                             + "@.INSTANCE")
                                                     .collect(Collectors.joining(", ")))
                                    .addLine(")");
                        }
                    }
                    }
                }
                }
            }

            /*
            Commit the dependencies
             */
            method.addLine(".commit();")
                    .decreasePadding()
                    .addLine("");
        });
    }

    enum BindingType {
        SINGLE,
        OPTIONAL,
        MANY
    }

    enum BindingTime {
        BUILD,
        RUNTIME
    }

    record InjectionPlan(List<?> unqualifiedProviders,
                         List<ServiceProvider<?>> qualifiedProviders) {
    }

    record BindingPlan(TypeName descriptorType,
                       Set<Binding> bindings) {
    }

    record Binding(BindingTime time,
                   BindingType type,
                   IpId ipInfo,
                   boolean useProvider,
                   List<TypeName> typeNames) {
    }
}
