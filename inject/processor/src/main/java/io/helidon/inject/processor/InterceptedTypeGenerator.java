package io.helidon.inject.processor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.common.codegen.TypesCodeGen;
import io.helidon.common.processor.CopyrightHandler;
import io.helidon.common.processor.GeneratedAnnotationHandler;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.processor.classmodel.Constructor;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.inject.processor.InjectionProcessorExtension.LIST_OF_ANNOTATIONS_TYPE;
import static io.helidon.inject.processor.InjectionProcessorExtension.SET_OF_QUALIFIERS_TYPE;

class InterceptedTypeGenerator {
    private static final TypeName GENERATOR = TypeName.create(InterceptedTypeGenerator.class);
    private static final TypeName INVOKER_TYPE = TypeName.create("io.helidon.inject.api.Invoker");
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
                    .accessModifier(info.accessModifier())
                    .name(info.elementName())
                    .returnType(info.typeName())
                    .update(it -> info.parameterArguments().forEach(arg -> it.addParameter(param -> param.type(arg.typeName())
                            .name(arg.elementName()))))
                    .update(it -> it.add(interceptedMethod.isVoid() ? "" : "return "))
                    .addLine(invoker
                                     + ".invoke("
                                     + info.parameterArguments()
                            .stream().map(TypedElementInfo::elementName)
                            .collect(Collectors.joining(", "))
                                     + ");"));
        }
    }

    private void generateConstructor(ClassModel.Builder classModel) {
        classModel.addConstructor(constructor -> constructor
                .addParameter(interceptMeta -> interceptMeta.type(io.helidon.inject.tools.TypeNames.INTERCEPTION_METADATA)
                        .name("helidonInject__interceptMeta"))
                .addParameter(descriptor -> descriptor.type(descriptorType)
                        .name("helidonInject__serviceDescriptor"))
                .addParameter(qualifiers -> qualifiers.type(SET_OF_QUALIFIERS_TYPE)
                        .name("helidonInject__typeQualifiers"))
                .addParameter(qualifiers -> qualifiers.type(LIST_OF_ANNOTATIONS_TYPE)
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
            cModel.addLine(");")
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
                    .type(TypeNames.TYPED_ELEMENT_INFO)
                    .name(interceptedMethod.constantName())
                    .defaultValueContent(TypesCodeGen.toCreate(interceptedMethod.info())));
        }
    }

    private record MethodDefinition(TypedElementInfo info,
                                    String constantName,
                                    String invokerName,
                                    boolean isVoid) {

        public static List<MethodDefinition> toDefinitions(List<TypedElementInfo> interceptedMethods) {
            List<MethodDefinition> result = new ArrayList<>();
            for (int i = 0; i < interceptedMethods.size(); i++) {
                TypedElementInfo typedElementInfo = interceptedMethods.get(i);

                String constantName =
                        "METHOD_" + i + "_" + InjectionProcessorExtension.toConstantName(typedElementInfo.elementName());
                String invokerName = typedElementInfo.elementName() + "_" + i + "_invoker";

                result.add(new MethodDefinition(typedElementInfo,
                                                constantName,
                                                invokerName,
                                                TypeNames.PRIMITIVE_VOID.equals(typedElementInfo.typeName())));
            }
            result.sort(Comparator.comparing(o -> o.invokerName));
            return result;
        }
    }
}
