package io.helidon.inject.processor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.Weight;
import io.helidon.common.codegen.TypesCodeGen;
import io.helidon.common.processor.CopyrightHandler;
import io.helidon.common.processor.ElementInfoPredicates;
import io.helidon.common.processor.GeneratedAnnotationHandler;
import io.helidon.common.processor.TypeInfoFactory;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.processor.classmodel.Method;
import io.helidon.common.processor.classmodel.TypeArgument;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotated;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.Modifier;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.api.InterceptionStrategy;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.processor.spi.HelidonProcessorExtension;
import io.helidon.inject.processor.spi.InjectionAnnotationProcessorObserver;
import io.helidon.inject.tools.Options;
import io.helidon.inject.tools.ToolsException;
import io.helidon.inject.tools.TypeNames;

class InjectionProcessorExtension implements HelidonProcessorExtension {
    static final TypeName LIST_OF_ANNOTATIONS_TYPE = TypeName.builder(io.helidon.common.types.TypeNames.LIST)
            .addTypeArgument(TypeName.create(Annotation.class))
            .build();
    static final TypeName SET_OF_QUALIFIERS_TYPE = TypeName.builder(io.helidon.common.types.TypeNames.SET)
            .addTypeArgument(TypeNames.HELIDON_QUALIFIER)
            .build();
    private static final TypeName SERVICE_DEPS = TypeName.create("io.helidon.inject.api.ServiceDependencies");
    private static final TypeName DEPENDENCIES_RETURN_TYPE = TypeName.builder(io.helidon.common.types.TypeNames.LIST)
            .addTypeArgument(SERVICE_DEPS)
            .build();
    private static final TypeName CONTRACTS_RETURN_TYPE = TypeName.builder(io.helidon.common.types.TypeNames.SET)
            .addTypeArgument(io.helidon.common.types.TypeNames.TYPE_NAME)
            .build();

    private static final TypeName SERVICE_SOURCE_TYPE = TypeName.builder(TypeNames.SERVICE_SOURCE_TYPE)
            .addTypeArgument(TypeName.create("T"))
            .build();
    private static final TypeName GENERATOR = TypeName.create(InjectionProcessorExtension.class);
    private static final Annotation RUNTIME_RETENTION = Annotation.create(Retention.class, RetentionPolicy.RUNTIME.name());
    private static final Annotation CLASS_RETENTION = Annotation.create(Retention.class, RetentionPolicy.CLASS.name());
    private static final TypedElementInfo DEFAULT_CONSTRUCTOR = TypedElementInfo.builder()
            .typeName(io.helidon.common.types.TypeNames.OBJECT)
            .accessModifier(AccessModifier.PUBLIC)
            .elementTypeKind(ElementKind.CONSTRUCTOR)
            .build();

    private final InjectionProcessingContext ctx;
    private final boolean autoAddIfaces;
    private final InterceptionStrategy interceptionStrategy;

    InjectionProcessorExtension(InjectionProcessingContext ctx) {
        this.ctx = ctx;
        this.autoAddIfaces = ctx.ctx().options().option(Options.TAG_AUTO_ADD_NON_CONTRACT_INTERFACES, false);
        this.interceptionStrategy = ctx.ctx().options()
                .option(Options.TAG_INTERCEPTION_STRATEGY, InterceptionStrategy.EXPLICIT, InterceptionStrategy.class);
    }

