package io.helidon.inject.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.helidon.common.GenericType;
import io.helidon.common.processor.CopyrightHandler;
import io.helidon.common.processor.GeneratedAnnotationHandler;
import io.helidon.common.processor.classmodel.Annotation;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.processor.classmodel.Javadoc;
import io.helidon.common.processor.classmodel.Method;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.processor.spi.HelidonProcessorExtension;
import io.helidon.inject.tools.TypeNames;

class InjectionProcessorExtension implements HelidonProcessorExtension {
    private static final TypeName DEPENDENCIES_RETURN_TYPE = TypeName.builder(io.helidon.common.types.TypeNames.LIST)
            .addTypeArgument(TypeName.builder(TypeName.create("io.helidon.inject.api.InjectionContext.InjectionParameterId"))
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
            Map<TypeName, GenericTypeDeclaration> genericTypes = genericTypes(params, methods);

            // declare the class
            ClassModel.Builder classModel = ClassModel.builder()
                    .copyright(CopyrightHandler.copyright(GENERATOR,
                                                          serviceType,
                                                          descriptorType))
                    .addAnnotation(annot -> {
                        var generated = GeneratedAnnotationHandler.create(GENERATOR,
                                                                          serviceType,
                                                                          descriptorType,
                                                                          "1",
                                                                          "");
                        annot.type(generated.typeName());
                        generated.values()
                                .forEach(annot::addParameter);
                    })
                    .javadoc(Javadoc.builder()
                                     .add("Service descriptor for {@link " + serviceType.fqName() + "}.")
                                     .build())
                    .type(descriptorType)
                    .addInterface(serviceDescriptorImplementsType(serviceType));

            // singleton instance of the descriptor
            classModel.addField(instance -> instance.description("Global singleton instance for this descriptor.")
                    .accessModifier(AccessModifier.PUBLIC)
                    .isStatic(true)
                    .isFinal(true)
                    .type(descriptorType)
                    .name("INSTANCE")
                    .defaultValueContent("new " + descriptorType.className() + "();"));

            // constants for injection point parameter types
            genericTypes.forEach((typeName, generic) -> {
                classModel.addField(field -> field.accessModifier(AccessModifier.PRIVATE)
                        .isStatic(true)
                        .isFinal(true)
                        .type(genericType(typeName))
                        .name(generic.constantName())
                        .defaultValueContent("new @io.helidon.common.GenericType@<" + typeName.resolvedName() + ">() {}"));
            });

            // constants for methods
            for (MethodDefinition method : methods) {
                classModel.addField(field -> field.accessModifier(AccessModifier.PRIVATE)
                        .isStatic(true)
                        .isFinal(true)
                        .type(TypeName.create("io.helidon.inject.api.InjectionContext.MethodId"))
                        .name(method.constantName())
                        .defaultValueContent("new @io.helidon.inject.api.InjectionContext.MethodId@("
                                                     + method.name()
                                                     + method.types()
                                .stream()
                                .map(it -> genericTypes.get(it).constantName())
                                .collect(Collectors.joining(", "))
                                                     + ")"));
            }

            // constant for injection points
            for (ParamDefinition param : params) {
                classModel.addField(field -> field.accessModifier(AccessModifier.PRIVATE)
                        .isStatic(true)
                        .isFinal(true)
                        .type(ipId(param.type()))
                        .name(param.constantName())
                        .defaultValueContent("new @io.helidon.inject.api.InjectionContext.InjectionParameterId@("
                                                     + "@io.helidon.common.types.ElementKind@." + param.kind.name() + ", "
                                                     + "\"" + param.ipName() + "\", "
                                                     + "\"" + param.ipParamName() + "\", "
                                                     + genericTypes.get(param.type()).constantName() + ", "
                                                     + param.methodConstantName() + ")"));
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

            // T create(InjectionContext ctx)
            classModel.addMethod(method -> method.addAnnotation(OVERRIDE)
                    .returnType(serviceType)
                    .name("create")
                    .addParameter(ctxParam -> ctxParam.type(TypeNames.INJECTION_CONTEXT)
                            .name("ctx"))
                    .update(it -> createMethodBody(serviceType, it, params)));

            // postConstruct()
            // preDestroy

            ctx.addServiceDescriptor(serviceType,
                                     descriptorType,
                                     classModel);
        }
        return false;
    }

    private void createDependenciesBody(Method.Builder method, List<ParamDefinition> params) {
        method.addLine("return @java.util.List@.of(" + params.stream()
                .map(ParamDefinition::constantName)
                .collect(Collectors.joining(", "))
                               + ");");
    }

