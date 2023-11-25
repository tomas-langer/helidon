package io.helidon.inject.codegen;

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
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.CodegenOptions;
import io.helidon.codegen.CopyrightHandler;
import io.helidon.codegen.ElementInfoPredicates;
import io.helidon.codegen.GeneratedAnnotationHandler;
import io.helidon.codegen.TypesCodeGen;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Method;
import io.helidon.codegen.classmodel.TypeArgument;
import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotated;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.Modifier;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.codegen.spi.InjectCodegenExtension;
import io.helidon.inject.codegen.spi.InjectCodegenObserver;

import static io.helidon.codegen.CodegenUtil.capitalize;
import static io.helidon.codegen.CodegenUtil.toConstantName;
import static io.helidon.inject.codegen.InjectCodegenTypes.HELIDON_SERVICE_SOURCE_METHOD;
import static java.util.function.Predicate.not;

class InjectionExtension implements InjectCodegenExtension {
    static final TypeName LIST_OF_ANNOTATIONS = TypeName.builder(TypeNames.LIST)
            .addTypeArgument(TypeNames.HELIDON_ANNOTATION)
            .build();
    static final TypeName SET_OF_QUALIFIERS = TypeName.builder(TypeNames.SET)
            .addTypeArgument(InjectCodegenTypes.HELIDON_QUALIFIER)
            .build();
    static final TypeName SET_OF_TYPES = TypeName.builder(TypeNames.SET)
            .addTypeArgument(TypeNames.HELIDON_TYPE_NAME)
            .build();

    static final TypeName SET_OF_SIGNATURES = TypeName.builder(TypeNames.SET)
            .addTypeArgument(InjectCodegenTypes.HELIDON_SERVICE_SOURCE_METHOD)
            .build();
    private static final TypeName LIST_OF_IP_IDS = TypeName.builder(TypeNames.LIST)
            .addTypeArgument(InjectCodegenTypes.HELIDON_IP_ID)
            .build();

    private static final TypeName SERVICE_SOURCE_TYPE = TypeName.builder(InjectCodegenTypes.HELIDON_SERVICE_SOURCE)
            .addTypeArgument(TypeName.create("T"))
            .build();
    private static final TypeName GENERATOR = TypeName.create(InjectionExtension.class);
    private static final Annotation RUNTIME_RETENTION = Annotation.create(Retention.class, RetentionPolicy.RUNTIME.name());
    private static final Annotation CLASS_RETENTION = Annotation.create(Retention.class, RetentionPolicy.CLASS.name());
    private static final TypedElementInfo DEFAULT_CONSTRUCTOR = TypedElementInfo.builder()
            .typeName(TypeNames.OBJECT)
            .accessModifier(AccessModifier.PUBLIC)
            .elementTypeKind(ElementKind.CONSTRUCTOR)
            .build();
    private static final TypeName GENERIC_T_TYPE = TypeName.createFromGenericDeclaration("T");

    private final InjectionCodegenContext ctx;
    private final boolean autoAddContracts;
    private final InterceptionStrategy interceptionStrategy;
    private final Set<TypeName> scopeMetaAnnotations;
    private final List<InjectCodegenObserver> observers;
    private final boolean strictJsr330;

    InjectionExtension(InjectionCodegenContext codegenContext) {
        this.ctx = codegenContext;
        CodegenOptions options = codegenContext.options();
        this.autoAddContracts = InjectOptions.autoAddNonContractInterfaces(options);
        this.interceptionStrategy = InjectOptions.interceptionStrategy(options);
        this.scopeMetaAnnotations = InjectOptions.scopeMetaAnnotations(options);
        this.observers = HelidonServiceLoader.create(ServiceLoader.load(InjectCodegenObserver.class))
                .asList();
        this.strictJsr330 = options.enabled(InjectOptions.JSR_330_STRICT);
    }

    @Override
    public void process(RoundContext roundContext) {
        Collection<TypeInfo> descriptorsRequired = roundContext.types();

        for (TypeInfo typeInfo : descriptorsRequired) {
            generateDescriptor(descriptorsRequired, typeInfo);
        }

        notifyObservers(roundContext, descriptorsRequired);
    }

