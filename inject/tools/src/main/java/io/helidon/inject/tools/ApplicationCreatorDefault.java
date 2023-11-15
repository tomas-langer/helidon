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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import io.helidon.inject.api.InjectionException;
import io.helidon.inject.api.InjectionPointProvider;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.IpInfo;
import io.helidon.inject.api.ModuleComponent;
import io.helidon.inject.api.ServiceDependencies;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceInjectionPlanBinder;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.Services;
import io.helidon.inject.runtime.VoidServiceProvider;
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

    static String toModuleName(ApplicationCreatorRequest req) {
        return req.moduleName().orElse(ModuleInfoDescriptor.DEFAULT_MODULE_NAME);
    }

    static Optional<TypeName> moduleServiceTypeOf(InjectionServices injectionServices,
                                                  String moduleName) {
        Services services = injectionServices.services();
        ServiceProvider<?> serviceProvider;
        try {
            serviceProvider = services.lookup(ModuleComponent.class, moduleName);
        } catch (InjectionException e) {
            return Optional.empty();
        }
        return Optional.of(serviceProvider.serviceType());
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
        String applicationName = toModuleName(req);
        classModel.addMethod(nameMethod -> nameMethod
                .addAnnotation(Annotations.OVERRIDE)
                .typeName(io.helidon.common.types.TypeNames.STRING)
                .name("name")
                .addLine("return \"" + applicationName + "\";"));

        // public void configure(ServiceInjectionPlanBinder binder)
        classModel.addMethod(configureMethod -> configureMethod
                .addAnnotation(Annotations.OVERRIDE)
                .name("configure")
                .addParameter(binderParam -> binderParam
                        .name("binder")
                        .type(BINDER_TYPE))
                .update(it -> createConfigureMethodBody(it, req.serviceTypeNames())));

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

    Map<TypeName, BindingPlan> toServiceTypeInjectionPlan(Services services,
                                                          TypeName serviceTypeName) {

        ServiceInfoCriteria si = toServiceInfoCriteria(serviceTypeName);
        ServiceProvider<?> sp = services.lookupFirst(si);

        if (!isQualifiedInjectionTarget(sp)) {
            return Map.of();
        }

        List<ServiceDependencies> dependencies = sp.dependencies();
        if (dependencies.isEmpty()) {
            return Map.of();
        }

        Map<TypeName, BindingPlan> response = new LinkedHashMap<>();

        for (ServiceDependencies dependency : dependencies) {
            TypeName depServiceType = dependency.serviceType();
            // service provider of the service receiving the injection point
            ServiceProvider<?> depSp = services.lookupFirst(toServiceInfoCriteria(depServiceType));
            TypeName descriptorTypeName = depSp.descriptorType();

            Set<Binding> bindings = new LinkedHashSet<>();
            List<IpInfo> ipoints = dependency.dependencies();

            for (IpInfo ipoint : ipoints) {
                IpId<?> id = ipoint.id();
                ipoint.qualifiers();
                ipoint.contract();
                ipoint.access();
                ipoint.annotations();

                // type of the result that satisfies the injection point (full generic type)
                TypeName ipType = id.typeName();
                // contract of the service that satisfies this injection point
                TypeName contractType = ipoint.contract();

                InjectionPlan iPlan = injectionPlan(services, depSp, ipoint);
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
                    Class<?> targetType;
                    if (target instanceof Class<?> aClass) {
                        targetType = aClass;
                    } else {
                        targetType = target.getClass();
                    }
                    bindings.add(new Binding(type, ipoint, List.of(TypeName.create(targetType))));
                }
                case BIND -> bindings.add(new Binding(type, ipoint, List.of(qualified.getFirst().descriptorType())));
                case MANY -> bindings.add(new Binding(type, ipoint, qualified.stream()
                        .map(ServiceProvider::descriptorType)
                        .toList()));
                case VOID -> bindings.add(new Binding(type, ipoint, List.of()));
                }
            }

            response.put(serviceTypeName, new BindingPlan(descriptorTypeName, bindings));
        }

        return response;
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

    private InjectionPlan injectionPlan(Services services, ServiceProvider<?> self, IpInfo ipoint) {
        ServiceInfoCriteria dependencyTo = toServiceInfoCriteria(ipoint);

        List<ServiceProvider<?>> qualifiedProviders = services.lookupAll(dependencyTo, false);
        List<ServiceProvider<?>> unqualifiedProviders = List.of();
        if (qualifiedProviders.isEmpty()) {
            if (ipoint.id().typeName().isOptional()) {
                qualifiedProviders = VoidServiceProvider.LIST_INSTANCE;
            } else {
                unqualifiedProviders = injectionPointProvidersFor(services, ipoint);
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

    private void createConfigureMethodBody(Method.Builder method, List<TypeName> serviceTypes) {
        InjectionServices injectionServices = InjectionServices.injectionServices().orElseThrow();
        Services services = injectionServices.services();

        Map<TypeName, BindingPlan> injectionPlans = new LinkedHashMap<>();

        for (TypeName serviceType : serviceTypes) {
            // we need to merge the plan - as we may have a constructor injection combined with
            // field injection discovered in another service, registered later, and that would override the constructor binding
            Map<TypeName, BindingPlan> plansByServices = toServiceTypeInjectionPlan(services, serviceType);

            plansByServices.forEach((typeName, plan) -> {
                if (injectionPlans.containsKey(typeName)) {
                    injectionPlans.get(typeName).merge(plan);
                } else {
                    injectionPlans.put(typeName, plan);
                }
            });
        }

        injectionPlans.forEach((serviceType, plan) -> {
            String descriptor = "@" + plan.descriptorType().fqName() + "@";

            method.addLine("binder.bindTo(" + descriptor + ".INSTANCE)")
                    .increasePadding()
                    .update(it -> {
                        for (Binding binding : plan.bindings()) {
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
                    .addLine(".commit()")
                    .decreasePadding()
                    .addLine("");
        });
    }

    enum BindingType {
        BIND,
        MANY,
        VOID,
        RUNTIME
    }

    record InjectionPlan(List<ServiceProvider<?>> unqualifiedProviders,
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