    private Map<TypeName, GenericTypeDeclaration> genericTypes(List<ParamDefinition> params, List<MethodDefinition> methods) {
        Map<TypeName, GenericTypeDeclaration> result = new HashMap<>();
        AtomicInteger counter = new AtomicInteger();

        for (ParamDefinition param : params) {
            result.computeIfAbsent(param.type(), type -> new GenericTypeDeclaration("TYPE_" + counter.getAndIncrement(),
                                                                                    type));
        }

        for (MethodDefinition method : methods) {
            method.types()
                    .forEach(it -> result.computeIfAbsent(it,
                                                          type -> new GenericTypeDeclaration("TYPE_" + counter.getAndIncrement(),
                                                                                             type)));
        }

        return result;
    }

    private void createMethodBody(TypeName serviceType, Method.Builder method, List<ParamDefinition> params) {
        /*
            var ipParam1_serviceProviders = ctx.param(IP_PARAM_1);
            var ipParam2_someOtherName = ctx.param(IP_PARAM_2);
            var result = new ConfigProducer(ipParam1_serviceProviders, someOtherName);
            result.setter(...);
            result.setter(...);
            result.field = ...;
         */
        List<ParamDefinition> constructorParams = new ArrayList<>();
        Map<String, List<ParamDefinition>> methodCalls = new HashMap<>();
        List<ParamDefinition> fields = new ArrayList<>();

        for (ParamDefinition param : params) {
            switch (param.kind()) {
            case CONSTRUCTOR -> constructorParams.add(param);
            case FIELD -> fields.add(param);
            case METHOD -> methodCalls.computeIfAbsent(param.methodConstantName(), it -> new ArrayList<>()).add(param);
            }
        }

        // for each parameter, obtain its value from context
        for (ParamDefinition param : params) {
            method.addLine("var " + param.fieldName() + "_" + param.ipParamName() + " = ctx.param(" + param.constantName() + ")"
                                   + ";");
        }

        method.addLine("var instance = new " + serviceType.classNameWithEnclosingNames() + "("
                               + constructorParams.stream()
                .map(param -> param.fieldName() + "_" + param.ipParamName())
                .collect(Collectors.joining(", "))
                               + ");");

        for (ParamDefinition field : fields) {
            method.addLine("instance." + field.ipName() + " = " + field.fieldName() + "_" + field.ipParamName() + ";");
        }

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

        method.addLine("return instance;");
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

    /*
    GenericType<List<Provider<ConfigSource>> IP_PARAM_1_TYPE = new GenericType<List<Provider<ConfigSource>> {};


    MethodId METHOD_1 = new MethodId("someMethod", IP_PARAM_1_TYPE);
    InjectionContext.InjectionParameterId<List<Provider<ConfigSource>> IP_PARAM_1 = new InjectionContext.InjectionParameterId


    (ElementKind.CONSTRUCTOR, "<init>", "serviceProviders", IP_PARAM_1_TYPE);

    constructor(1, 2);
    setter(3)
    setter(4)
    field(5)
     */

    private void params(TypeInfo typeInfo,
                        List<MethodDefinition> methods, List<ParamDefinition> params) {
        AtomicInteger paramCounter = new AtomicInteger();
        AtomicInteger methodCounter = new AtomicInteger();

        // find constructor with @Inject, if none, find the first constructor (assume @Inject)
        TypedElementInfo constructor = injectConstructor(typeInfo);
        if (constructor != null) {
            injectConstructorParams(params, paramCounter, constructor);
        }

        // find all fields with @Inject
        typeInfo.elementInfo()
                .stream()
                .filter(it -> it.elementTypeKind() == ElementKind.FIELD)
                .filter(it -> it.hasAnnotation(TypeNames.JAKARTA_INJECT_TYPE))
                .forEach(it -> fieldParam(params, paramCounter, it));

        // find all methods with @Inject
        typeInfo.elementInfo()
                .stream()
                .filter(it -> it.elementTypeKind() == ElementKind.METHOD)
                .filter(it -> it.hasAnnotation(TypeNames.JAKARTA_INJECT_TYPE))
                .forEach(it -> methodParams(methods, params, methodCounter, paramCounter, it));

    }

    private void methodParams(List<MethodDefinition> methods,
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
                                                  methodConstantName))
                .forEach(params::add);
    }

    private void fieldParam(List<ParamDefinition> result, AtomicInteger paramCounter, TypedElementInfo field) {
        result.add(new ParamDefinition("IP_PARAM_" + paramCounter.get(),
                                       "ipParam" + paramCounter.getAndIncrement(),
                                       field.typeName(),
                                       ElementKind.FIELD,
                                       field.elementName(),
                                       field.elementName(),
                                       null));
    }

    private void injectConstructorParams(List<ParamDefinition> result,
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
                                                  null))
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
                                   String methodConstantName) {

    }

}