    private void generateDescriptor(Collection<TypeInfo> services,
                                    TypeInfo typeInfo) {
        if (typeInfo.typeKind() == ElementKind.INTERFACE) {
            // we cannot support multiple inheritance, so descriptors for interfaces do not make sense
            return;
        }
        boolean isAbstractClass = typeInfo.modifiers().contains(Modifier.ABSTRACT)
                && typeInfo.typeKind() == ElementKind.CLASS;

        SuperType superType = superType(typeInfo, services);

        // this set now contains all fields, constructors, and methods that may be intercepted, as they contain
        // an annotation that is an interception trigger (based on interceptionStrategy)
        Set<TypedElementInfo> maybeIntercepted = maybeIntercepted(typeInfo);
        boolean canIntercept = !maybeIntercepted.isEmpty();
        boolean methodsIntercepted = maybeIntercepted.stream()
                .anyMatch(ElementInfoPredicates::isMethod);

        TypeName serviceType = typeInfo.typeName();
        // this must result in generating a service descriptor file
        TypeName descriptorType = ctx.descriptorType(serviceType);

        List<ParamDefinition> params = new ArrayList<>();
        List<MethodDefinition> methods = new ArrayList<>();

        TypedElementInfo constructorInjectElement = injectConstructor(typeInfo);
        List<TypedElementInfo> fieldInjectElements = fieldInjectElements(typeInfo);
        List<TypedElementInfo> methodInjectElements = methodInjectElements(services, typeInfo, superType);

        boolean constructorIntercepted = maybeIntercepted.contains(constructorInjectElement);

        params(services,
               typeInfo,
               superType,
               methods,
               params,
               constructorInjectElement,
               fieldInjectElements,
               methodInjectElements);

        Map<String, GenericTypeDeclaration> genericTypes = genericTypes(params, methods);
        Set<TypeName> scopes = scopes(typeInfo);
        Set<Annotation> qualifiers = qualifiers(typeInfo, typeInfo);
        Set<TypeName> contracts = new HashSet<>();
        contracts(contracts, typeInfo, autoAddContracts);

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

        if (superType.hasSupertype()) {
            classModel.superType(superType.superDescriptorType());
        } else {
            classModel.addInterface(SERVICE_SOURCE_TYPE);
        }

            /*
            Fields
             */
        singletonInstanceField(classModel, descriptorType);
        serviceTypeFields(classModel, serviceType, descriptorType);
        typeFields(classModel, genericTypes);
        injectionPointIdFields(classModel, genericTypes, params);
        contractsField(classModel, contracts);
        dependenciesField(classModel, params);
        qualifiersField(classModel, qualifiers);
        scopesField(classModel, scopes, superType);
        methodFields(classModel, methods);

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
        dependenciesMethod(classModel, params, superType);
        isAbstractMethod(classModel, superType, isAbstractClass);
        instantiateMethod(classModel, serviceType, params, isAbstractClass, constructorIntercepted, methodsIntercepted);
        injectMethod(classModel, serviceType, params, superType, methods, canIntercept, maybeIntercepted);
        postConstructMethod(typeInfo, classModel, serviceType);
        preDestroyMethod(typeInfo, classModel, serviceType);
        qualifiersMethod(classModel, qualifiers, superType);
        scopesMethod(classModel, scopes, superType);
        weightMethod(typeInfo, classModel, superType);
        runLevelMethod(typeInfo, classModel, superType);

        ctx.addDescriptor(serviceType,
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

            ctx.addType(interceptedType,
                        generator.generate(),
                        serviceType,
                        typeInfo.originatingElement().orElse(serviceType));
        }
    }

