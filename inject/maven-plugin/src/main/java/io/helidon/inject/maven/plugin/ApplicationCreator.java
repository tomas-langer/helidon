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

package io.helidon.inject.maven.plugin;

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

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.CodegenOptions;
import io.helidon.codegen.CodegenScope;
import io.helidon.codegen.CopyrightHandler;
import io.helidon.codegen.GeneratedAnnotationHandler;
import io.helidon.codegen.ModuleInfo;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Javadoc;
import io.helidon.codegen.classmodel.Method;
import io.helidon.codegen.compiler.Compiler;
import io.helidon.codegen.compiler.CompilerOptions;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectTypes;
import io.helidon.inject.api.InjectionPointProvider;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceInjectionPlanBinder;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.Services;
import io.helidon.inject.codegen.InjectCodegenTypes;
import io.helidon.inject.codegen.InjectOptions;
import io.helidon.inject.spi.InjectionResolver;

import jakarta.inject.Provider;

import static io.helidon.inject.runtime.ServiceUtils.isQualifiedInjectionTarget;

/**
 * The default implementation for {@link io.helidon.inject.maven.plugin.ApplicationCreator}.
 */
public class ApplicationCreator {
    /**
     * The application class name.
     */
    public static final String APPLICATION_NAME = "HelidonInjection__Application";
    public static final TypeName IP_PROVIDER_TYPE = TypeName.create(InjectionPointProvider.class);
    private static final TypeName CREATOR = TypeName.create(ApplicationCreator.class);
    private static final TypeName BINDER_TYPE = TypeName.create(ServiceInjectionPlanBinder.class);
    private final MavenCodegenContext ctx;
    private final boolean failOnError;
    private final PermittedProviderType permittedProviderType;
    private final Set<TypeName> permittedProviderTypes;
    private final Set<TypeName> permittedProviderQualifierTypes;
    private final String moduleName;

    ApplicationCreator(MavenCodegenContext scanContext, boolean failOnError) {
        this.ctx = scanContext;
        this.failOnError = failOnError;

        CodegenOptions options = scanContext.options();
        this.moduleName = options.option(InjectOptions.MODULE_NAME)
                .or(() -> scanContext.module().map(ModuleInfo::name))
                .orElse(null);
        this.permittedProviderType = options.option(ApplicationOptions.PERMITTED_PROVIDER_TYPE,
                                                    PermittedProviderType.NAMED,
                                                    PermittedProviderType.class);
        this.permittedProviderTypes = options.listOption(ApplicationOptions.PERMITTED_PROVIDER_TYPE_NAMES)
                .stream()
                .map(TypeName::create)
                .collect(Collectors.toSet());

        permittedProviderQualifierTypes = options.listOption(ApplicationOptions.PERMITTED_PROVIDER_QUALIFIER_TYPE_NAMES)
                .stream()
                .map(TypeName::create)
                .collect(Collectors.toSet());
    }

    /**
     * Generates the source and class file for {@link io.helidon.inject.api.Application} using the current classpath.
     *
     * @param injectionServices injection services to use
     * @param serviceTypes      types to process
     * @param typeName          generated application type name
     * @param compilerOptions   compilation options
     */
    public void createApplication(InjectionServices injectionServices,
                                  Set<TypeName> serviceTypes,
                                  TypeName typeName,
                                  CompilerOptions compilerOptions) {
        Objects.requireNonNull(injectionServices);
        Objects.requireNonNull(serviceTypes);

        List<TypeName> providersInUseThatAreNotAllowed = providersNotAllowed(injectionServices, serviceTypes);
        if (!providersInUseThatAreNotAllowed.isEmpty()) {
            String message = "There are dynamic Providers being used that are not allow-listed: "
                    + providersInUseThatAreNotAllowed
                    + "; see the documentation for examples of allow-listing.";

            handleError(message);
        }

        try {
            codegen(injectionServices, serviceTypes, typeName, compilerOptions);
        } catch (Throwable te) {
            handleError(te, "Failed to code generate application class");
        }
    }

