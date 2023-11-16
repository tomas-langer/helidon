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
import java.util.HashMap;
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
import io.helidon.inject.api.InjectionPointProvider;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.IpInfo;
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

        // TODO the maven plugin should analyze all applications on classpath, and assign a higher weight
        // to this one, so we always load the last one

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
                .addAnnotation(Annotation.builder()
                                       .type(Weight.class)
                                       // .putValue("value", req.weight())
                                       .putValue("value", 100)
                                       .build())
                .addInterface(TypeNames.APPLICATION);

        // deprecated default constructor - application should always be service loaded
        classModel.addConstructor(ctr -> ctr.javadoc(Javadoc.builder()
                                                             .addLine(
                                                                     "Constructor only for use by {@link java.util"
                                                                             + ".ServiceLoader}.")
                                                             .addTag("deprecated", "to be used by Java Service Loader only")
                                                             .build())
                .addAnnotation(Annotation.create(Deprecated.class)));

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
            codegen(null, req, applicationType, body);
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
                            TypeName serviceTypeName,
                            List<IpInfo> dependencies) {

        ServiceInfoCriteria si = toServiceInfoCriteria(serviceTypeName);
        ServiceProvider<?> sp = services.services().lookupFirst(si);
        TypeName serviceDescriptorType = sp.descriptorType();

        if (!isQualifiedInjectionTarget(sp)) {
            return new BindingPlan(serviceDescriptorType, Set.of());
        }

        if (dependencies.isEmpty()) {
            return new BindingPlan(serviceDescriptorType, Set.of());
        }

        Set<Binding> bindings = new LinkedHashSet<>();
        for (IpInfo ipoint : dependencies) {
            IpId<?> id = ipoint.id();

            // type of the result that satisfies the injection point (full generic type)
            TypeName ipType = ipoint.typeName();

            InjectionPlan iPlan = injectionPlan(services, sp, ipoint);
            List<ServiceProvider<?>> qualified = iPlan.qualifiedProviders();
            List<?> unqualified = iPlan.unqualifiedProviders();

            BindingType type;
            if (ipType.isList()) {
                type = BindingType.MANY;
            } else {
                if (qualified.isEmpty()) {
                    if (unqualified.isEmpty()) {
                        type = BindingType.VOID;
                    } else {
                        type = BindingType.RUNTIME;
                    }
                } else {
                    type = BindingType.BIND;
                }
            }
            switch (type) {
            case RUNTIME -> {
                Object target = unqualified.getFirst();
                TypeName targetType;
                if (target instanceof Class<?> aClass) {
                    targetType = TypeName.create(aClass);
                } else if (target instanceof TypeName tn) {
                    targetType = tn;
                } else {
                    // the actual class name may be some generated type, we are interested in the contract required
                    targetType = ipoint.contract();
                }
                bindings.add(new Binding(type, ipoint, List.of(targetType)));
            }
            case BIND -> bindings.add(new Binding(type, ipoint, List.of(qualified.getFirst().descriptorType())));
            case MANY -> bindings.add(new Binding(type, ipoint, qualified.stream()
                    .map(ServiceProvider::descriptorType)
                    .toList()));
            case VOID -> bindings.add(new Binding(type, ipoint, List.of()));
            }
        }

        return new BindingPlan(serviceDescriptorType, bindings);
    }

    /**
     * Perform the file creation and javac it.
     *
     * @param injectionServices   the injection services to use
     * @param req                 the request
     * @param applicationTypeName the application type name
     * @param body                the source code / body to generate
     */
    void codegen(InjectionServices injectionServices,
                 ApplicationCreatorRequest req,
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

    private InjectionPlan injectionPlan(InjectionServices services, ServiceProvider<?> self, IpInfo ipoint) {
        ServiceInfoCriteria dependencyTo = toServiceInfoCriteria(ipoint);

        if (self instanceof InjectionResolver ir) {
            Optional<Object> resolved = ir.resolve(ipoint, services, self, false);
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
            if (ipoint.typeName().isOptional()) {
                return new InjectionPlan(List.of(), List.of());
            } else {
                unqualifiedProviders = injectionPointProvidersFor(services.services(), ipoint);
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

    private List<ServiceProvider<?>> injectionPointProvidersFor(Services services, IpInfo ipoint) {
        if (ipoint.qualifiers().isEmpty()) {
            return List.of();
        }
        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder(toServiceInfoCriteria(ipoint))
                .qualifiers(Set.of())
                .addContract(IP_PROVIDER_TYPE)
                .build();
        return services.lookupAll(criteria);
    }

    private ServiceInfoCriteria toServiceInfoCriteria(IpInfo ipoint) {
        return ServiceInfoCriteria.builder()
                .addContract(ipoint.contract())
                .qualifiers(ipoint.qualifiers())
                .build();
    }

    private void createConfigureMethodBody(InjectionServices services, List<TypeName> serviceTypes, Method.Builder method) {
        // first collect required dependencies by descriptor
        Map<IpId<?>, IpInfo> dependenciesById = new HashMap<>();
        for (TypeName typeName : serviceTypes) {
            gatherDependencies(services, typeName, dependenciesById);
        }

        Map<TypeName, List<IpInfo>> dependenciesByService = new HashMap<>();
        dependenciesById.forEach((id, info) -> {
            dependenciesByService.computeIfAbsent(id.serviceType(), it -> new ArrayList<>())
                    .add(info);
        });

        Map<TypeName, Set<Binding>> injectionPlan = new LinkedHashMap<>();
        dependenciesByService.forEach((service, dependencies) -> {
            BindingPlan plan = bindingPlan(services, service, dependencies);
            if (!plan.bindings.isEmpty()) {
                injectionPlan.put(plan.descriptorType(), plan.bindings());
            }
        });

        // we group all bindings by descriptor they belong to
        injectionPlan.forEach((descriptorType, bindings) -> {
            String descriptor = "@" + descriptorType.fqName() + "@";

            method.addLine("binder.bindTo(" + descriptor + ".INSTANCE)")
                    .increasePadding()
                    .update(it -> {
                        for (Binding binding : bindings) {
                            it.add(".")
                                    .add(switch (binding.type()) {
                                        case BIND -> "bind";
                                        case MANY -> "bindMany";
                                        case VOID -> "bindVoid";
                                        case RUNTIME -> "runtimeBind";
                                    })
                                    .add("(")
                                    .add(descriptor)
                                    .add(".")
                                    .add(binding.ipInfo().field());

                            // we trust our own method to prepare the stuff correctly
                            if (!binding.typeNames.isEmpty()) {
                                if (binding.type() == BindingType.RUNTIME) {
                                    it.add(", @" + binding.typeNames().getFirst().fqName() + "@.class");
                                } else {
                                    it.add((", "))
                                            .add(binding.typeNames.stream()
                                                         .map(targetDescriptor -> "@" + targetDescriptor.fqName()
                                                                 + "@.INSTANCE")
                                                         .collect(Collectors.joining(", ")));
                                }
                            }
                            it.addLine(")");
                        }
                    })
                    .addLine(".commit();")
                    .decreasePadding()
                    .addLine("");
        });
    }

    private void gatherDependencies(InjectionServices services,
                                    TypeName serviceTypeName,
                                    Map<IpId<?>, IpInfo> dependenciesByServiceType) {
        ServiceInfoCriteria si = toServiceInfoCriteria(serviceTypeName);
        ServiceProvider<?> sp = services.services().lookupFirst(si);

        if (!isQualifiedInjectionTarget(sp)) {
            return;
        }

        List<IpInfo> dependencies = sp.dependencies();
        if (dependencies.isEmpty()) {
            return;
        }

        for (IpInfo dependency : dependencies) {
            IpId<?> id = dependency.id();
            dependenciesByServiceType.put(id, dependency);
        }
    }

    enum BindingType {
        BIND,
        MANY,
        VOID,
        RUNTIME
    }

    record InjectionPlan(List<?> unqualifiedProviders,
                         List<ServiceProvider<?>> qualifiedProviders) {
    }

    record BindingPlan(TypeName descriptorType,
                       Set<Binding> bindings) {
        void merge(BindingPlan plan) {
            if (!descriptorType.equals(plan.descriptorType)) {
                throw new IllegalStateException("Descriptor type mis-match in injection plan. " + descriptorType
                                                        + ", and " + plan.descriptorType);
            }
            bindings.addAll(plan.bindings);
        }
    }

    record Binding(BindingType type,
                   IpInfo ipInfo,
                   List<TypeName> typeNames) {
    }
}