    private SuperType superType(TypeInfo typeInfo, Collection<TypeInfo> services) {
        // find super type if it is also a service (or has a service descriptor)

        // check if the super type is part of current annotation processing
        Optional<TypeInfo> superTypeInfoOptional = typeInfo.superTypeInfo();
        if (superTypeInfoOptional.isEmpty()) {
            return SuperType.noSuperType();
        }
        TypeInfo superType = superTypeInfoOptional.get();
        TypeName expectedSuperDescriptor = ctx.descriptorType(superType.typeName());
        TypeName superTypeToExtend = TypeName.builder(expectedSuperDescriptor)
                .addTypeArgument(TypeName.create("T"))
                .build();
        for (TypeInfo service : services) {
            if (service.typeName().equals(superType.typeName())) {
                return new SuperType(true, superTypeToExtend, service);
            }
        }
        // if not found in current list, try checking existing types
        return ctx.typeInfo(expectedSuperDescriptor)
                .map(it -> new SuperType(true, expectedSuperDescriptor, superType))
                .orElseGet(SuperType::noSuperType);
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
                                          throw new CodegenException(typeInfo.typeName()
                                                                             .fqName() + "#" + it.elementName() + " is declared "
                                                                             + "as private, but has interceptor trigger "
                                                                             + "annotation declared. "
                                                                             + "This cannot be supported, as we do not modify "
                                                                             + "sources or bytecode.",
                                                                     it.originatingElement().orElse(typeInfo.typeName()));
                                      }
                                  })
                                  .toList());
        }

        return result;
    }

    private boolean hasInterceptTrigger(TypeInfo typeInfo, Annotated element) {
        for (Annotation annotation : element.annotations()) {
            if (interceptionStrategy.ordinal() >= InterceptionStrategy.EXPLICIT.ordinal()) {
                if (typeInfo.hasMetaAnnotation(annotation.typeName(), InjectCodegenTypes.HELIDON_INTERCEPTED_TRIGGER)) {
                    return true;
                }
            }
            if (interceptionStrategy.ordinal() >= InterceptionStrategy.ALL_RUNTIME.ordinal()) {
                Optional<Annotation> retention = typeInfo.metaAnnotation(annotation.typeName(),
                                                                         TypeNames.RETENTION);
                boolean isRuntime = retention.map(RUNTIME_RETENTION::equals).orElse(false);
                if (isRuntime) {
                    return true;
                }
            }
            if (interceptionStrategy.ordinal() >= InterceptionStrategy.ALL_RETAINED.ordinal()) {
                Optional<Annotation> retention = typeInfo.metaAnnotation(annotation.typeName(),
                                                                         TypeNames.RETENTION);
                boolean isClass = retention.map(CLASS_RETENTION::equals).orElse(false);
                if (isClass) {
                    return true;
                }
            }
        }
        return false;
    }

    // find constructor with @Inject, if none, find the first constructor (assume @Inject)
    private TypedElementInfo injectConstructor(TypeInfo typeInfo) {
        // first @Inject
        Optional<TypedElementInfo> first = typeInfo.elementInfo()
                .stream()
                .filter(it -> it.elementTypeKind() == ElementKind.CONSTRUCTOR)
                .filter(it -> it.hasAnnotation(InjectCodegenTypes.INJECT_INJECT))
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

    private List<TypedElementInfo> fieldInjectElements(TypeInfo typeInfo) {
        return typeInfo.elementInfo()
                .stream()
                .filter(not(ElementInfoPredicates::isPrivate))
                .filter(not(ElementInfoPredicates::isStatic))
                .filter(ElementInfoPredicates::isField)
                .filter(ElementInfoPredicates.hasAnnotation(InjectCodegenTypes.INJECT_INJECT))
                .toList();
    }

    private List<TypedElementInfo> methodInjectElements(Collection<TypeInfo> services, TypeInfo typeInfo, SuperType superType) {
        List<TypedElementInfo> atInjectMethods = typeInfo.elementInfo()
                .stream()
                .filter(not(ElementInfoPredicates::isPrivate))
                .filter(not(ElementInfoPredicates::isStatic))
                .filter(ElementInfoPredicates::isMethod)
                .filter(ElementInfoPredicates.hasAnnotation(InjectCodegenTypes.INJECT_INJECT))
                .toList();

        List<TypedElementInfo> result = new ArrayList<>(atInjectMethods);

        if (!superType.hasSupertype()) {
            return result;
        }

        List<TypedElementInfo> otherMethods = typeInfo.elementInfo()
                .stream()
                .filter(ElementInfoPredicates::isMethod)
                .filter(not(ElementInfoPredicates::isStatic))
                .filter(not(ElementInfoPredicates::isPrivate))
                .filter(it -> !it.hasAnnotation(InjectCodegenTypes.INJECT_INJECT))
                .toList();

        if (otherMethods.isEmpty()) {
            return result;
        }

        for (TypedElementInfo otherMethod : otherMethods) {
            // now find all methods that override a method that is annotated from any supertype (ouch)
            if (overrides(services,
                          superType.superType(),
                          otherMethod,
                          typeInfo.typeName().packageName(),
                          InjectCodegenTypes.INJECT_INJECT)) {

                result.add(otherMethod);
            }
        }
        return result;
    }

    private boolean overrides(Collection<TypeInfo> services,
                              TypeInfo type,
                              TypedElementInfo method,
                              String currentPackage,
                              TypeName... expectedAnnotations) {
        List<TypeName> arguments = method.parameterArguments()
                .stream()
                .map(TypedElementInfo::typeName)
                .toList();
        String methodName = method.elementName();
        // we look only for exact match (including types)
        Optional<TypedElementInfo> found = type.elementInfo()
                .stream()
                .filter(ElementInfoPredicates::isMethod)
                .filter(not(ElementInfoPredicates::isPrivate))
                .filter(ElementInfoPredicates.elementName(methodName))
                .filter(it -> {
                    for (TypeName expectedAnnotation : expectedAnnotations) {
                        if (!it.hasAnnotation(expectedAnnotation)) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(ElementInfoPredicates.hasParams(arguments))
                .findFirst();

        boolean normalCase = true;
        if (found.isPresent()) {
            // only found if super method protected or public, or if both type in the same package
            TypedElementInfo superMethod = found.get();
            if (superMethod.accessModifier() == AccessModifier.PUBLIC
                    || superMethod.accessModifier() == AccessModifier.PROTECTED) {
                return true;
            }
            if (superMethod.accessModifier() == AccessModifier.PACKAGE_PRIVATE) {
                // there may be another type in the hierarchy that declares the same method that is in the same package
                if (currentPackage.equals(type.typeName().packageName())) {
                    return true;
                }
                normalCase = false;
            }
            if (normalCase) {
                return false;
            }
        }
        SuperType superType = superType(type, services);
        if (superType.hasSupertype) {
            return overrides(services, superType.superType(), method, currentPackage, expectedAnnotations);
        }
        return false;
    }

    private void params(Collection<TypeInfo> services,
                        TypeInfo service,
                        SuperType superType,
                        List<MethodDefinition> methods,
                        List<ParamDefinition> params,
                        TypedElementInfo constructor,
                        List<TypedElementInfo> fieldInjectElements,
                        List<TypedElementInfo> methodInjectElements) {
        AtomicInteger paramCounter = new AtomicInteger();
        AtomicInteger methodCounter = new AtomicInteger();

        if (!constructor.parameterArguments().isEmpty()) {
            injectConstructorParams(service, params, paramCounter, constructor);
        }

        fieldInjectElements
                .forEach(it -> fieldParam(service, params, paramCounter, it));

        methodInjectElements
                .forEach(it -> methodParams(services, service, superType, methods, params, methodCounter, paramCounter, it));

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

    private void methodParams(Collection<TypeInfo> services,
                              TypeInfo service,
                              SuperType superType,
                              List<MethodDefinition> methods,
                              List<ParamDefinition> params,
                              AtomicInteger methodCounter,
                              AtomicInteger paramCounter,
                              TypedElementInfo method) {

        List<ParamDefinition> methodParams = new ArrayList<>();
        int methodIndex = methodCounter.getAndIncrement();
        String methodId = method.elementName() + "_" + methodIndex;

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
                .forEach(methodParams::add);

        // there is a chance this method is forbidding injection
        // overrides a method with @Inject that do not have @Inject should forbid the super type injection
        boolean isInjectionPoint = method.hasAnnotation(InjectCodegenTypes.INJECT_INJECT);

        if (isInjectionPoint) {
            params.addAll(methodParams);
        }

        String constantName = "METHOD_" + methodIndex;

        boolean overrides;
        if (superType.hasSupertype()) {
            overrides = overrides(services, superType.superType(), method, service.typeName().packageName());
        } else {
            overrides = false;
        }
        MethodDefinition methodDefinition = new MethodDefinition(method.accessModifier(),
                                                                 methodId,
                                                                 constantName,
                                                                 method.elementName(),
                                                                 overrides,
                                                                 methodParams,
                                                                 isInjectionPoint);

        methods.add(methodDefinition);
    }

    private Set<Annotation> qualifiers(TypeInfo service, Annotated element) {
        Set<Annotation> result = new LinkedHashSet<>();

        for (Annotation anno : element.annotations()) {
            if (service.hasMetaAnnotation(anno.typeName(), InjectCodegenTypes.INJECT_QUALIFIER)) {
                result.add(anno);
            }
        }

        // note: should qualifiers be inheritable? Right now we assume not to support the jsr-330 spec (see note above).
        return result;
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
        if (typeName.equals(InjectCodegenTypes.HELIDON_SERVICE_PROVIDER)) {
            if (typeName.typeArguments().isEmpty()) {
                throw new IllegalArgumentException(
                        "Injection point with ServiceProvider type must have a declared type argument: " + description);
            }
            return contract(description, typeName.typeArguments().getFirst());
        }
        if (typeName.equals(InjectCodegenTypes.INJECT_PROVIDER)) {
            if (typeName.typeArguments().isEmpty()) {
                throw new IllegalArgumentException("Injection point with Provider type must have a declared type argument: " + description);
            }
            return contract(description, typeName.typeArguments().getFirst());
        }

        return typeName;
    }

    private Map<String, GenericTypeDeclaration> genericTypes(List<ParamDefinition> params, List<MethodDefinition> methods) {
        // we must use map by string (as type name is equal if the same class, not full generic declaration)
        Map<String, GenericTypeDeclaration> result = new LinkedHashMap<>();
        AtomicInteger counter = new AtomicInteger();

        for (ParamDefinition param : params) {
            result.computeIfAbsent(param.type().resolvedName(),
                                   type -> new GenericTypeDeclaration("TYPE_" + counter.getAndIncrement(),
                                                                      param.type()));
            result.computeIfAbsent(param.contract().fqName(),
                                   type -> new GenericTypeDeclaration("TYPE_" + counter.getAndIncrement(),
                                                                      param.type()));
        }

        for (MethodDefinition method : methods) {
            method.params()
                    .forEach(it -> result.computeIfAbsent(it.type().resolvedName(),
                                                          type -> new GenericTypeDeclaration("TYPE_" + counter.getAndIncrement(),
                                                                                             it.type())));
        }

        return result;
    }

    private Set<TypeName> scopes(TypeInfo service) {
        Set<TypeName> result = new LinkedHashSet<>();

        for (Annotation anno : service.annotations()) {
            TypeName annoType = anno.typeName();
            for (TypeName scopeMetaAnnotation : scopeMetaAnnotations) {
                if (service.hasMetaAnnotation(annoType, scopeMetaAnnotation)) {
                    result.add(annoType);
                }
            }
        }

        return result;
    }

    private void contracts(Set<TypeName> collectedContracts, TypeInfo typeInfo, boolean contractEligible) {
        TypeName typeName = typeInfo.typeName();

        if (contractEligible) {
            collectedContracts.add(typeName);
        }

        if (typeName.equals(InjectCodegenTypes.INJECT_PROVIDER)
                || typeName.equals(InjectCodegenTypes.HELIDON_INJECTION_POINT_PROVIDER)
                || typeName.equals(InjectCodegenTypes.HELIDON_SERVICE_PROVIDER)) {
            // this may be the interface itself, and then it does not have a type argument
            if (!typeName.typeArguments().isEmpty()) {
                // provider must have a type argument (and the type argument is an automatic contract
                TypeName providedType = typeName.typeArguments().getFirst();
                if (!providedType.generic()) {
                    collectedContracts.add(providedType);
                }
            }

            // provider itself is a contract
            collectedContracts.add(typeName);
        }

        // add contracts from interfaces and types annotated as @Contract
        typeInfo.findAnnotation(InjectCodegenTypes.HELIDON_CONTRACT)
                .ifPresent(it -> collectedContracts.add(typeInfo.typeName()));

        // add contracts from @ExternalContracts
        typeInfo.findAnnotation(InjectCodegenTypes.HELIDON_EXTERNAL_CONTRACTS)
                .ifPresent(it -> collectedContracts.addAll(it.typeValues().orElseGet(List::of)));

        // go through hierarchy
        typeInfo.superTypeInfo().ifPresent(it -> contracts(collectedContracts, it, contractEligible));
        // interfaces are considered contracts by default
        typeInfo.interfaceTypeInfo().forEach(it -> contracts(collectedContracts, it, contractEligible));
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

    private void serviceTypeFields(ClassModel.Builder classModel, TypeName serviceType, TypeName descriptorType) {
        classModel.addField(field -> field
                .isStatic(true)
                .isFinal(true)
                .accessModifier(AccessModifier.PRIVATE)
                .type(TypeNames.HELIDON_TYPE_NAME)
                .name("TYPE_NAME")
                .defaultValueContent("@" + TypeName.class.getName() + "@.create("
                                             + serviceType.classNameWithEnclosingNames() + ".class)"));

        classModel.addField(field -> field
                .isStatic(true)
                .isFinal(true)
                .accessModifier(AccessModifier.PRIVATE)
                .type(TypeNames.HELIDON_TYPE_NAME)
                .name("DESCRIPTOR_TYPE")
                .defaultValueContent("@" + TypeName.class.getName() + "@.create("
                                             + descriptorType.classNameWithEnclosingNames() + ".class)"));
    }

    private void typeFields(ClassModel.Builder classModel, Map<String, GenericTypeDeclaration> genericTypes) {
        // constants for injection point parameter types (used by next section)
        genericTypes.forEach((typeName, generic) -> classModel.addField(field -> field.accessModifier(AccessModifier.PRIVATE)
                .isStatic(true)
                .isFinal(true)
                .type(TypeNames.HELIDON_TYPE_NAME)
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

    private void injectionPointIdFields(ClassModel.Builder classModel,
                                        Map<String, GenericTypeDeclaration> genericTypes,
                                        List<ParamDefinition> params) {
        // constant for injection points
        for (ParamDefinition param : params) {
            classModel.addField(field -> field
                    .accessModifier(AccessModifier.PUBLIC)
                    .isStatic(true)
                    .isFinal(true)
                    .type(InjectCodegenTypes.HELIDON_IP_ID)
                    .name(param.constantName())
                    .update(it -> {
                        StringBuilder defaultContent = new StringBuilder();
                        defaultContent.append("@io.helidon.inject.api.IpId@")
                                .append(".builder()")
                                .append(".typeName(")
                                .append(genericTypes.get(param.type().resolvedName()).constantName())
                                .append(")")
                                .append(".elementKind(@io.helidon.common.types.ElementKind@.")
                                .append(param.kind.name())
                                .append(")")
                                .append(".name(\"")
                                .append(param.fieldId())
                                .append("\")")
                                .append(".service(TYPE_NAME)")
                                .append(".descriptor(DESCRIPTOR_TYPE)")
                                .append(".field(\"")
                                .append(param.constantName())
                                .append("\")")
                                .append(".contract(")
                                .append(genericTypes.get(param.contract().fqName()).constantName())
                                .append(")");
                        if (param.access() != AccessModifier.PACKAGE_PRIVATE) {
                            defaultContent.append(".access(@io.helidon.common.types.AccessModifier@.")
                                    .append(param.access())
                                    .append(")");
                        }

                        if (param.isStatic()) {
                            defaultContent.append(".isStatic(true)");
                        }

                        if (!param.qualifiers().isEmpty()) {
                            for (Annotation qualifier : param.qualifiers()) {
                                defaultContent.append(
                                                "\n.addQualifier(qualifier -> qualifier.typeName(@io.helidon.common.types"
                                                        + ".TypeName@"
                                                        + ".create(\"")
                                        .append(qualifier.typeName())
                                        .append("\"))");
                                qualifier.value().ifPresent(q -> defaultContent.append(".value(\"")
                                        .append(q)
                                        .append("\")"));
                                defaultContent.append(")");
                            }
                        }

                        defaultContent.append(".build()");
                        it.defaultValueContent(defaultContent.toString());
                    }));
        }
    }

    private void contractsField(ClassModel.Builder classModel, Set<TypeName> contracts) {
        if (contracts.isEmpty()) {
            return;
        }
        classModel.addField(contractsField -> contractsField
                .isStatic(true)
                .isFinal(true)
                .name("CONTRACTS")
                .type(SET_OF_TYPES)
                .defaultValueContent("@java.util.Set@.of(" + contracts.stream()
                        .map(it -> "@io.helidon.common.types.TypeName@.create(@" + it.fqName() + "@.class)")
                        .collect(Collectors.joining(", "))
                                             + ")"));
    }

    private void dependenciesField(ClassModel.Builder classModel, List<ParamDefinition> params) {
        classModel.addField(dependencies -> dependencies
                .isStatic(true)
                .isFinal(true)
                .name("DEPENDENCIES")
                .type(LIST_OF_IP_IDS)
                .defaultValueContent("@java.util.List@.of(" + params.stream()
                        .map(ParamDefinition::constantName)
                        .collect(Collectors.joining(", "))
                                             + ")"));
    }

    private void qualifiersField(ClassModel.Builder classModel, Set<Annotation> qualifiers) {
        classModel.addField(qualifiersField -> qualifiersField
                .isStatic(true)
                .isFinal(true)
                .name("QUALIFIERS")
                .type(SET_OF_QUALIFIERS)
                .defaultValueContent("@java.util.Set@.of(" + qualifiers.stream()
                        .map(this::codeGenQualifier)
                        .collect(Collectors.joining(", "))
                                             + ")"));
    }

    private String codeGenQualifier(Annotation qualifier) {
        if (qualifier.value().isPresent()) {
            return "@" + InjectCodegenTypes.HELIDON_QUALIFIER.fqName() + "@.create(@" + qualifier.typeName()
                    .fqName() + "@.class, "
                    + "\"" + qualifier.value().get() + "\")";
        }
        return "@" + InjectCodegenTypes.HELIDON_QUALIFIER.fqName() + "@.create(" + qualifier.typeName().fqName() + ".class)";
    }

    private void scopesField(ClassModel.Builder classModel, Set<TypeName> scopes, SuperType superType) {
        if (!superType.hasSupertype()) {
            if (scopes.size() == 1 && scopes.contains(InjectCodegenTypes.INJECT_SINGLETON)) {
                // this is the default as returned from the parent
                return;
            }
        }
        classModel.addField(scopesField -> scopesField
                .isStatic(true)
                .isFinal(true)
                .name("SCOPES")
                .type(SET_OF_TYPES)
                .defaultValueContent("@java.util.Set@.of(" + scopes.stream()
                        .map(it -> TypesCodeGen.toCreate(it, false))
                        .collect(Collectors.joining(", "))
                                             + ")"));
    }

    private void methodFields(ClassModel.Builder classModel, List<MethodDefinition> methods) {
        for (MethodDefinition method : methods) {
            classModel.addField(methodField -> methodField
                    .isStatic(true)
                    .isFinal(true)
                    .name(method.constantName)
                    .type(HELIDON_SERVICE_SOURCE_METHOD)
                    // MethodSignature is inherited from ServiceSource, no need to import
                    .defaultValueContent(fieldForMethodConstantBody(method)));
        }
    }

    private String fieldForMethodConstantBody(MethodDefinition method) {
        StringBuilder result = new StringBuilder("new @" + HELIDON_SERVICE_SOURCE_METHOD.fqName() + "@(\"")
                .append(method.methodName)
                .append("\"");

        if (!method.params().isEmpty()) {
            result.append(", @java.util.List@.of(\"");
            result.append(method.params()
                                  .stream()
                                  .map(ParamDefinition::type)
                                  .map(TypeName::resolvedName)
                                  .collect(Collectors.joining("\", \"")));
            result.append("\")");
        }

        return result.append(")")
                .toString();
    }

    private void annotationsField(ClassModel.Builder classModel, TypeInfo typeInfo) {
        classModel.addField(annotations -> annotations
                .isStatic(true)
                .isFinal(true)
                .name("ANNOTATIONS")
                .type(LIST_OF_ANNOTATIONS)
                .defaultValueContent("@java.util.List@.of(" + typeInfo.annotations()
                        .stream()
                        .map(TypesCodeGen::toCreate)
                        .collect(Collectors.joining(", "))
                                             + ")"));
    }

    private void fieldElementField(ClassModel.Builder classModel, TypedElementInfo fieldElement) {
        classModel.addField(ctorElement -> ctorElement
                .isStatic(true)
                .isFinal(true)
                .name(fieldElementConstantName(fieldElement.elementName()))
                .type(TypeNames.HELIDON_TYPED_ELEMENT_INFO)
                .defaultValueContent(TypesCodeGen.toCreate(fieldElement)));
    }

    private String fieldElementConstantName(String elementName) {
        return "FIELD_INFO_" + toConstantName(elementName);
    }

    private void constructorElementField(ClassModel.Builder classModel, TypedElementInfo constructorInjectElement) {
        classModel.addField(ctorElement -> ctorElement
                .isStatic(true)
                .isFinal(true)
                .name("CTOR_ELEMENT")
                .type(TypeNames.HELIDON_TYPED_ELEMENT_INFO)
                .defaultValueContent(TypesCodeGen.toCreate(constructorInjectElement)));
    }

    private void serviceTypeMethod(ClassModel.Builder classModel) {
        // TypeName serviceType()
        classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.HELIDON_TYPE_NAME)
                .name("serviceType")
                .addLine("return TYPE_NAME;"));
    }

    private void descriptorTypeMethod(ClassModel.Builder classModel) {
        // TypeName descriptorType()
        classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.HELIDON_TYPE_NAME)
                .name("descriptorType")
                .addLine("return DESCRIPTOR_TYPE;"));
    }

    private void contractsMethod(ClassModel.Builder classModel, Set<TypeName> contracts) {
        if (contracts.isEmpty()) {
            return;
        }
        // Set<Class<?>> contracts()
        classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                .name("contracts")
                .returnType(SET_OF_TYPES)
                .addLine("return CONTRACTS;"));
    }

    private void dependenciesMethod(ClassModel.Builder classModel, List<ParamDefinition> params, SuperType superType) {
        // List<InjectionParameterId> dependencies()
        boolean hasSuperType = superType.hasSupertype();
        if (hasSuperType || !params.isEmpty()) {
            classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                    .returnType(LIST_OF_IP_IDS)
                    .name("dependencies")
                    .update(it -> {
                        if (hasSuperType) {
                            it.addLine("return combineDependencies(DEPENDENCIES, super.dependencies());");
                        } else {
                            it.addLine("return DEPENDENCIES;");
                        }
                    }));
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

        // T instantiate(InjectionContext ctx, InterceptionMetadata interceptMeta)
        TypeName toInstantiate = interceptedMethods
                ? TypeName.builder(serviceType).className(serviceType.className() + "__Intercepted").build()
                : serviceType;

        classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                .returnType(serviceType)
                .name("instantiate")
                .addParameter(ctxParam -> ctxParam.type(InjectCodegenTypes.HELIDON_INJECTION_CONTEXT)
                        .name("ctx"))
                .addParameter(interceptMeta -> interceptMeta.type(InjectCodegenTypes.HELIDON_INTERCEPTION_METADATA)
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
                    .addParameter(interceptMeta -> interceptMeta.type(InjectCodegenTypes.HELIDON_INTERCEPTION_METADATA)
                            .name("interceptMeta"))
                    .addParameter(ctrParams -> ctrParams.type(TypeName.create("Object..."))
                            .name("params"))
                    .update(it -> createDoInstantiateBody(toInstantiate, it, params, interceptedMethods)));
        }
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

    private List<ParamDefinition> declareCtrParamsAndGetThem(Method.Builder method, List<ParamDefinition> params) {
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
            method.addLine(param.type().resolvedName() + " " + param.ipParamName() + " = ctx.param(" + param.constantName() + ")"
                                   + ";");
        }
        if (!params.isEmpty()) {
            method.addLine("");
        }
        return constructorParams;
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

    private void isAbstractMethod(ClassModel.Builder classModel, SuperType superType, boolean isAbstractClass) {
        if (!isAbstractClass && !superType.hasSupertype()) {
            return;
        }
        // only override for abstract types (and subtypes, where we do not want to check if super is abstract), default is false
        classModel.addMethod(isAbstract -> isAbstract
                .name("isAbstract")
                .returnType(TypeNames.PRIMITIVE_BOOLEAN)
                .addAnnotation(Annotations.OVERRIDE)
                .addLine("return " + isAbstractClass + ";"));
    }

    private void injectMethod(ClassModel.Builder classModel,
                              TypeName serviceType,
                              List<ParamDefinition> params,
                              SuperType superType,
                              List<MethodDefinition> methods, boolean canIntercept,
                              Set<TypedElementInfo> maybeIntercepted) {

        // method for field and method injections
        List<ParamDefinition> fields = params.stream()
                .filter(it -> it.kind == ElementKind.FIELD)
                .toList();
        classModel.addMethod(method -> method.addAnnotation(Annotations.OVERRIDE)
                .name("inject")
                .addParameter(ctxParam -> ctxParam.type(InjectCodegenTypes.HELIDON_INJECTION_CONTEXT)
                        .name("ctx"))
                .addParameter(interceptMeta -> interceptMeta.type(InjectCodegenTypes.HELIDON_INTERCEPTION_METADATA)
                        .name("interceptMeta"))
                .addParameter(injectedParam -> injectedParam.type(SET_OF_SIGNATURES)
                        .name("injected"))
                .addParameter(instanceParam -> instanceParam.type(GENERIC_T_TYPE)
                        .name("instance"))
                .update(it -> createInjectBody(it,
                                               serviceType,
                                               superType.hasSupertype(),
                                               methods,
                                               fields,
                                               canIntercept,
                                               maybeIntercepted)));
    }

    private void createInjectBody(Method.Builder methodBuilder,
                                  TypeName serviceType,
                                  boolean hasSuperType,
                                  List<MethodDefinition> methods,
                                  List<ParamDefinition> fields,
                                  boolean canIntercept,
                                  Set<TypedElementInfo> maybeIntercepted) {

        // two passes for methods - first mark method to be injected, then call super, then inject
        for (MethodDefinition method : methods) {
            if (method.isInjectionPoint()) {
                if (method.overrides()) {
                    // we are overriding something from supertype - need to keep that method in, and tell them we do it
                    methodBuilder.addLine("boolean " + method.invokeName() + " = injected.add(" + method.constantName() + ");");
                } else {
                    // we are not overriding method from supertype - need to remove that method if exists, so supertype
                    // can inject its own
                    methodBuilder.addLine("boolean " + method.invokeName() + " = !injected.remove(" + method.constantName() +
                                                  ");");
                }
            } else {
                methodBuilder.addLine("injected.add(" + method.constantName() + ");");
            }
        }
        methodBuilder.addLine("");

        if (hasSuperType) {
            // must be done at the very end, so the same method is not injected first in the supertype
            methodBuilder.addLine("super.inject(ctx, interceptMeta, injected, instance);");
            methodBuilder.addLine("");
        }

        /*
        Inject fields
         */
        for (ParamDefinition field : fields) {
            /*
            instance.myField  = ctx.param(IP_PARAM_X)
             */
            injectFieldBody(methodBuilder, field, canIntercept, maybeIntercepted);
        }

        if (!fields.isEmpty()) {
            methodBuilder.addLine("");
        }

        // now finally invoke the methods
        for (MethodDefinition method : methods) {
            if (!method.isInjectionPoint()) {
                // this method "disabled" injection point from superclass
                continue;
            }
            methodBuilder.addLine("if (" + method.invokeName() + ") {");
            for (ParamDefinition param : method.params()) {
                // MyType ipParam4_param = ctx.param(IP_PARAM_4);
                methodBuilder.addLine(param.type()
                                              .resolvedName() + " " + param.fieldId() + " = ctx.param(" + param.constantName() + ")"
                                              + ";");
            }

            // we need to explicitly cast the current instance to correctly invoke methods
            // on this instance, and not on subtype
            //            if (method.access() == AccessModifier.PACKAGE_PRIVATE) {
            //                // package private methods will only be called if the instance we are injecting si the same class
            //                methodBuilder.addLine("if (instance.getClass().equals(@" + serviceType.fqName() + "@.class)) {");
            //            }
            methodBuilder
                    .add("instance." + method.methodName() + "(")
                    .add(method.params().stream()
                                 .map(ParamDefinition::fieldId)
                                 .collect(Collectors.joining(", ")))
                    .addLine(");");
            //            if (method.access() == AccessModifier.PACKAGE_PRIVATE) {
            //                methodBuilder.addLine("}");
            //            }
            methodBuilder.addLine("}");
            methodBuilder.addLine("");
        }
    }

    private void injectFieldBody(Method.Builder methodBuilder,
                                 ParamDefinition field,
                                 boolean canIntercept,
                                 Set<TypedElementInfo> maybeIntercepted) {
        if (canIntercept && maybeIntercepted.contains(field.elementInfo())) {
            methodBuilder.addLine(field.type().resolvedName() + " "
                                          + field.ipParamName()
                                          + " = ctx.param(" + field.constantName() + ");");
            String interceptorsName = field.ipParamName() + "__interceptors";
            String constantName = fieldElementConstantName(field.ipParamName);
            methodBuilder.add("var ")
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
            methodBuilder.addLine("instance." + field.ipParamName() + " = ctx.param(" + field.constantName() + ");");
        }
    }

    private void postConstructMethod(TypeInfo typeInfo, ClassModel.Builder classModel, TypeName serviceType) {
        // postConstruct()
        lifecycleMethod(typeInfo, InjectCodegenTypes.INJECT_POST_CONSTRUCT).ifPresent(method -> {
            classModel.addMethod(postConstruct -> postConstruct.name("postConstruct")
                    .addAnnotation(Annotations.OVERRIDE)
                    .addParameter(instance -> instance.type(serviceType)
                            .name("instance"))
                    .addLine("instance." + method.elementName() + "();"));
        });
    }

    private void preDestroyMethod(TypeInfo typeInfo, ClassModel.Builder classModel, TypeName serviceType) {
        // preDestroy
        lifecycleMethod(typeInfo, InjectCodegenTypes.INJECT_PRE_DESTROY).ifPresent(method -> {
            classModel.addMethod(preDestroy -> preDestroy.name("preDestroy")
                    .addAnnotation(Annotations.OVERRIDE)
                    .addParameter(instance -> instance.type(serviceType)
                            .name("instance"))
                    .addLine("instance." + method.elementName() + "();"));
        });
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
        if (!method.typeName().equals(TypeNames.PRIMITIVE_VOID)) {
            throw new IllegalStateException("Method annotated with " + annotationType.fqName()
                                                    + ", is not void, which is not supported: " + typeInfo.typeName().fqName()
                                                    + "#" + method.elementName());
        }
        return Optional.of(method);
    }

    private void qualifiersMethod(ClassModel.Builder classModel, Set<Annotation> qualifiers, SuperType superType) {
        if (qualifiers.isEmpty() && !superType.hasSupertype()) {
            return;
        }
        // List<Qualifier> qualifiers()
        classModel.addMethod(qualifiersMethod -> qualifiersMethod.name("qualifiers")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(SET_OF_QUALIFIERS)
                .addLine("return QUALIFIERS;"));
    }

    private void scopesMethod(ClassModel.Builder classModel, Set<TypeName> scopes, SuperType superType) {
        if (!superType.hasSupertype()) {
            if (scopes.size() == 1 && scopes.contains(InjectCodegenTypes.INJECT_SINGLETON)) {
                // this is the default as returned from the parent
                return;
            }
        }
        // List<Qualifier> qualifiers()
        classModel.addMethod(scopesMethod -> scopesMethod.name("scopes")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(SET_OF_TYPES)
                .addLine("return SCOPES;"));
    }

    private void weightMethod(TypeInfo typeInfo, ClassModel.Builder classModel, SuperType superType) {
        boolean hasSuperType = superType.hasSupertype();
        // double weight()
        Optional<Double> weight = weight(typeInfo);

        if (!hasSuperType && weight.isEmpty()) {
            return;
        }
        double usedWeight = weight.orElse(Weighted.DEFAULT_WEIGHT);
        if (!hasSuperType && usedWeight == Weighted.DEFAULT_WEIGHT) {
            return;
        }

        classModel.addMethod(weightMethod -> weightMethod.name("weight")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.PRIMITIVE_DOUBLE)
                .addLine("return " + usedWeight + ";"));
    }

    private Optional<Double> weight(TypeInfo typeInfo) {
        return typeInfo.findAnnotation(TypeName.create(Weight.class))
                .flatMap(Annotation::doubleValue);
    }

    private void runLevelMethod(TypeInfo typeInfo, ClassModel.Builder classModel, SuperType superType) {
        boolean hasSuperType = superType.hasSupertype();
        // int runLevel()
        Optional<Integer> runLevel = runLevel(typeInfo);

        if (!hasSuperType && runLevel.isEmpty()) {
            return;
        }
        int usedRunLevel = runLevel.orElse(100); // normal run level
        if (!hasSuperType && usedRunLevel == 100) {
            return;
        }

        classModel.addMethod(runLevelMethod -> runLevelMethod.name("runLevel")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.PRIMITIVE_INT)
                .addLine("return " + usedRunLevel + ";"));
    }

    private Optional<Integer> runLevel(TypeInfo typeInfo) {
        return typeInfo.findAnnotation(InjectCodegenTypes.HELIDON_RUN_LEVEL)
                .flatMap(Annotation::intValue);
    }

    private void notifyObservers(RoundContext roundContext, Collection<TypeInfo> descriptorsRequired) {
        // we have correct classloader set in current thread context
        if (!observers.isEmpty()) {
            Set<TypedElementInfo> elements = descriptorsRequired.stream()
                    .flatMap(it -> it.elementInfo().stream())
                    .collect(Collectors.toSet());
            observers.forEach(it -> it.onProcessingEvent(ctx, roundContext, elements));
        }
    }

    private record GenericTypeDeclaration(String constantName,
                                          TypeName typeName) {
    }

    private record MethodDefinition(AccessModifier access,
                                    String methodId,
                                    String constantName,
                                    String methodName,
                                    boolean overrides,
                                    List<ParamDefinition> params,
                                    boolean isInjectionPoint) {
        public String invokeName() {
            return "invoke" + capitalize(methodId());
        }
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
                                   List<Annotation> annotations,
                                   Set<Annotation> qualifiers,
                                   TypeName contract,
                                   AccessModifier access,
                                   String methodId) {

    }

    private record SuperType(boolean hasSupertype,
                             TypeName superDescriptorType,
                             TypeInfo superType) {
        static SuperType noSuperType() {
            return new SuperType(false, null, null);
        }
    }
}
