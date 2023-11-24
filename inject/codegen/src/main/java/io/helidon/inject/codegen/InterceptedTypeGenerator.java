package io.helidon.inject.codegen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.codegen.CopyrightHandler;
import io.helidon.codegen.GeneratedAnnotationHandler;
import io.helidon.codegen.TypesCodeGen;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Constructor;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.codegen.CodegenUtil.toConstantName;

class InterceptedTypeGenerator {
    private static final TypeName GENERATOR = TypeName.create(InterceptedTypeGenerator.class);
    private static final TypeName INVOKER_TYPE = TypeName.create("io.helidon.inject.api.Invoker");
    private static final TypeName RUNTIME_EXCEPTION_TYPE = TypeName.create(RuntimeException.class);
    private final TypeName serviceType;
    private final TypeName descriptorType;
    private final TypeName interceptedType;
    private final TypedElementInfo constructor;
    private final List<MethodDefinition> interceptedMethods;

    public InterceptedTypeGenerator(TypeName serviceType,
                                    TypeName descriptorType,
                                    TypeName interceptedType,
                                    TypedElementInfo constructor,
                                    List<TypedElementInfo> interceptedMethods) {
        this.serviceType = serviceType;
        this.descriptorType = descriptorType;
        this.interceptedType = interceptedType;
        this.constructor = constructor;
        this.interceptedMethods = MethodDefinition.toDefinitions(interceptedMethods);
    }

    ClassModel.Builder generate() {
        ClassModel.Builder classModel = ClassModel.builder();

        classModel.copyright(CopyrightHandler.copyright(GENERATOR,
                                                        serviceType,
                                                        interceptedType))
                .addAnnotation(GeneratedAnnotationHandler.create(GENERATOR,
                                                                 serviceType,
                                                                 interceptedType,
                                                                 "1",
                                                                 ""))
                .description("Intercepted sub-type for {@link " + serviceType.fqName() + "}.")
                .type(interceptedType)
                .superType(serviceType);

        generateElementInfoFields(classModel);
        generateInvokerFields(classModel);

        generateConstructor(classModel);

        generateInterceptedMethods(classModel);

        return classModel;
    }

    private static TypeName invokerType(TypeName type) {
        return TypeName.builder(INVOKER_TYPE)
                .addTypeArgument(type.boxed())
                .build();
    }

    private void generateInterceptedMethods(ClassModel.Builder classModel) {
        for (MethodDefinition interceptedMethod : interceptedMethods) {
            TypedElementInfo info = interceptedMethod.info();
            String invoker = interceptedMethod.invokerName();

            classModel.addMethod(method -> method
                    .addAnnotation(Annotations.OVERRIDE)
                    .accessModifier(info.accessModifier())
                    .name(info.elementName())
                    .returnType(info.typeName())
                    .update(it -> info.parameterArguments().forEach(arg -> it.addParameter(param -> param.type(arg.typeName())
                            .name(arg.elementName()))))
                    .update(it -> {
                        // add throws statements
                        if (!interceptedMethod.exceptionTypes().isEmpty()) {
                            for (TypeName exceptionType : interceptedMethod.exceptionTypes()) {
                                it.addThrows(exceptionType, "thrown by intercepted method");
                            }
                        }
                    })
                    .update(it -> {
                        String invokeLine = invoker
                                + ".invoke("
                                + info.parameterArguments()
                                .stream().map(TypedElementInfo::elementName)
                                .collect(Collectors.joining(", "))
                                + ");";
                        // body of the method
                        it.addLine("try {")
                                .add(interceptedMethod.isVoid() ? "" : "return ")
                                .addLine(invokeLine)
                                .add("}");
                        for (TypeName exceptionType : interceptedMethod.exceptionTypes()) {
                            it.addLine(" catch (@" + exceptionType.fqName() + "@ helidonInject__e) {")
                                    .addLine(" throw helidonInject__e;")
                                    .add("}");

                        }
                        if (!interceptedMethod.exceptionTypes().contains(RUNTIME_EXCEPTION_TYPE)) {
                            it.addLine(" catch (@" + RuntimeException.class.getName() + "@ helidonInject__e) {")
                                    .addLine("throw helidonInject__e;")
                                    .add("}");
                        }
                        it.addLine(" catch (@" + Exception.class.getName() + "@ helidonInject__e) {")
                                .addLine("throw new @" + RuntimeException.class.getName() + "@(helidonInject__e);")
                                .addLine("}");

                    }));

        }
    }

    private void generateConstructor(ClassModel.Builder classModel) {
        classModel.addConstructor(constructor -> constructor
                .addParameter(interceptMeta -> interceptMeta.type(InjectCodegenTypes.HELIDON_INTERCEPTION_METADATA)
                        .name("helidonInject__interceptMeta"))
                .addParameter(descriptor -> descriptor.type(descriptorType)
                        .name("helidonInject__serviceDescriptor"))
                .addParameter(qualifiers -> qualifiers.type(InjectionExtension.SET_OF_QUALIFIERS)
                        .name("helidonInject__typeQualifiers"))
                .addParameter(qualifiers -> qualifiers.type(InjectionExtension.LIST_OF_ANNOTATIONS)
                        .name("helidonInject__typeAnnotations"))
                .update(this::addConstructorParameters)
                .update(this::callSuperConstructor)
                .update(this::createInvokers)
        );
    }

