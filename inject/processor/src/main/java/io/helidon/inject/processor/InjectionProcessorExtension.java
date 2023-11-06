package io.helidon.inject.processor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.helidon.common.GenericType;
import io.helidon.common.processor.CopyrightHandler;
import io.helidon.common.processor.ElementInfoPredicates;
import io.helidon.common.processor.GeneratedAnnotationHandler;
import io.helidon.common.processor.classmodel.Annotation;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.processor.classmodel.Method;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.Modifier;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.processor.spi.HelidonProcessorExtension;
import io.helidon.inject.tools.TypeNames;

class InjectionProcessorExtension implements HelidonProcessorExtension {
    private static final TypeName DEPENDENCIES_RETURN_TYPE = TypeName.builder(io.helidon.common.types.TypeNames.LIST)
            .addTypeArgument(TypeName.builder(TypeName.create("io.helidon.inject.api.IpInfo"))
                                     .addTypeArgument(TypeName.create("?"))
                                     .build())
            .build();
    private static final TypeName GENERATOR = TypeName.create(InjectionProcessorExtension.class);
    private static final Annotation OVERRIDE = Annotation.create(Override.class);

    private final InjectionProcessingContext ctx;

    InjectionProcessorExtension(InjectionProcessingContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean process(RoundContext context) {
        for (TypeInfo typeInfo : context.annotatedTypes(TypeNames.JAKARTA_SINGLETON_TYPE)) {
            TypeName serviceType = typeInfo.typeName();
            // this must result in generating a service descriptor file
            TypeName descriptorType = ctx.serviceDescriptorType(serviceType);

            List<ParamDefinition> params = new ArrayList<>();
            List<MethodDefinition> methods = new ArrayList<>();

            params(typeInfo, methods, params);
            Map<String, GenericTypeDeclaration> genericTypes = genericTypes(params, methods);

            // declare the class
            ClassModel.Builder classModel = ClassModel.builder()
                    .copyright(CopyrightHandler.copyright(GENERATOR,
                                                          serviceType,
                                                          descriptorType))
                    .addAnnotation(GeneratedAnnotationHandler.create(GENERATOR,
                                                                     serviceType,
                                                                     descriptorType,
                                                                     "1",
                                                                     ""))
                    .description("Service descriptor for {@link " + serviceType.fqName() + "}.")
                    .type(descriptorType)
                    .addInterface(serviceDescriptorImplementsType(serviceType))
                    // we need to keep insertion order, as constants may depend on each other
                    .sortStaticFields(false);

            // singleton instance of the descriptor
            classModel.addField(instance -> instance.description("Global singleton instance for this descriptor.")
                    .accessModifier(AccessModifier.PUBLIC)
                    .isStatic(true)
                    .isFinal(true)
                    .type(descriptorType)
                    .name("INSTANCE")
                    .defaultValueContent("new " + descriptorType.className() + "()"));

            // constants for injection point parameter types (used by next section)
            genericTypes.forEach((typeName, generic) -> {
                classModel.addField(field -> field.accessModifier(AccessModifier.PRIVATE)
                        .isStatic(true)
                        .isFinal(true)
                        .type(genericType(generic.typeName()))
                        .name(generic.constantName())
                        .defaultValueContent("new @io.helidon.common.GenericType@<" + typeName + ">() {}"));
            });

            // constants for methods (used by next section)
            for (MethodDefinition method : methods) {
                classModel.addField(field -> field.accessModifier(AccessModifier.PRIVATE)
                        .isStatic(true)
                        .isFinal(true)
                        .type(TypeName.create("io.helidon.inject.api.MethodId"))
                        .name(method.constantName())
                        .defaultValueContent("new @io.helidon.inject.api.MethodId@(\""
                                                     + method.name() + "\", "
                                                     + method.types()
                                .stream()
                                .map(it -> genericTypes.get(it.resolvedName()).constantName())
                                .collect(Collectors.joining(", "))
                                                     + ")"));
            }

            // constant for injection points
            for (ParamDefinition param : params) {
                if (param.methodConstantName() == null) {
                    classModel.addField(field -> field.accessModifier(AccessModifier.PRIVATE)
                            .isStatic(true)
                            .isFinal(true)
                            .type(ipId(param.type()))
                            .name(param.constantName())
                            .defaultValueContent("@io.helidon.inject.api.IpId@.<"
                                                         + param.type.resolvedName()
                                                         + ">builder()"
                                                         + ".elementKind(@io.helidon.common.types.ElementKind@." + param.kind.name() + ")"
                                                         + ".name(\"" + param.ipParamName() + "\")"
                                                         + ".type(" + genericTypes.get(param.type().resolvedName())
                                    .constantName() + ")"
                                                         + ".build()"));
                } else {
                    classModel.addField(field -> field.accessModifier(AccessModifier.PRIVATE)
                            .isStatic(true)
                            .isFinal(true)
                            .type(ipId(param.type()))
                            .name(param.constantName())
                            .defaultValueContent("@io.helidon.inject.api.IpId@.<"
                                                         + param.type.resolvedName()
                                                         + ">builder()"
                                                         + ".elementKind(@io.helidon.common.types.ElementKind@." + param.kind.name() + ")"
                                                         + ".name(\"" + param.ipParamName() + "\")"
                                                         + ".type(" + genericTypes.get(param.type().resolvedName())
                                    .constantName() + ")"
                                                         + ".method(" + param.methodConstantName() + ")"
                                                         + ".build()"));
                }
                classModel.addField(field -> {
                    field.accessModifier(AccessModifier.PRIVATE)
                            .isStatic(true)
                            .isFinal(true)
                            .type(ipInfo(param.type()))
                            .name(param.constantName() + "_INFO");
                    StringBuilder defaultContent = new StringBuilder();
                    defaultContent.append("@io.helidon.inject.api.IpInfo@.<")
                            .append(param.type.resolvedName())
                            .append(">builder()")
                            .append(".id(")
                            .append(param.constantName())
                            .append(")");

                    if (param.isStatic()) {
                        defaultContent.append(".isStatic(true)");
                    }

                    if (!param.qualifiers().isEmpty()) {
                        for (Qualifier qualifier : param.qualifiers()) {
                            defaultContent.append(
                                            "\n.addQualifier(qualifier -> qualifier.typeName(@io.helidon.common.types.TypeName@"
                                                    + ".create(\"")
                                    .append(qualifier.qualifierTypeName())
                                    .append("\"))");
                            qualifier.value().ifPresent(it -> defaultContent.append(".value(\"")
                                    .append(it)
                                    .append("\")"));
                            defaultContent.append(")");
                        }
                    }

                    defaultContent.append(".build()");
                    field.defaultValueContent(defaultContent.toString());
                });

            }

            // add protected constructor
            classModel.addConstructor(constructor -> constructor.description("Constructor with no side effects")
                    .accessModifier(AccessModifier.PROTECTED));

            // ServiceDescriptor method implementations
            // Class<T> serviceType()
            classModel.addMethod(method -> method.addAnnotation(OVERRIDE)
                    .returnType(TypeName.builder()
                                        .type(Class.class)
                                        .addTypeArgument(serviceType)
                                        .build())
                    .name("serviceType")
                    .addLine("return " + serviceType.classNameWithEnclosingNames() + ".class;"));

            // List<InjectionParameterId> dependencies()
            classModel.addMethod(method -> method.addAnnotation(OVERRIDE)
                    .returnType(DEPENDENCIES_RETURN_TYPE)
                    .name("dependencies")
                    .update(it -> createDependenciesBody(it, params)));

            // T instantiate(InjectionContext ctx)
            classModel.addMethod(method -> method.addAnnotation(OVERRIDE)
                    .returnType(serviceType)
                    .name("instantiate")
                    .addParameter(ctxParam -> ctxParam.type(TypeNames.INJECTION_CONTEXT)
                            .name("ctx"))
                    .update(it -> createInstantiateBody(serviceType, it, params)));

            // T injectFields(InjectionContext ctx, T instance)
            classModel.addMethod(method -> method.addAnnotation(OVERRIDE)
                    .name("injectFields")
                    .addParameter(ctxParam -> ctxParam.type(TypeNames.INJECTION_CONTEXT)
                            .name("ctx"))
                    .addParameter(instanceParam -> instanceParam.type(serviceType)
                            .name("instance"))
                    .update(it -> createInjectFieldsBody(it, params)));

            // T injectMethods(InjectionContext ctx, T instance)
            classModel.addMethod(method -> method.addAnnotation(OVERRIDE)
                    .name("injectMethods")
                    .addParameter(ctxParam -> ctxParam.type(TypeNames.INJECTION_CONTEXT)
                            .name("ctx"))
                    .addParameter(instanceParam -> instanceParam.type(serviceType)
                            .name("instance"))
                    .update(it -> createInjectMethodsBody(it, params)));

            // postConstruct()
            lifecycleMethod(typeInfo, TypeNames.JAKARTA_POST_CONSTRUCT_TYPE).ifPresent(method -> {
                classModel.addMethod(postConstruct -> postConstruct.name("postConstruct")
                        .addAnnotation(OVERRIDE)
                        .addParameter(instance -> instance.type(serviceType)
                                .name("instance"))
                        .addLine("instance." + method.elementName() + "();"));
            });
            // preDestroy
            lifecycleMethod(typeInfo, TypeNames.JAKARTA_PRE_DESTROY_TYPE).ifPresent(method -> {
                classModel.addMethod(preDestroy -> preDestroy.name("preDestroy")
                        .addAnnotation(OVERRIDE)
                        .addParameter(instance -> instance.type(serviceType)
                                .name("instance"))
                        .addLine("instance." + method.elementName() + "();"));
            });

            ctx.addServiceDescriptor(serviceType,
                                     descriptorType,
                                     classModel);
        }
        return false;
    }

    private Optional<TypedElementInfo> lifecycleMethod(TypeInfo typeInfo, TypeName annotationType) {
        List<TypedElementInfo> list = typeInfo.elementInfo()
                .stream()
                .filter(ElementInfoPredicates.hasAnnotation(annotationType))
                .toList();
        if (list.isEmpty()) {
            return Optional.empty();
        }
        if (list.size() > 1) {
            throw new IllegalStateException("There is more than one method annotated with " + annotationType.fqName()
                                                    + ", which is not allowed on type " + typeInfo.typeName().fqName());
        }
        TypedElementInfo method = list.getFirst();
        if (method.accessModifier() == AccessModifier.PRIVATE) {
            throw new IllegalStateException("Method annotated with " + annotationType.fqName()
                                                    + ", is private, which is not supported: " + typeInfo.typeName().fqName()
                                                    + "#" + method.elementName());
        }
        if (!method.parameterArguments().isEmpty()) {
            throw new IllegalStateException("Method annotated with " + annotationType.fqName()
                                                    + ", has parameters, which is not supported: " + typeInfo.typeName().fqName()
                                                    + "#" + method.elementName());
        }
        if (!method.typeName().equals(io.helidon.common.types.TypeNames.PRIMITIVE_VOID)) {
            throw new IllegalStateException("Method annotated with " + annotationType.fqName()
                                                    + ", is not void, which is not supported: " + typeInfo.typeName().fqName()
                                                    + "#" + method.elementName());
        }
        return Optional.of(method);
    }

    private void createDependenciesBody(Method.Builder method, List<ParamDefinition> params) {
        method.addLine("return @java.util.List@.of(" + params.stream()
                .map(ParamDefinition::constantName)
                .map(it -> it + "_INFO")
                .collect(Collectors.joining(", "))
                               + ");");
    }

    private Map<String, GenericTypeDeclaration> genericTypes(List<ParamDefinition> params, List<MethodDefinition> methods) {
        // we must use map by string (as type name is equal if the same class, not full generic declaration)
        Map<String, GenericTypeDeclaration> result = new LinkedHashMap<>();
        AtomicInteger counter = new AtomicInteger();

        for (ParamDefinition param : params) {
            result.computeIfAbsent(param.type().resolvedName(),
                                   type -> new GenericTypeDeclaration("TYPE_" + counter.getAndIncrement(),
                                                                      param.type()));
        }

        for (MethodDefinition method : methods) {
            method.types()
                    .forEach(it -> result.computeIfAbsent(it.resolvedName(),
                                                          type -> new GenericTypeDeclaration("TYPE_" + counter.getAndIncrement(),
                                                                                             it)));
        }

        return result;
    }

    private void createInstantiateBody(TypeName serviceType, Method.Builder method, List<ParamDefinition> params) {
        /*
            var ipParam1_serviceProviders = ctx.param(IP_PARAM_1);
            var ipParam2_someOtherName = ctx.param(IP_PARAM_2);
            return new ConfigProducer(ipParam1_serviceProviders, someOtherName);
         */
        List<ParamDefinition> constructorParams = params.stream()
                .filter(it -> it.kind() == ElementKind.CONSTRUCTOR)
                .toList();

        // for each parameter, obtain its value from context
        for (ParamDefinition param : constructorParams) {
            method.addLine("var " + param.ipParamName() + " = ctx.param(" + param.constantName() + ")"
                                   + ";");
        }

        method.addLine("");

        method.addLine("return new " + serviceType.classNameWithEnclosingNames() + "("
                               + constructorParams.stream()
                .map(ParamDefinition::ipParamName)
                .collect(Collectors.joining(", "))
                               + ");");
    }

    private void createInjectFieldsBody(Method.Builder method, List<ParamDefinition> params) {
        /*
        var field1 = ctx.param(IP_PARAM_3);
        instance.field1 = field1;
         */
        List<ParamDefinition> fields = params.stream()
                .filter(it -> it.kind == ElementKind.FIELD)
                .toList();

        for (ParamDefinition param : fields) {
            method.addLine("var " + param.ipParamName() + " = ctx.param(" + param.constantName() + ")"
                                   + ";");
        }

        method.addLine("");

        for (ParamDefinition field : fields) {
            method.addLine("instance." + field.ipParamName() + " = " + field.ipParamName() + ";");
        }
    }

    private void createInjectMethodsBody(Method.Builder method, List<ParamDefinition> params) {
        /*
        var ipParam4_param = ctx.param(IP_PARAM_4);
        instance.someMethod(param)
         */
        Map<String, List<ParamDefinition>> methodCalls = new LinkedHashMap<>();

        params.stream()
                .filter(it -> it.kind == ElementKind.METHOD)
                .forEach(param -> methodCalls.computeIfAbsent(param.methodConstantName(), it -> new ArrayList<>()).add(param));

        List<ParamDefinition> methodsParams = params.stream()
                .filter(it -> it.kind == ElementKind.METHOD)
                .toList();

        // for each parameter, obtain its value from context
        for (ParamDefinition param : methodsParams) {
            method.addLine("var " + param.fieldName() + "_" + param.ipParamName() + " = ctx.param(" + param.constantName() + ")"
                                   + ";");
        }

        method.addLine("");

        for (List<ParamDefinition> value : methodCalls.values()) {
            if (value.isEmpty()) {
                continue;
            }
            ParamDefinition first = value.getFirst();
            method.addLine("instance." + first.ipName() + "("
                                   + value.stream()
                    .map(param -> param.fieldName() + "_" + param.ipParamName())
                    .collect(Collectors.joining(", "))
                                   + ");");
        }
    }

    private TypeName genericType(TypeName type) {
        return TypeName.builder()
                .type(GenericType.class)
                .addTypeArgument(type)
                .build();
    }

    private TypeName ipId(TypeName type) {
        return TypeName.builder(TypeNames.INJECTION_PARAMETER_ID)
                .addTypeArgument(type)
                .build();
    }

    private TypeName ipInfo(TypeName type) {
        return TypeName.builder(TypeNames.INJECTION_PARAMETER_INFO)
                .addTypeArgument(type)
                .build();
    }

    private void params(TypeInfo typeInfo,
                        List<MethodDefinition> methods,
                        List<ParamDefinition> params) {
        AtomicInteger paramCounter = new AtomicInteger();
        AtomicInteger methodCounter = new AtomicInteger();

        // find constructor with @Inject, if none, find the first constructor (assume @Inject)
        TypedElementInfo constructor = injectConstructor(typeInfo);
        if (constructor != null) {
            injectConstructorParams(typeInfo, params, paramCounter, constructor);
        }

        // find all fields with @Inject
        typeInfo.elementInfo()
                .stream()
                .filter(it -> it.elementTypeKind() == ElementKind.FIELD)
                .filter(it -> it.hasAnnotation(TypeNames.JAKARTA_INJECT_TYPE))
                .forEach(it -> fieldParam(typeInfo, params, paramCounter, it));

        // find all methods with @Inject
        typeInfo.elementInfo()
                .stream()
                .filter(it -> it.elementTypeKind() == ElementKind.METHOD)
                .filter(it -> it.hasAnnotation(TypeNames.JAKARTA_INJECT_TYPE))
                .forEach(it -> methodParams(typeInfo, methods, params, methodCounter, paramCounter, it));

    }

    private void methodParams(TypeInfo service,
                              List<MethodDefinition> methods,
                              List<ParamDefinition> params,
                              AtomicInteger methodCounter,
                              AtomicInteger paramCounter,
                              TypedElementInfo method) {

        String methodConstantName = "METHOD_" + methodCounter.getAndIncrement();

        MethodDefinition methodDefinition = new MethodDefinition(methodConstantName,
                                                                 method.elementName(),
                                                                 method.parameterArguments()
                                                                         .stream()
                                                                         .map(TypedElementInfo::typeName)
                                                                         .toList());
        methods.add(methodDefinition);
        method.parameterArguments()
                .stream()
                .map(param -> new ParamDefinition("IP_PARAM_" + paramCounter.get(),
                                                  "ipParam" + paramCounter.getAndIncrement(),
                                                  param.typeName(),
                                                  ElementKind.METHOD,
                                                  method.elementName(),
                                                  param.elementName(),
                                                  methodConstantName,
                                                  method.modifiers().contains(Modifier.STATIC),
                                                  param.annotations(),
                                                  qualifiers(service, param)))
                .forEach(params::add);
    }

    private void fieldParam(TypeInfo service, List<ParamDefinition> result, AtomicInteger paramCounter, TypedElementInfo field) {
        result.add(new ParamDefinition("IP_PARAM_" + paramCounter.get(),
                                       "ipParam" + paramCounter.getAndIncrement(),
                                       field.typeName(),
                                       ElementKind.FIELD,
                                       field.elementName(),
                                       field.elementName(),
                                       null,
                                       field.modifiers().contains(Modifier.STATIC),
                                       field.annotations(),
                                       qualifiers(service, field)));
    }

    private Set<Qualifier> qualifiers(TypeInfo service, TypedElementInfo element) {
        Set<Qualifier> result = new LinkedHashSet<>();

        for (io.helidon.common.types.Annotation anno : element.annotations()) {
            if (service.hasMetaAnnotation(anno.typeName(), TypeNames.JAKARTA_QUALIFIER_TYPE)) {
                result.add(Qualifier.create(anno));
            }
        }

        // note: should qualifiers be inheritable? Right now we assume not to support the jsr-330 spec (see note above).
        return result;
    }

    private void injectConstructorParams(TypeInfo service,
                                         List<ParamDefinition> result,
                                         AtomicInteger paramCounter,
                                         TypedElementInfo constructor) {
        constructor.parameterArguments()
                .stream()
                .map(param -> new ParamDefinition("IP_PARAM_" + paramCounter.get(),
                                                  "ipParam" + paramCounter.getAndIncrement(),
                                                  param.typeName(),
                                                  ElementKind.CONSTRUCTOR,
                                                  constructor.elementName(),
                                                  param.elementName(),
                                                  null,
                                                  false,
                                                  param.annotations(),
                                                  qualifiers(service, param)))
                .forEach(result::add);
    }

    private TypedElementInfo injectConstructor(TypeInfo typeInfo) {
        // first @Inject
        Optional<TypedElementInfo> first = typeInfo.elementInfo()
                .stream()
                .filter(it -> it.elementTypeKind() == ElementKind.CONSTRUCTOR)
                .filter(it -> it.hasAnnotation(TypeNames.JAKARTA_INJECT_TYPE))
                .findFirst();
        if (first.isPresent()) {
            return first.get();
        }

        // first constructor
        return typeInfo.elementInfo()
                .stream()
                .filter(it -> it.elementTypeKind() == ElementKind.CONSTRUCTOR)
                .findFirst()
                .orElse(null);
    }

    private TypeName serviceDescriptorImplementsType(TypeName serviceType) {
        return TypeName.builder(TypeNames.SERVICE_DESCRIPTOR_TYPE)
                .addTypeArgument(serviceType)
                .build();
    }

    private record GenericTypeDeclaration(String constantName,
                                          TypeName typeName) {
    }

    private record MethodDefinition(String constantName,
                                    String name,
                                    List<TypeName> types) {
    }

    private record ParamDefinition(String constantName,
                                   String fieldName,
                                   TypeName type,
                                   ElementKind kind,
                                   String ipName,
                                   String ipParamName,
                                   String methodConstantName,
                                   boolean isStatic,
                                   List<io.helidon.common.types.Annotation> annotations,
                                   Set<Qualifier> qualifiers) {

    }

}