    static String toConstantName(String elementName) {
        /*
        Method name is camel case (such as maxInitialLineLength)
        result is constant like (such as MAX_INITIAL_LINE_LENGTH).
        */
        StringBuilder result = new StringBuilder();

        char[] chars = elementName.toCharArray();
        for (char aChar : chars) {
            if (Character.isUpperCase(aChar)) {
                if (result.isEmpty()) {
                    result.append(aChar);
                } else {
                    result.append('_')
                            .append(Character.toLowerCase(aChar));
                }
            } else {
                result.append(aChar);
            }
        }

        return result.toString().toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean process(RoundContext roundContext) {
        Collection<TypeInfo> descriptorsRequired = roundContext.types();

        for (TypeInfo typeInfo : descriptorsRequired) {
            if (typeInfo.typeKind() == ElementKind.INTERFACE) {
                // we cannot support multiple inheritance, so descriptors for interfaces do not make sense
                continue;
            }
            boolean isAbstractClass = typeInfo.modifiers().contains(Modifier.ABSTRACT)
                    && typeInfo.typeKind() == ElementKind.CLASS;
            TypeName superType = superType(typeInfo, descriptorsRequired);
            boolean hasSuperType = superType != null;

            // this set now contains all fields, constructors, and methods that may be intercepted, as they contain
            // an annotation that is an interception trigger (based on interceptionStrategy)
            Set<TypedElementInfo> maybeIntercepted = maybeIntercepted(typeInfo);
            boolean canIntercept = !maybeIntercepted.isEmpty();
            boolean methodsIntercepted = maybeIntercepted.stream()
                    .anyMatch(ElementInfoPredicates::isMethod);

            TypeName serviceType = typeInfo.typeName();
            // this must result in generating a service descriptor file
            TypeName descriptorType = ctx.serviceDescriptorType(serviceType);

            List<ParamDefinition> params = new ArrayList<>();
            List<MethodDefinition> methods = new ArrayList<>();

            TypedElementInfo constructorInjectElement = injectConstructor(typeInfo);
            List<TypedElementInfo> fieldInjectElements = fieldInjectElements(typeInfo);
            List<TypedElementInfo> methodInjectElements = methodInjectElements(typeInfo);

            boolean constructorIntercepted = maybeIntercepted.contains(constructorInjectElement);

            params(typeInfo,
                   methods,
                   params,
                   constructorInjectElement,
                   fieldInjectElements,
                   methodInjectElements);

            Map<String, GenericTypeDeclaration> genericTypes = genericTypes(params, methods);
            Set<Qualifier> qualifiers = qualifiers(typeInfo, typeInfo);
            Set<TypeName> contracts = new HashSet<>();
            contracts(contracts, typeInfo, autoAddIfaces);

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
                    .addGenericArgument(TypeArgument.create("T extends " + serviceType.fqName()))
                    // we need to keep insertion order, as constants may depend on each other
                    .sortStaticFields(false);

            if (hasSuperType) {
                classModel.superType(superType);
            } else {
                classModel.addInterface(SERVICE_SOURCE_TYPE);
            }

            /*
            Fields
             */
            singletonInstanceField(classModel, descriptorType);
            typeFields(classModel, genericTypes);
            injectionPointIdFields(classModel, genericTypes, params);
            injectionPointInfoFields(classModel, genericTypes, params);
            serviceTypeFields(classModel, serviceType, descriptorType);
            contractsField(classModel, contracts);
            dependenciesField(classModel, params);
            qualifiersField(classModel, qualifiers);
            if (canIntercept) {
                annotationsField(classModel, typeInfo);
                // if constructor intercepted, add its element
                if (constructorIntercepted) {
                    constructorElementField(classModel, constructorInjectElement);
                }
                // if injected field intercepted, add its element (other fields cannot be intercepted)
                fieldInjectElements.stream()
                        .filter(maybeIntercepted::contains)
                        .forEach(fieldElement -> fieldElementField(classModel, fieldElement));
                // all other interception is done on method level and is handled by the
                // service descriptor delegating to a generated type
            }

            /*
            Constructor
             */
            // add protected constructor
            classModel.addConstructor(constructor -> constructor.description("Constructor with no side effects")
                    .accessModifier(AccessModifier.PROTECTED));

            /*
            Methods
             */
            serviceTypeMethod(classModel);
            descriptorTypeMethod(classModel);
            contractsMethod(classModel, contracts);
            dependenciesMethod(classModel, params, hasSuperType);
            instantiateMethod(classModel, serviceType, params, isAbstractClass, constructorIntercepted, methodsIntercepted);
            injectFieldsMethod(classModel, serviceType, params, hasSuperType, canIntercept, maybeIntercepted);
            injectMethodsMethod(classModel, serviceType, params, hasSuperType);
            postConstructMethod(typeInfo, classModel, serviceType);
            preDestroyMethod(typeInfo, classModel, serviceType);
            qualifiersMethod(classModel, qualifiers);
            weightMethod(typeInfo, classModel);
            runLevelMethod(typeInfo, classModel);

            ctx.addServiceDescriptor(serviceType,
                                     descriptorType,
                                     classModel);

            if (methodsIntercepted) {
                TypeName interceptedType = TypeName.builder(serviceType)
                        .className(serviceType.classNameWithEnclosingNames().replace('.', '_') + "__Intercepted")
                        .build();

                var generator = new InterceptedTypeGenerator(serviceType,
                                                             descriptorType,
                                                             interceptedType,
                                                             constructorInjectElement,
                                                             maybeIntercepted.stream()
                                                                     .filter(ElementInfoPredicates::isMethod)
                                                                     .toList());

                ctx.addClass(serviceType, interceptedType, generator.generate());
            }
        }

        notifyObservers(descriptorsRequired);

        return false;
    }


    private void notifyObservers(Collection<TypeInfo> descriptorsRequired) {
        // we have correct classloader set in current thread context
        List<InjectionAnnotationProcessorObserver> observers = HelidonServiceLoader.create(ServiceLoader.load(
                        InjectionAnnotationProcessorObserver.class))
                .asList();
        if (!observers.isEmpty()) {
            ProcessingEvent event = ProcessingEvent.builder()
                    .processingEnvironment(ctx.ctx().aptEnv())
                    .elementsOfInterest(descriptorsRequired.stream()
                                                .flatMap(it -> it.elementInfo().stream())
                                                .collect(Collectors.toSet()))
                    .build();
            observers.forEach(it -> it.onProcessingEvent(event));
        }
    }

    private void runLevelMethod(TypeInfo typeInfo, ClassModel.Builder classModel) {
        // int runLevel()
        Optional<Integer> runLevel = runLevel(typeInfo);
        runLevel.ifPresent(integer -> classModel.addMethod(runLevelMethod -> runLevelMethod.name("runLevel")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(io.helidon.common.types.TypeNames.PRIMITIVE_INT)
                .addLine("return " + integer + ";")));
    }

    private void weightMethod(TypeInfo typeInfo, ClassModel.Builder classModel) {
        // double weight()
        Optional<Double> weight = weight(typeInfo);
        weight.ifPresent(aDouble -> classModel.addMethod(weightMethod -> weightMethod.name("weight")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(io.helidon.common.types.TypeNames.PRIMITIVE_DOUBLE)
                .addLine("return " + aDouble + ";")));
    }

    private void qualifiersMethod(ClassModel.Builder classModel, Set<Qualifier> qualifiers) {
        if (qualifiers.isEmpty()) {
            return;
        }
        // List<Qualifier> qualifiers()
        classModel.addMethod(qualifiersMethod -> qualifiersMethod.name("qualifiers")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(SET_OF_QUALIFIERS_TYPE)
                .addLine("return QUALIFIERS;"));
    }

    private void preDestroyMethod(TypeInfo typeInfo, ClassModel.Builder classModel, TypeName serviceType) {
        // preDestroy
        lifecycleMethod(typeInfo, TypeNames.JAKARTA_PRE_DESTROY_TYPE).ifPresent(method -> {
            classModel.addMethod(preDestroy -> preDestroy.name("preDestroy")
                    .addAnnotation(Annotations.OVERRIDE)
                    .addParameter(instance -> instance.type(serviceType)
                            .name("instance"))
                    .addLine("instance." + method.elementName() + "();"));
        });
    }

    private void postConstructMethod(TypeInfo typeInfo, ClassModel.Builder classModel, TypeName serviceType) {
        // postConstruct()
        lifecycleMethod(typeInfo, TypeNames.JAKARTA_POST_CONSTRUCT_TYPE).ifPresent(method -> {
            classModel.addMethod(postConstruct -> postConstruct.name("postConstruct")
                    .addAnnotation(Annotations.OVERRIDE)
                    .addParameter(instance -> instance.type(serviceType)
                            .name("instance"))
                    .addLine("instance." + method.elementName() + "();"));
        });
    }

    private void injectMethodsMethod(ClassModel.Builder classModel,
                                     TypeName serviceType,
                                     List<ParamDefinition> params,
                                     boolean hasSuperType) {
        // as these methods are called on an instance, interception is done through that instance, and is ignored here

        // T injectMethods(InjectionContext ctx, T instance)
        Map<String, List<ParamDefinition>> methodCalls = new LinkedHashMap<>();

        params.stream()
                .filter(it -> it.kind == ElementKind.METHOD)
                .forEach(param -> methodCalls.computeIfAbsent(param.methodId(), it -> new ArrayList<>())
                        .add(param));
        if (!methodCalls.isEmpty()) {
            classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                    .name("injectMethods")
                    .addParameter(ctxParam -> ctxParam.type(TypeNames.INJECTION_CONTEXT)
                            .name("ctx"))
                    .addParameter(instanceParam -> instanceParam.type(serviceType)
                            .name("instance"))
                    .update(it -> createInjectMethodsBody(hasSuperType, methodCalls, params, it)));
        }
    }