    private void createInvokers(Constructor.Builder cModel) {
        for (MethodDefinition interceptedMethod : interceptedMethods) {
            cModel.add("this.")
                    .add(interceptedMethod.invokerName)
                    .addLine(" = helidonInject__interceptMeta.createInvoker(")
                    .increasePadding()
                    .addLine("helidonInject__serviceDescriptor,")
                    .addLine("helidonInject__typeQualifiers,")
                    .addLine("helidonInject__typeAnnotations,")
                    .add(interceptedMethod.constantName())
                    .addLine(",")
                    .add("helidonInject__params -> ");
            if (interceptedMethod.isVoid()) {
                cModel.addLine("{");
            }
            cModel.add("super.")
                    .add(interceptedMethod.info().elementName())
                    .add("(");

            List<String> allArgs = new ArrayList<>();
            List<TypedElementInfo> args = interceptedMethod.info().parameterArguments();
            for (int i = 0; i < args.size(); i++) {
                TypedElementInfo arg = args.get(i);
                allArgs.add("(" + arg.typeName().resolvedName() + ") helidonInject__params[" + i + "]");
            }
            cModel.add(String.join(", ", allArgs));
            cModel.add(")");

            if (interceptedMethod.isVoid()) {
                cModel.addLine(";");
                cModel.addLine("return null;");
                cModel.add("}");
            }
            cModel.add(", @java.util.Set@.of(")
                    .add(interceptedMethod.exceptionTypes()
                                 .stream()
                                 .map(it -> it.fqName() + ".class")
                                 .collect(Collectors.joining(", ")))
                    .addLine("));")
                    .decreasePadding();
        }
    }

    private void callSuperConstructor(Constructor.Builder cModel) {
        cModel.add("super(");
        cModel.add(this.constructor.parameterArguments()
                           .stream()
                           .map(TypedElementInfo::elementName)
                           .collect(Collectors.joining(", ")));
        cModel.addLine(");");
        cModel.addLine("");
    }

    private void addConstructorParameters(Constructor.Builder cModel) {
        // for each constructor parameter, add it as is (same type and name as super type)
        // this will not create conflicts, unless somebody names their constructor parameters same
        // as the ones above (which we will not do, and others should not do)
        this.constructor.parameterArguments().forEach(constructorArg -> {
            cModel.addParameter(generatedCtrParam -> generatedCtrParam.type(constructorArg.typeName())
                    .name(constructorArg.elementName()));
        });
    }

    private void generateInvokerFields(ClassModel.Builder classModel) {
        for (MethodDefinition interceptedMethod : interceptedMethods) {
            classModel.addField(methodField -> methodField
                    .accessModifier(AccessModifier.PRIVATE)
                    .isFinal(true)
                    .type(invokerType(interceptedMethod.info().typeName()))
                    .name(interceptedMethod.invokerName()));
        }
    }

    private void generateElementInfoFields(ClassModel.Builder classModel) {
        for (MethodDefinition interceptedMethod : interceptedMethods) {
            classModel.addField(methodField -> methodField
                    .accessModifier(AccessModifier.PRIVATE)
                    .isStatic(true)
                    .isFinal(true)
                    .type(TypeNames.HELIDON_TYPED_ELEMENT_INFO)
                    .name(interceptedMethod.constantName())
                    .defaultValueContent(TypesCodeGen.toCreate(interceptedMethod.info())));
        }
    }

    private record MethodDefinition(TypedElementInfo info,
                                    String constantName,
                                    String invokerName,
                                    boolean isVoid,
                                    Set<TypeName> exceptionTypes) {

        public static List<MethodDefinition> toDefinitions(List<TypedElementInfo> interceptedMethods) {
            List<TypedElementInfo> sortedMethods = new ArrayList<>(interceptedMethods);
            // order must be fixed
            sortedMethods.sort((first, second) -> {
                int compare = first.elementName().compareTo(second.elementName());
                if (compare != 0) {
                    return compare;
                }
                compare = Integer.compare(first.parameterArguments().size(), second.parameterArguments().size());
                if (compare != 0) {
                    return compare;
                }
                for (int i = 0; i < first.parameterArguments().size(); i++) {
                    compare = first.parameterArguments().get(i).elementName()
                            .compareTo(second.parameterArguments().get(i).elementName());
                    if (compare != 0) {
                        return compare;
                    }
                }
                return 0;
            });

            List<MethodDefinition> result = new ArrayList<>();
            for (int i = 0; i < sortedMethods.size(); i++) {
                TypedElementInfo typedElementInfo = sortedMethods.get(i);

                String constantName =
                        "METHOD_" + i + "_" + toConstantName(typedElementInfo.elementName());
                String invokerName = typedElementInfo.elementName() + "_" + i + "_invoker";

                result.add(new MethodDefinition(typedElementInfo,
                                                constantName,
                                                invokerName,
                                                TypeNames.PRIMITIVE_VOID.equals(typedElementInfo.typeName()),
                                                typedElementInfo.throwsChecked()));
            }
            result.sort(Comparator.comparing(o -> o.invokerName));
            return result;
        }
    }
}