    void codegen(InjectionServices injectionServices,
                 Set<TypeName> serviceTypes,
                 TypeName typeName,
                 CompilerOptions compilerOptions) {
        ClassModel.Builder classModel = ClassModel.builder()
                .copyright(CopyrightHandler.copyright(CREATOR,
                                                      CREATOR,
                                                      typeName))
                .description("Generated Application to provide explicit bindings for known services.")
                .type(typeName)
                .addAnnotation(GeneratedAnnotationHandler.create(CREATOR,
                                                                 CREATOR,
                                                                 typeName,
                                                                 "1",
                                                                 ""))
                .addInterface(InjectCodegenTypes.HELIDON_APPLICATION);

        // deprecated default constructor - application should always be service loaded
        classModel.addConstructor(ctr -> ctr.javadoc(Javadoc.builder()
                                                             .addLine(
                                                                     "Constructor only for use by {@link java.util"
                                                                             + ".ServiceLoader}.")
                                                             .addTag("deprecated", "to be used by Java Service Loader only")
                                                             .build())
                .addAnnotation(Annotations.DEPRECATED));

        String applicationName = applicationName(moduleName, ctx.scope(), typeName);

        // public String name()
        classModel.addMethod(nameMethod -> nameMethod
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(io.helidon.common.types.TypeNames.STRING)
                .name("name")
                .addLine("return \"" + applicationName + "\";"));

        // public void configure(ServiceInjectionPlanBinder binder)
        classModel.addMethod(configureMethod -> configureMethod
                .addAnnotation(Annotations.OVERRIDE)
                .name("configure")
                .addParameter(binderParam -> binderParam
                        .name("binder")
                        .type(BINDER_TYPE))
                .update(it -> createConfigureMethodBody(injectionServices,
                                                        serviceTypes,
                                                        it)));

        Path generated = ctx.filer()
                .writeSourceFile(classModel.build());

        // now we have the source generated, we add the META-INF/service, and compile the class
        ctx.filer()
                .services(CREATOR,
                          InjectCodegenTypes.HELIDON_APPLICATION,
                          List.of(typeName));

        Compiler.compile(compilerOptions, generated);
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

    private static boolean isProvider(TypeName typeName,
                                      Services services) {
        ServiceProvider<?> sp = toServiceProvider(typeName, services);
        return sp.isProvider();
    }

    private static ServiceInfoCriteria toServiceInfoCriteria(TypeName typeName) {
        return ServiceInfoCriteria.builder()
                .serviceTypeName(typeName)
                .build();
    }

    private static ServiceProvider<?> toServiceProvider(TypeName typeName,
                                                        Services services) {
        return services.lookupFirst(toServiceInfoCriteria(typeName), true).orElseThrow();
    }

    private String applicationName(String moduleName, CodegenScope scope, TypeName typeName) {
        String applicationName = moduleName == null
                ? "unknown/" + typeName.packageName()
                : moduleName;
        if (!scope.isProduction()) {
            applicationName = applicationName + "/" + scope.name();
        }
        return applicationName;
    }

    private boolean isAllowListedProviderQualifierTypeName(TypeName typeName,
                                                           Services services) {

        if (permittedProviderQualifierTypes.isEmpty()) {
            return false;
        }

        ServiceProvider<?> sp = toServiceProvider(typeName, services);
        Set<TypeName> spQualifierTypeNames = sp.qualifiers().stream()
                .map(Annotation::typeName)
                .collect(Collectors.toSet());
        spQualifierTypeNames.retainAll(permittedProviderQualifierTypes);
        return !spQualifierTypeNames.isEmpty();
    }

    private boolean isAllowListedProviderName(TypeName typeName) {
        return switch (permittedProviderType) {
            case ALL -> true;
            case NONE -> false;
            default -> permittedProviderTypes.contains(typeName);
        };
    }

    private void handleError(Throwable t, String message) {
        CodegenException ce;

        if (t instanceof CodegenException cex) {
            ce = cex;
        } else {
            ce = new CodegenException(message, t);
        }

        if (failOnError) {
            throw ce;
        } else {
            ctx.logger().log(ce.toEvent(System.Logger.Level.WARNING, message));
        }
    }

    private void handleError(String message) {
        if (failOnError) {
            throw new CodegenException(message);
        } else {
            ctx.logger().log(System.Logger.Level.WARNING, message);
        }
    }

    @SuppressWarnings("rawtypes")
    private List<TypeName> providersNotAllowed(InjectionServices injectionServices,
                                               Set<TypeName> serviceTypes) {
        Services services = injectionServices.services();

        List<ServiceProvider<Provider>> providers = services.lookupAll(Provider.class);
        if (providers.isEmpty()) {
            return List.of();
        }

        List<TypeName> providersInUseThatAreNotAllowed = new ArrayList<>();
        for (TypeName typeName : serviceTypes) {
            if (!isAllowListedProviderName(typeName)
                    && isProvider(typeName, services)
                    && !isAllowListedProviderQualifierTypeName(typeName, services)) {
                providersInUseThatAreNotAllowed.add(typeName);
            }
        }
        return providersInUseThatAreNotAllowed;
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

    private InjectionPlan injectionPlan(InjectionServices injectionServices,
                                        ServiceProvider<?> self,
                                        IpId dependency) {
        ServiceInfoCriteria dependencyTo = dependency.toCriteria();
        Set<Qualifier> qualifiers = dependencyTo.qualifiers();
        if (self.contracts().containsAll(dependencyTo.contracts()) && self.qualifiers().equals(qualifiers)) {
            // criteria must have a single contract for each injection point
            // if this service implements the contracts actually required, we must look for services with lower weight
            // but only if we also have the same qualifiers
            dependencyTo = ServiceInfoCriteria.builder(dependencyTo)
                    .weight(self.weight())
                    .build();
        }

        if (self instanceof InjectionResolver ir) {
            Optional<Object> resolved = ir.resolve(dependency, injectionServices, self, false);
            Object target = resolved instanceof Optional<?> opt
                    ? opt.orElse(null)
                    : resolved;
            if (target != null) {
                return new InjectionPlan(toUnqualified(target).toList(), toQualified(target).toList());
            }
        }

        Services services = injectionServices.services();
        List<ServiceProvider<?>> qualifiedProviders = services.lookupAll(dependencyTo, false);
        List<ServiceProvider<?>> unqualifiedProviders = List.of();

        if (qualifiedProviders.isEmpty()) {
            if (dependency.typeName().isOptional()) {
                return new InjectionPlan(List.of(), List.of());
            } else {
                unqualifiedProviders = injectionPointProvidersFor(services, dependency);
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

    private void createConfigureMethodBody(InjectionServices services, Set<TypeName> serviceTypes, Method.Builder method) {
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
                    // such as .runtimeBind(MyDescriptor.IP_ID_0, false, ConfigBean.class)
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
                                throw new CodegenException("Injection point requires a value, but no provider discovered: "
                                                                   + binding.ipInfo() + " for "
                                                                   + binding.ipInfo().service().fqName());
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