    private void injectFieldsMethod(ClassModel.Builder classModel,
                                    TypeName serviceType,
                                    List<ParamDefinition> params,
                                    boolean hasSuperType,
                                    boolean canIntercept,
                                    Set<TypedElementInfo> maybeIntercepted) {
        // T injectFields(InjectionContext ctx, T instance)
        List<ParamDefinition> fields = params.stream()
                .filter(it -> it.kind == ElementKind.FIELD)
                .toList();
        if (!fields.isEmpty()) {
            classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                    .name("injectFields")
                    .addParameter(ctxParam -> ctxParam.type(TypeNames.INJECTION_CONTEXT)
                            .name("ctx"))
                    .addParameter(interceptMeta -> interceptMeta.type(TypeNames.INTERCEPTION_METADATA)
                            .name("interceptMeta"))
                    .addParameter(instanceParam -> instanceParam.type(serviceType)
                            .name("instance"))
                    .update(it -> createInjectFieldsBody(hasSuperType, fields, it, canIntercept, maybeIntercepted)));
        }
    }

    private void instantiateMethod(ClassModel.Builder classModel,
                                   TypeName serviceType,
                                   List<ParamDefinition> params,
                                   boolean isAbstractClass,
                                   boolean constructorIntercepted,
                                   boolean interceptedMethods) {
        if (isAbstractClass) {
            return;
        }

        // T instantiate(InjectionContext ctx)
        TypeName toInstantiate = interceptedMethods
                ? TypeName.builder(serviceType).className(serviceType.className() + "__Intercepted").build()
                : serviceType;

        classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                .returnType(serviceType)
                .name("instantiate")
                .addParameter(ctxParam -> ctxParam.type(TypeNames.INJECTION_CONTEXT)
                        .name("ctx"))
                .addParameter(interceptMeta -> interceptMeta.type(TypeNames.INTERCEPTION_METADATA)
                        .name("interceptMeta"))
                .update(it -> {
                    if (constructorIntercepted) {
                        createInstantiateInterceptBody(it, params);
                    } else {
                        createInstantiateBody(toInstantiate, it, params, interceptedMethods);
                    }
                }));

        if (constructorIntercepted) {
            classModel.addMethod(method -> method.returnType(serviceType)
                    .name("doInstantiate")
                    .accessModifier(AccessModifier.PRIVATE)
                    .addParameter(interceptMeta -> interceptMeta.type(TypeNames.INTERCEPTION_METADATA)
                            .name("interceptMeta"))
                    .addParameter(ctrParams -> ctrParams.type(TypeName.create("Object..."))
                            .name("params"))
                    .update(it -> createDoInstantiateBody(toInstantiate, it, params, interceptedMethods)));
        }
    }

    private void dependenciesMethod(ClassModel.Builder classModel, List<ParamDefinition> params, boolean hasSuperType) {
        // List<InjectionParameterId> dependencies()

        if (hasSuperType || !params.isEmpty()) {
            classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                    .returnType(DEPENDENCIES_RETURN_TYPE)
                    .name("dependencies")
                    .update(it -> {
                        if (hasSuperType) {
                            it.addLine("return combineDependencies(DEPENDENCIES, super.dependencies());");
                        } else {
                            it.addLine("return @java.util.List@.of(DEPENDENCIES);");
                        }
                    }));
        }
    }

    private void contractsMethod(ClassModel.Builder classModel, Set<TypeName> contracts) {
        if (contracts.isEmpty()) {
            return;
        }
        // Set<Class<?>> contracts()
        classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                .name("contracts")
                .returnType(CONTRACTS_RETURN_TYPE)
                .addLine("return CONTRACTS;"));
    }

    private void descriptorTypeMethod(ClassModel.Builder classModel) {
        // TypeName descriptorType()
        classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                .returnType(io.helidon.common.types.TypeNames.TYPE_NAME)
                .name("descriptorType")
                .addLine("return DESCRIPTOR_TYPE;"));
    }

    private void serviceTypeMethod(ClassModel.Builder classModel) {
        // TypeName serviceType()
        classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                .returnType(io.helidon.common.types.TypeNames.TYPE_NAME)
                .name("serviceType")
                .addLine("return TYPE_NAME;"));
    }

    private List<TypedElementInfo> methodInjectElements(TypeInfo typeInfo) {
        return typeInfo.elementInfo()
                .stream()
                .filter(it -> it.elementTypeKind() == ElementKind.METHOD)
                .filter(it -> it.hasAnnotation(TypeNames.JAKARTA_INJECT_TYPE))
                .toList();
    }

    private List<TypedElementInfo> fieldInjectElements(TypeInfo typeInfo) {
        return typeInfo.elementInfo()
                .stream()
                .filter(it -> it.elementTypeKind() == ElementKind.FIELD)
                .filter(it -> it.hasAnnotation(TypeNames.JAKARTA_INJECT_TYPE))
                .toList();
    }

    private void constructorElementField(ClassModel.Builder classModel, TypedElementInfo constructorInjectElement) {
        classModel.addField(ctorElement -> ctorElement
                .isStatic(true)
                .isFinal(true)
                .name("CTOR_ELEMENT")
                .type(io.helidon.common.types.TypeNames.TYPED_ELEMENT_INFO)
                .defaultValueContent(TypesCodeGen.toCreate(constructorInjectElement)));
    }

    private void fieldElementField(ClassModel.Builder classModel, TypedElementInfo fieldElement) {
        classModel.addField(ctorElement -> ctorElement
                .isStatic(true)
                .isFinal(true)
                .name(fieldElementConstantName(fieldElement.elementName()))
                .type(io.helidon.common.types.TypeNames.TYPED_ELEMENT_INFO)
                .defaultValueContent(TypesCodeGen.toCreate(fieldElement)));
    }

    private void annotationsField(ClassModel.Builder classModel, TypeInfo typeInfo) {
        classModel.addField(annotations -> annotations
                .isStatic(true)
                .isFinal(true)
                .name("ANNOTATIONS")
                .type(LIST_OF_ANNOTATIONS_TYPE)
                .defaultValueContent("@java.util.List@.of(" + typeInfo.annotations()
                        .stream()
                        .map(TypesCodeGen::toCreate)
                        .collect(Collectors.joining(", "))
                                             + ")"));
    }

    private void qualifiersField(ClassModel.Builder classModel, Set<Qualifier> qualifiers) {
        classModel.addField(qualifiersField -> qualifiersField
                .isStatic(true)
                .isFinal(true)
                .name("QUALIFIERS")
                .type(SET_OF_QUALIFIERS_TYPE)
                .defaultValueContent("@java.util.Set@.of(" + qualifiers.stream()
                        .map(this::codeGenQualifier)
                        .collect(Collectors.joining(", "))
                                             + ")"));
    }

    private void dependenciesField(ClassModel.Builder classModel, List<ParamDefinition> params) {
        classModel.addField(dependencies -> dependencies
                .isStatic(true)
                .isFinal(true)
                .name("DEPENDENCIES")
                .type(SERVICE_DEPS)
                .defaultValueContent("new @" + SERVICE_DEPS.fqName() + "@(TYPE_NAME, @java.util.List@.of(" + params.stream()
                        .map(ParamDefinition::constantName)
                        .map(it -> it + "_INFO")
                        .collect(Collectors.joining(", "))
                                             + "))"));
    }

    private void contractsField(ClassModel.Builder classModel, Set<TypeName> contracts) {
        if (contracts.isEmpty()) {
            return;
        }
        classModel.addField(contractsField -> contractsField
                .isStatic(true)
                .isFinal(true)
                .name("CONTRACTS")
                .type(CONTRACTS_RETURN_TYPE)
                .defaultValueContent("@java.util.Set@.of(" + contracts.stream()
                        .map(it -> "@io.helidon.common.types.TypeName@.create(@" + it.fqName() + "@.class)")
                        .collect(Collectors.joining(", "))
                                             + ")"));
    }

    private void injectionPointIdFields(ClassModel.Builder classModel,
                                        Map<String, GenericTypeDeclaration> genericTypes,
                                        List<ParamDefinition> params) {
        // constant for injection points
        for (ParamDefinition param : params) {
            classModel.addField(field -> field
                    .accessModifier(AccessModifier.PUBLIC)
                    .isStatic(true)
                    .isFinal(true)
                    .type(ipId(param.type()))
                    .name(param.constantName())
                    .defaultValueContent("@io.helidon.inject.api.IpId@.<"
                                                 + param.type.resolvedName()
                                                 + ">builder()"
                                                 + ".elementKind(@io.helidon.common.types.ElementKind@."
                                                 + param.kind.name() + ")"
                                                 + ".name(\""
                                                 + param.fieldId() + "\")"
                                                 + ".typeName("
                                                 + genericTypes.get(param.type().resolvedName()).constantName() + ")"
                                                 + ".build()"));
        }
    }

    private void injectionPointInfoFields(ClassModel.Builder classModel,
                                          Map<String, GenericTypeDeclaration> genericTypes,
                                          List<ParamDefinition> params) {
        // constant for injection points
        for (ParamDefinition param : params) {
            classModel.addField(field -> {
                field.accessModifier(AccessModifier.PRIVATE)
                        .isStatic(true)
                        .isFinal(true)
                        .type(TypeNames.INJECTION_PARAMETER_INFO)
                        .name(param.constantName() + "_INFO");
                StringBuilder defaultContent = new StringBuilder();
                defaultContent.append("@io.helidon.inject.api.IpInfo@.builder()")
                        .append(".id(")
                        .append(param.constantName())
                        .append(")")
                        .append(".field(\"")
                        .append(param.constantName())
                        .append("\")")
                        .append(".contract(")
                        .append(genericTypes.get(param.type().resolvedName()).constantName()).append(")");

                if (param.access() != AccessModifier.PACKAGE_PRIVATE) {
                    defaultContent.append(".access(@io.helidon.common.types.AccessModifier@.")
                            .append(param.access())
                            .append(")");
                }

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
    }

    private void typeFields(ClassModel.Builder classModel, Map<String, GenericTypeDeclaration> genericTypes) {
        // constants for injection point parameter types (used by next section)
        genericTypes.forEach((typeName, generic) -> classModel.addField(field -> field.accessModifier(AccessModifier.PRIVATE)
                .isStatic(true)
                .isFinal(true)
                .type(io.helidon.common.types.TypeNames.TYPE_NAME)
                .name(generic.constantName())
                .update(it -> {
                    if (typeName.indexOf('.') < 0) {
                        // there is no package, we must use class (if this is a generic type, we have a problem)
                        it.defaultValueContent("@io.helidon.common.types.TypeName@.create(" + typeName + ".class)");
                    } else {
                        it.defaultValueContent("@io.helidon.common.types.TypeName@.create(\"" + typeName + "\")");
                    }
                })));
    }

    private void serviceTypeFields(ClassModel.Builder classModel, TypeName serviceType, TypeName descriptorType) {
        classModel.addField(field -> field
                .isStatic(true)
                .isFinal(true)
                .accessModifier(AccessModifier.PRIVATE)
                .type(io.helidon.common.types.TypeNames.TYPE_NAME)
                .name("TYPE_NAME")
                .defaultValueContent("@" + TypeName.class.getName() + "@.create("
                                             + serviceType.classNameWithEnclosingNames() + ".class)"));

        classModel.addField(field -> field
                .isStatic(true)
                .isFinal(true)
                .accessModifier(AccessModifier.PRIVATE)
                .type(io.helidon.common.types.TypeNames.TYPE_NAME)
                .name("DESCRIPTOR_TYPE")
                .defaultValueContent("@" + TypeName.class.getName() + "@.create("
                                             + descriptorType.classNameWithEnclosingNames() + ".class)"));
    }

    private void singletonInstanceField(ClassModel.Builder classModel, TypeName descriptorType) {
        // singleton instance of the descriptor
        classModel.addField(instance -> instance.description("Global singleton instance for this descriptor.")
                .accessModifier(AccessModifier.PUBLIC)
                .isStatic(true)
                .isFinal(true)
                .type(descriptorType)
                .name("INSTANCE")
                .defaultValueContent("new " + descriptorType.className() + "()"));
    }

    private Set<TypedElementInfo> maybeIntercepted(TypeInfo typeInfo) {
        if (interceptionStrategy == InterceptionStrategy.NONE) {
            return Set.of();
        }

        Set<TypedElementInfo> result = Collections.newSetFromMap(new IdentityHashMap<>());

        // depending on strategy
        if (hasInterceptTrigger(typeInfo, typeInfo)) {
            // we cannot intercept private stuff (never modify source code or bytecode!)
            result.addAll(typeInfo.elementInfo()
                                  .stream()
                                  .filter(it -> it.accessModifier() != AccessModifier.PRIVATE)
                                  .toList());
            result.add(DEFAULT_CONSTRUCTOR); // we must intercept construction as well
        } else {
            result.addAll(typeInfo.elementInfo().stream()
                                  .filter(elementInfo -> hasInterceptTrigger(typeInfo, elementInfo))
                                  .peek(it -> {
                                      if (it.accessModifier() == AccessModifier.PRIVATE) {
                                          throw new ToolsException(typeInfo.typeName()
                                                                           .fqName() + "#" + it.elementName() + " is declared "
                                                                           + "as private, but has interceptor trigger "
                                                                           + "annotation declared. "
                                                                           + "This cannot be supported, as we do not modify "
                                                                           + "sources or bytecode.");
                                      }
                                  })
                                  .toList());
        }

        return result;
    }

    private boolean hasInterceptTrigger(TypeInfo typeInfo, Annotated element) {
        for (io.helidon.common.types.Annotation annotation : element.annotations()) {
            if (interceptionStrategy.ordinal() >= InterceptionStrategy.EXPLICIT.ordinal()) {
                if (typeInfo.hasMetaAnnotation(annotation.typeName(), TypeNames.INTERCEPTED_TRIGGER)) {
                    return true;
                }
            }
            if (interceptionStrategy.ordinal() >= InterceptionStrategy.ALL_RUNTIME.ordinal()) {
                Optional<io.helidon.common.types.Annotation> retention = typeInfo.metaAnnotation(annotation.typeName(),
                                                                                                 TypeNames.ANNOTATION_RETENTION);
                boolean isRuntime = retention.map(RUNTIME_RETENTION::equals).orElse(false);
                if (isRuntime) {
                    return true;
                }
            }
            if (interceptionStrategy.ordinal() >= InterceptionStrategy.ALL_RETAINED.ordinal()) {
                Optional<io.helidon.common.types.Annotation> retention = typeInfo.metaAnnotation(annotation.typeName(),
                                                                                                 TypeNames.ANNOTATION_RETENTION);
                boolean isClass = retention.map(CLASS_RETENTION::equals).orElse(false);
                if (isClass) {
                    return true;
                }
            }
        }
        return false;
    }

    private TypeName superType(TypeInfo typeInfo, Collection<TypeInfo> descriptors) {
        // find super type if it is also a service (or has a service descriptor)

        // check if the super type is part of current annotation processing
        Optional<TypeInfo> superTypeInfoOptional = typeInfo.superTypeInfo();
        if (superTypeInfoOptional.isEmpty()) {
            return null;
        }
        TypeInfo superType = superTypeInfoOptional.get();
        TypeName expectedSuperDescriptor = ctx.serviceDescriptorType(superType.typeName());
        TypeName superTypeToExtend = TypeName.builder(expectedSuperDescriptor)
                .addTypeArgument(TypeName.create("T"))
                .build();
        for (TypeInfo descriptor : descriptors) {
            if (descriptor.typeName().equals(superType.typeName())) {
                return superTypeToExtend;
            }
        }
        // if not found in current list, try checking existing types
        return TypeInfoFactory.create(ctx.ctx(), expectedSuperDescriptor)
                .map(it -> superTypeToExtend)
                .orElse(null);
    }

    private void contracts(Set<TypeName> collectedContracts, TypeInfo typeInfo, boolean contractEligible) {
        TypeName typeName = typeInfo.typeName();

        if (contractEligible) {
            collectedContracts.add(typeName);
        }

        if (typeName.equals(TypeNames.JAKARTA_PROVIDER_TYPE)
                || typeName.equals(TypeNames.INJECTION_POINT_PROVIDER_TYPE)
                || typeName.equals(TypeNames.SERVICE_PROVIDER_TYPE)) {
            // provider must have a type argument (and the type argument is an automatic contract
            TypeName providedType = typeName.typeArguments().getFirst();
            if (!providedType.generic()) {
                collectedContracts.add(providedType);
            }
            // provider itself is a contract
            collectedContracts.add(typeName);
        }

        // add contracts from interfaces and types annotated as @Contract
        typeInfo.findAnnotation(TypeNames.CONTRACT_TYPE)
                .ifPresent(it -> collectedContracts.add(typeInfo.typeName()));

        // add contracts from @ExternalContracts
        typeInfo.findAnnotation(TypeNames.EXTERNAL_CONTRACTS_TYPE)
                .ifPresent(it -> collectedContracts.addAll(it.typeValues().orElseGet(List::of)));

        // go through hierarchy
        typeInfo.superTypeInfo().ifPresent(it -> contracts(collectedContracts, it, false));
        // interfaces are considered contracts by default
        typeInfo.interfaceTypeInfo().forEach(it -> contracts(collectedContracts, it, true));
    }

    private String codeGenQualifier(Qualifier qualifier) {
        if (qualifier.value().isPresent()) {
            return "@" + TypeNames.HELIDON_QUALIFIER.fqName() + "@.create(@" + qualifier.typeName().fqName() + "@.class, "
                    + "\"" + qualifier.value().get() + "\")";
        }
        return "@" + TypeNames.HELIDON_QUALIFIER.fqName() + "@.create(" + qualifier.typeName().fqName() + ".class)";
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

    private void createInstantiateInterceptBody(Method.Builder method,
                                                List<ParamDefinition> params) {
        List<ParamDefinition> constructorParams = declareCtrParamsAndGetThem(method, params);
        String paramsDeclaration = constructorParams.stream()
                .map(ParamDefinition::ipParamName)
                .collect(Collectors.joining(", "));

        method.addLine("var activeInterceptors = interceptMeta.interceptors(QUALIFIERS, ANNOTATIONS, CTOR_ELEMENT);")
                .addLine("if (activeInterceptors.isEmpty()) {") // if active interceptors empty
                .add("return doInstantiate(interceptMeta");
        if (!constructorParams.isEmpty()) {
            method.add(", ")
                    .add(paramsDeclaration);
        }
        method.addLine(");")
                .addLine("} else {") // else if active interceptors empty
                .addLine("return @io.helidon.inject.runtime.Invocation@.createInvokeAndSupply(this,")
                .addLine("ANNOTATIONS,")
                .addLine("CTOR_ELEMENT,")
                .addLine("activeInterceptors,")
                .add("params__helidonInject -> doInstantiate(interceptMeta, params__helidonInject)");
        if (!paramsDeclaration.isEmpty()) {
            method.addLine(",");
            method.add(paramsDeclaration);
        }
        method.addLine(");")
                .addLine("}"); // end if active interceptors empty
    }

    private void createInstantiateBody(TypeName serviceType,
                                       Method.Builder method,
                                       List<ParamDefinition> params,
                                       boolean interceptedMethods) {
        List<ParamDefinition> constructorParams = declareCtrParamsAndGetThem(method, params);
        String paramsDeclaration = constructorParams.stream()
                .map(ParamDefinition::ipParamName)
                .collect(Collectors.joining(", "));

        if (interceptedMethods) {
            // return new MyImpl__Intercepted(interceptMeta, this, ANNOTATIONS, casted params
            method.add("return new ")
                    .add(serviceType.classNameWithEnclosingNames())
                    .addLine("(interceptMeta,")
                    .addLine("this,")
                    .addLine("QUALIFIERS,")
                    .add("ANNOTATIONS");
            if (!constructorParams.isEmpty()) {
                method.addLine(",");
                method.add(paramsDeclaration);
            }
            method.addLine(");");
        } else {
            // return new MyImpl(parameter, parameter2)
            method.addLine("return new " + serviceType.classNameWithEnclosingNames() + "(" + paramsDeclaration + ");");
        }
    }

    private void createDoInstantiateBody(TypeName serviceType,
                                         Method.Builder method,
                                         List<ParamDefinition> params,
                                         boolean interceptedMethods) {
        List<ParamDefinition> constructorParams = params.stream()
                .filter(it -> it.kind() == ElementKind.CONSTRUCTOR)
                .toList();

        List<String> paramDeclarations = new ArrayList<>();
        for (int i = 0; i < constructorParams.size(); i++) {
            ParamDefinition param = constructorParams.get(i);
            paramDeclarations.add("(" + param.type().resolvedName() + ") params[" + i + "]");
        }
        String paramsDeclaration = String.join(", ", paramDeclarations);

        if (interceptedMethods) {
            method.add("return new ")
                    .add(serviceType.classNameWithEnclosingNames())
                    .addLine("(interceptMeta,")
                    .addLine("this,")
                    .addLine("QUALIFIERS,")
                    .add("ANNOTATIONS");
            if (!constructorParams.isEmpty()) {
                method.addLine(",");
                method.add(paramsDeclaration);
            }
            method.addLine(");");
        } else {
            // return new MyImpl(IP_PARAM_0.type().cast(params[0])
            method.addLine("return new " + serviceType.classNameWithEnclosingNames() + "(" + paramsDeclaration + ");");
        }
    }

    private List<ParamDefinition> declareCtrParamsAndGetThem(Method.Builder method, List<ParamDefinition> params) {
        /*
            var ipParam1_serviceProviders = ctx.param(TYPE_NAME, IP_PARAM_1);
            var ipParam2_someOtherName = ctx.param(TYPE_NAME, IP_PARAM_2);
            return new ConfigProducer(ipParam1_serviceProviders, someOtherName);
         */
        List<ParamDefinition> constructorParams = params.stream()
                .filter(it -> it.kind() == ElementKind.CONSTRUCTOR)
                .toList();

        // for each parameter, obtain its value from context
        for (ParamDefinition param : constructorParams) {
            method.addLine("var " + param.ipParamName() + " = ctx.param(TYPE_NAME, " + param.constantName() + ")"
                                   + ";");
        }

        method.addLine("");
        return constructorParams;
    }

    private void createInjectFieldsBody(boolean hasSuperType,
                                        List<ParamDefinition> fields,
                                        Method.Builder method, boolean canIntercept,
                                        Set<TypedElementInfo> maybeIntercepted) {
        /*
        var field1 = ctx.param(TYPE_NAME, IP_PARAM_3);
        instance.field1 = field1;
         */
        if (hasSuperType) {
            method.addLine("super.injectFields(ctx, interceptMeta, instance);");
        }

        for (ParamDefinition param : fields) {
            method.addLine("var " + param.ipParamName() + " = ctx.param(TYPE_NAME, " + param.constantName() + ")"
                                   + ";");
        }

        method.addLine("");

        for (ParamDefinition field : fields) {
            if (canIntercept && maybeIntercepted.contains(field.elementInfo())) {
                String interceptorsName = field.ipParamName() + "__interceptors";
                String constantName = fieldElementConstantName(field.ipParamName);
                method.add("var ")
                        .add(interceptorsName)
                        .add(" = interceptMeta.interceptors(QUALIFIERS, ANNOTATIONS, ")
                        .add(constantName)
                        .addLine(");")
                        .add("if(")
                        .add(interceptorsName)
                        .addLine(".isEmpty() {")
                        .add("instance.")
                        .add(field.ipParamName())
                        .add(" = ")
                        .add(field.ipParamName())
                        .addLine(";")
                        .addLine("} else {")
                        .add("instance.")
                        .add(field.ipParamName())
                        .add(" = ")
                        .addLine("@io.helidon.inject.runtime.Invocation@.createInvokeAndSupply(this,")
                        .addLine("ANNOTATIONS,")
                        .add(constantName)
                        .addLine(",")
                        .add(interceptorsName)
                        .addLine(",")
                        .add("params__helidonInject -> ")
                        .add(field.constantName())
                        .addLine(".type().cast(params__helidonInject[0]),")
                        .add(field.ipParamName())
                        .addLine(");")
                        .addLine("}");
            } else {
                method.addLine("instance." + field.ipParamName() + " = " + field.ipParamName() + ";");
            }
        }
    }

    private void createInjectMethodsBody(boolean hasSuperType,
                                         Map<String, List<ParamDefinition>> methodCalls,
                                         List<ParamDefinition> params,
                                         Method.Builder method) {

        if (hasSuperType) {
            method.addLine("super.injectMethods(ctx, instance);");
        }
        /*
        var ipParam4_param = ctx.param(TYPE_NAME, IP_PARAM_4);
        instance.someMethod(param)
         */
        List<ParamDefinition> methodsParams = params.stream()
                .filter(it -> it.kind == ElementKind.METHOD)
                .toList();

        // for each parameter, obtain its value from context
        for (ParamDefinition param : methodsParams) {
            method.addLine("var " + param.fieldId() + " = ctx.param(TYPE_NAME, " + param.constantName() + ");");
        }

        method.addLine("");

        for (List<ParamDefinition> value : methodCalls.values()) {
            if (value.isEmpty()) {
                continue;
            }
            ParamDefinition first = value.getFirst();
            method.addLine("instance." + first.ipName() + "("
                                   + value.stream()
                    .map(ParamDefinition::fieldId)
                    .collect(Collectors.joining(", "))
                                   + ");");
        }
    }

    private TypeName ipId(TypeName type) {
        return TypeName.builder(TypeNames.INJECTION_PARAMETER_ID)
                .addTypeArgument(type)
                .build();
    }

    private Optional<Double> weight(TypeInfo typeInfo) {
        return typeInfo.findAnnotation(TypeName.create(Weight.class))
                .flatMap(io.helidon.common.types.Annotation::doubleValue);
    }

    private Optional<Integer> runLevel(TypeInfo typeInfo) {
        return typeInfo.findAnnotation(TypeNames.RUN_LEVEL_TYPE)
                .flatMap(io.helidon.common.types.Annotation::intValue);
    }

    private void params(TypeInfo typeInfo,
                        List<MethodDefinition> methods,
                        List<ParamDefinition> params,
                        TypedElementInfo constructor,
                        List<TypedElementInfo> fieldInjectElements,
                        List<TypedElementInfo> methodInjectElements) {
        AtomicInteger paramCounter = new AtomicInteger();
        AtomicInteger methodCounter = new AtomicInteger();

        if (!constructor.parameterArguments().isEmpty()) {
            injectConstructorParams(typeInfo, params, paramCounter, constructor);
        }

        fieldInjectElements
                .forEach(it -> fieldParam(typeInfo, params, paramCounter, it));

        methodInjectElements
                .forEach(it -> methodParams(typeInfo, methods, params, methodCounter, paramCounter, it));

    }

    private void methodParams(TypeInfo service,
                              List<MethodDefinition> methods,
                              List<ParamDefinition> params,
                              AtomicInteger methodCounter,
                              AtomicInteger paramCounter,
                              TypedElementInfo method) {

        String methodId = method.elementName() + "_" + methodCounter.getAndIncrement();

        MethodDefinition methodDefinition = new MethodDefinition(methodId,
                                                                 method.elementName(),
                                                                 method.parameterArguments()
                                                                         .stream()
                                                                         .map(TypedElementInfo::typeName)
                                                                         .toList());
        methods.add(methodDefinition);
        method.parameterArguments()
                .stream()
                .map(param -> new ParamDefinition(param,
                                                  "IP_PARAM_" + paramCounter.get(),
                                                  "ipParam" + paramCounter.getAndIncrement(),
                                                  param.typeName(),
                                                  ElementKind.METHOD,
                                                  method.elementName(),
                                                  param.elementName(),
                                                  methodId + "_" + param.elementName(),
                                                  method.modifiers().contains(Modifier.STATIC),
                                                  param.annotations(),
                                                  qualifiers(service, param),
                                                  contract("Method " + service.typeName()
                                                                   .fqName() + "#" + method.elementName() + ", parameter: " + param.elementName(),
                                                           param.typeName()),
                                                  method.accessModifier(),
                                                  methodId))
                .forEach(params::add);
    }

    private void fieldParam(TypeInfo service, List<ParamDefinition> result, AtomicInteger paramCounter, TypedElementInfo field) {
        result.add(new ParamDefinition(field,
                                       "IP_PARAM_" + paramCounter.get(),
                                       "ipParam" + paramCounter.getAndIncrement(),
                                       field.typeName(),
                                       ElementKind.FIELD,
                                       field.elementName(),
                                       field.elementName(),
                                       field.elementName(),
                                       field.modifiers().contains(Modifier.STATIC),
                                       field.annotations(),
                                       qualifiers(service, field),
                                       contract("Field " + service.typeName().fqName() + "." + field.elementName(),
                                                field.typeName()),
                                       field.accessModifier(),
                                       null));
    }

    private void injectConstructorParams(TypeInfo service,
                                         List<ParamDefinition> result,
                                         AtomicInteger paramCounter,
                                         TypedElementInfo constructor) {
        constructor.parameterArguments()
                .stream()
                .map(param -> new ParamDefinition(param,
                                                  "IP_PARAM_" + paramCounter.get(),
                                                  "ipParam" + paramCounter.getAndIncrement(),
                                                  param.typeName(),
                                                  ElementKind.CONSTRUCTOR,
                                                  constructor.elementName(),
                                                  param.elementName(),
                                                  param.elementName(),
                                                  false,
                                                  param.annotations(),
                                                  qualifiers(service, param),
                                                  contract(service.typeName()
                                                                   .fqName() + " Constructor parameter: " + param.elementName(),
                                                           param.typeName()),
                                                  constructor.accessModifier(),
                                                  "<init>"))
                .forEach(result::add);
    }

    private TypeName contract(String description, TypeName typeName) {
        /*
         get the contract expected for this injection point
         IP may be:
          - Optional
          - List
          - ServiceProvider
          - Provider
          - Optional<ServiceProvider>
          - Optional<Provider>
          - List<ServiceProvider>
          - List<Provider>
         */

        if (typeName.isOptional()) {
            if (typeName.typeArguments().isEmpty()) {
                throw new IllegalArgumentException("Injection point with Optional type must have a declared type argument: " + description);
            }
            return contract(description, typeName.typeArguments().getFirst());
        }
        if (typeName.isList()) {
            if (typeName.typeArguments().isEmpty()) {
                throw new IllegalArgumentException("Injection point with List type must have a declared type argument: " + description);
            }
            return contract(description, typeName.typeArguments().getFirst());
        }
        if (typeName.equals(TypeNames.SERVICE_PROVIDER_TYPE)) {
            if (typeName.typeArguments().isEmpty()) {
                throw new IllegalArgumentException(
                        "Injection point with ServiceProvider type must have a declared type argument: " + description);
            }
            return contract(description, typeName.typeArguments().getFirst());
        }
        if (typeName.equals(TypeNames.JAKARTA_PROVIDER_TYPE)) {
            if (typeName.typeArguments().isEmpty()) {
                throw new IllegalArgumentException("Injection point with Provider type must have a declared type argument: " + description);
            }
            return contract(description, typeName.typeArguments().getFirst());
        }

        return typeName;
    }

    private Set<Qualifier> qualifiers(TypeInfo service, Annotated element) {
        Set<Qualifier> result = new LinkedHashSet<>();

        for (io.helidon.common.types.Annotation anno : element.annotations()) {
            if (service.hasMetaAnnotation(anno.typeName(), TypeNames.JAKARTA_QUALIFIER_TYPE)) {
                result.add(Qualifier.create(anno));
            }
        }

        // note: should qualifiers be inheritable? Right now we assume not to support the jsr-330 spec (see note above).
        return result;
    }

    // find constructor with @Inject, if none, find the first constructor (assume @Inject)
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
        first = typeInfo.elementInfo()
                .stream()
                .filter(it -> it.elementTypeKind() == ElementKind.CONSTRUCTOR)
                .findFirst();

        return first.orElse(DEFAULT_CONSTRUCTOR);
    }

    private String fieldElementConstantName(String elementName) {
        return "FIELD_INFO_" + toConstantName(elementName);
    }

    private record GenericTypeDeclaration(String constantName,
                                          TypeName typeName) {
    }

    private record MethodDefinition(String methodId,
                                    String name,
                                    List<TypeName> types) {
    }

    private record ParamDefinition(TypedElementInfo elementInfo,
                                   String constantName,
                                   String fieldName,
                                   TypeName type,
                                   ElementKind kind,
                                   String ipName,
                                   String ipParamName,
                                   String fieldId,
                                   boolean isStatic,
                                   List<io.helidon.common.types.Annotation> annotations,
                                   Set<Qualifier> qualifiers,
                                   TypeName contract,
                                   AccessModifier access,
                                   String methodId) {

    }

}
