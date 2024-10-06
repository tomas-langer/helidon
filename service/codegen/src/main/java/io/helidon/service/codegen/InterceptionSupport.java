/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.service.codegen;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.CodegenUtil;
import io.helidon.codegen.ElementInfoPredicates;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.ElementSignature;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.service.codegen.InjectCodegenTypes.INJECT_SERVICE_DESCRIPTOR;
import static io.helidon.service.codegen.InjectCodegenTypes.INTERCEPT_METADATA;
import static java.util.function.Predicate.not;

final class InterceptionSupport {
    private static final TypeName GENERATOR = TypeName.create(InterceptionSupport.class);
    private static final TypeName DESCRIPTOR_TYPE = TypeName.builder(INJECT_SERVICE_DESCRIPTOR)
            .addTypeArgument(TypeName.create("?"))
            .build();
    private static final String INTERCEPT_META_PARAM = "interceptMeta";
    private static final String DESCRIPTOR_PARAM = "descriptor";
    private static final String TYPE_ANNOTATIONS_FIELD = "ANNOTATIONS";
    private static final String DELEGATE_PARAM = "delegate";

    private final RegistryCodegenContext ctx;
    private final Interception interception;

    private InterceptionSupport(RegistryCodegenContext ctx, Interception interception) {
        this.ctx = ctx;
        this.interception = interception;
    }

    /**
     * Create a new instance.
     *
     * @param ctx codegen context
     * @return a new interception support instance
     */
    static InterceptionSupport create(RegistryCodegenContext ctx) {
        return new InterceptionSupport(ctx,
                                       new Interception(ctx, InjectOptions.INTERCEPTION_STRATEGY.value(ctx.options())));
    }

    /**
     * Generates required code to handle interception by delegation.
     * <p>
     * Creates the following types:
     * <ul>
     *     <li>Type__InterceptedDelegate - the type implementing interception, package private</li>
     * </ul>
     *
     * Usage: When processing types, you can call this method to generate the types above. This is not done automatically,
     * as we do not know which interface is the "right" one to intercept (there may be a generated type, or it may be implemented
     * directly by a service, in which case the interception is generated by Helidon automatically).
     * The steps you need to do:
     * <ol>
     *     <li>Invoke {@code Contract__InterceptedDelegate.create(interceptMeta, ServiceDescriptor.INSTANCE, instance)}
     *     to wrap your instance, the service descriptor must be your descriptor that describes the service</li>
     * </ol>
     *
     * @param typeInfo        interface type info that will be intercepted
     * @param interceptedType type of the interface (or class) used for interception (may differ from typeInfo type)
     * @param packageName     package to generate the delegate into (may differ from type, when using external delegates)
     * @return type name of the generated delegate implementation
     * @throws io.helidon.codegen.CodegenException in case the type is not an interface
     */
    TypeName generateDelegateInterception(RegistryRoundContext roundContext,
                                                 TypeInfo typeInfo,
                                                 TypeName interceptedType,
                                                 String packageName) {
        boolean samePackage = packageName.equals(interceptedType.packageName());

        List<TypedElements.ElementMeta> elementMetas = interception.maybeIntercepted(typeInfo);
        Set<ElementSignature> interceptedSignatures = new HashSet<>();
        elementMetas.stream()
                .map(TypedElements.ElementMeta::element)
                .peek(it -> {
                    if (samePackage) {
                        return;
                    }
                    if (it.accessModifier() != AccessModifier.PUBLIC) {
                        throw new CodegenException("Cannot generate interception delegate for a non-public method"
                                                           + " when the delegate is in a different package",
                                                   it.originatingElementValue());
                    }
                })
                .map(TypedElementInfo::signature)
                .forEach(interceptedSignatures::add);
        List<TypedElementInfo> otherMethods = typeInfo.elementInfo()
                .stream()
                .filter(ElementInfoPredicates::isMethod)
                .filter(not(ElementInfoPredicates::isStatic))
                .filter(not(ElementInfoPredicates::isPrivate))
                .filter(it -> {
                    if (samePackage) {
                        return true;
                    }
                    return ElementInfoPredicates.isPublic(it);
                })
                .filter(it -> !interceptedSignatures.contains(it.signature()))
                .collect(Collectors.toUnmodifiableList());

        TypeName generatedType = interceptedDelegateType(interceptedType);
        generatedType = TypeName.builder()
                .from(generatedType)
                .packageName(packageName)
                .build();

        if (typeInfo.kind() == ElementKind.INTERFACE) {
            return generateInterceptionDelegateInterface(roundContext,
                                                         typeInfo,
                                                         interceptedType,
                                                         generatedType,
                                                         elementMetas,
                                                         otherMethods);
        } else {
            return generateInterceptionDelegateClass(roundContext,
                                                     typeInfo,
                                                     interceptedType,
                                                     generatedType,
                                                     elementMetas,
                                                     otherMethods);
        }

    }

    /**
     * Type name for the intercepted delegate generated type.
     *
     * @param interfaceType type of the interface
     * @return type name that will be generated for it
     */
    TypeName interceptedDelegateType(TypeName interfaceType) {
        return TypeName.builder()
                .packageName(interfaceType.packageName())
                .className(interfaceType.classNameWithEnclosingNames().replace('.', '_') + "__InterceptedDelegate")
                .build();
    }

    void generateDelegateInterception(RegistryRoundContext roundContext,
                                      TypeInfo typeInfo,
                                      DescribedElements describedElements,
                                      TypeName interceptionDelegateType) {
        TypeName interceptedTypeName = typeInfo.typeName();
        boolean samePackage = interceptedTypeName.packageName().equals(interceptionDelegateType.packageName());

        // validate we can generate everything
        describedElements.interceptedElements()
                .stream()
                .forEach(it -> {
                    if (ElementInfoPredicates.isStatic(it.element())) {
                        throw new CodegenException("Interception of static methods is not possible",
                                                   it.element().originatingElementValue());
                    }
                    if (it.element().accessModifier() == AccessModifier.PRIVATE) {
                        throw new CodegenException("Cannot generate interception delegate for a private method: ",
                                                   it.element().originatingElementValue());
                    }
                    if (samePackage) {
                        return;
                    }
                    Set<AccessModifier> allowedModifiers = EnumSet.of(AccessModifier.PUBLIC);
                    if (typeInfo.kind() == ElementKind.CLASS) {
                        allowedModifiers.add(AccessModifier.PROTECTED);
                    }
                    if (!allowedModifiers.contains(it.element().accessModifier())) {
                        throw new CodegenException("Cannot generate interception delegate for a method that is not public"
                                                           + " or protected, when"
                                                           + " the delegate is in a different package than the intercepted type"
                                                           + " intercepted type: " + interceptedTypeName.fqName()
                                                           + ", delegate: " + interceptionDelegateType.fqName(),
                                                   it.element().originatingElementValue());
                    }
                });
        // now we are sure we can implement all intercepted methods
        List<TypedElements.ElementMeta> interceptedMethods = describedElements.interceptedElements()
                .stream()
                .collect(Collectors.toUnmodifiableList());
        List<TypedElementInfo> plainSignatures = describedElements.plainElements()
                .stream()
                .map(TypedElements.ElementMeta::element)
                .filter(ElementInfoPredicates::isMethod)
                .filter(not(ElementInfoPredicates::isStatic))
                .filter(not(ElementInfoPredicates::isPrivate))
                .filter(it -> {
                    if (samePackage) {
                        return true;
                    }
                    return ElementInfoPredicates.isPublic(it);
                })
                .collect(Collectors.toUnmodifiableList());

        if (typeInfo.kind() == ElementKind.INTERFACE) {
            generateInterceptionDelegateInterface(roundContext,
                                                  typeInfo,
                                                  interceptedTypeName,
                                                  interceptionDelegateType,
                                                  interceptedMethods,
                                                  plainSignatures);
        } else {
            generateInterceptionDelegateClass(roundContext,
                                              typeInfo,
                                              interceptedTypeName,
                                              interceptionDelegateType,
                                              interceptedMethods,
                                              plainSignatures);
        }
    }

    private TypeName generateInterceptionDelegateClass(RegistryRoundContext roundContext,
                                                       TypeInfo typeInfo,
                                                       TypeName classType,
                                                       TypeName generatedType,
                                                       List<TypedElements.ElementMeta> elementMetas,
                                                       List<TypedElementInfo> otherMethods) {

        Optional<TypedElementInfo> foundCtr = typeInfo.elementInfo()
                .stream()
                .filter(ElementInfoPredicates::isConstructor)
                .filter(ElementInfoPredicates.hasParams())
                .filter(not(ElementInfoPredicates::isPrivate))
                .findFirst();

        if (foundCtr.isEmpty()) {
            throw new CodegenException("Interception delegate requires accessible no-arg constructor.",
                                       typeInfo.originatingElementValue());
        }

        var definitions = InterceptedTypeGenerator.MethodDefinition.toDefinitions(ctx, typeInfo, elementMetas);

        var classModel = ClassModel.builder()
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .superType(classType)
                .type(generatedType)
                .copyright(CodegenUtil.copyright(GENERATOR,
                                                 classType,
                                                 generatedType))
                .description("Intercepted class implementation, that delegates to the provided instance.")
                .addAnnotation(CodegenUtil.generatedAnnotation(GENERATOR,
                                                               classType,
                                                               generatedType,
                                                               "",
                                                               ""));

        // add type annotations
        InjectionExtension.annotationsField(classModel, typeInfo);
        // this is a special case, where we may not have the correct descriptor
        InterceptedTypeGenerator.generateElementInfoFields(classModel, definitions);
        InterceptedTypeGenerator.generateInvokerFields(classModel, definitions);
        InterceptedTypeGenerator.generateInterceptedMethods(classModel, definitions);
        generateOtherMethods(classModel, otherMethods);

        classModel.addField(delegate -> delegate
                .name(DELEGATE_PARAM)
                .accessModifier(AccessModifier.PRIVATE)
                .isFinal(true)
                .type(classType));

        classModel.addConstructor(ctr -> ctr
                .accessModifier(AccessModifier.PRIVATE)
                .addParameter(interceptMeta -> interceptMeta
                        .type(INTERCEPT_METADATA)
                        .name(INTERCEPT_META_PARAM))
                .addParameter(descriptor -> descriptor
                        .type(DESCRIPTOR_TYPE)
                        .name(DESCRIPTOR_PARAM))
                .addParameter(delegate -> delegate
                        .type(classType)
                        .name(DELEGATE_PARAM))
                .addContentLine("// no-arg constructor is required for delegation")
                .addContentLine("super();")
                .addContentLine("")
                .addContent("this.")
                .addContent(DELEGATE_PARAM)
                .addContent(" = ")
                .addContent(DELEGATE_PARAM)
                .addContentLine(";")
                .update(it -> InterceptedTypeGenerator.createInvokers(it,
                                                                      definitions,
                                                                      false,
                                                                      INTERCEPT_META_PARAM,
                                                                      DESCRIPTOR_PARAM,
                                                                      DESCRIPTOR_PARAM + ".qualifiers()",
                                                                      TYPE_ANNOTATIONS_FIELD,
                                                                      DELEGATE_PARAM)));

        // and finally the create method (to be invoked by user code)
        classModel.addMethod(create -> create
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .isStatic(true)
                .returnType(classType)
                .name("create")
                .addParameter(interceptMeta -> interceptMeta
                        .type(INTERCEPT_METADATA)
                        .name(INTERCEPT_META_PARAM))
                .addParameter(descriptor -> descriptor
                        .type(DESCRIPTOR_TYPE)
                        .name(DESCRIPTOR_PARAM))
                .addParameter(delegate -> delegate
                        .type(classType)
                        .name(DELEGATE_PARAM))
                .addContent("return new ")
                .addContent(generatedType)
                .addContent("(")
                .addContent(INTERCEPT_META_PARAM)
                .addContentLine(",")
                .increaseContentPadding()
                .increaseContentPadding()
                .addContent(DESCRIPTOR_PARAM)
                .addContentLine(",")
                .addContent(DELEGATE_PARAM)
                .addContentLine(");")
        );

        roundContext.addGeneratedType(generatedType, classModel, classType, typeInfo.originatingElementValue());

        return generatedType;
    }

    private TypeName generateInterceptionDelegateInterface(RegistryRoundContext roundContext,
                                                           TypeInfo typeInfo,
                                                           TypeName interfaceType,
                                                           TypeName generatedType,
                                                           List<TypedElements.ElementMeta> elementMetas,
                                                           List<TypedElementInfo> otherMethods) {

        var definitions = InterceptedTypeGenerator.MethodDefinition.toDefinitions(ctx, typeInfo, elementMetas);

        var classModel = ClassModel.builder()
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .addInterface(interfaceType)
                .type(generatedType)
                .copyright(CodegenUtil.copyright(GENERATOR,
                                                 interfaceType,
                                                 generatedType))
                .description("Intercepted interface implementation, that delegates to the provided instance.")
                .addAnnotation(CodegenUtil.generatedAnnotation(GENERATOR,
                                                               interfaceType,
                                                               generatedType,
                                                               "",
                                                               ""));

        // add type annotations
        InjectionExtension.annotationsField(classModel, typeInfo);
        // this is a special case, where we may not have the correct descriptor
        InterceptedTypeGenerator.generateElementInfoFields(classModel, definitions);
        InterceptedTypeGenerator.generateInvokerFields(classModel, definitions);
        InterceptedTypeGenerator.generateInterceptedMethods(classModel, definitions);
        generateOtherMethods(classModel, otherMethods);

        classModel.addField(delegate -> delegate
                .name(DELEGATE_PARAM)
                .accessModifier(AccessModifier.PRIVATE)
                .isFinal(true)
                .type(interfaceType));

        classModel.addConstructor(ctr -> ctr
                .accessModifier(AccessModifier.PRIVATE)
                .addParameter(interceptMeta -> interceptMeta
                        .type(INTERCEPT_METADATA)
                        .name(INTERCEPT_META_PARAM))
                .addParameter(descriptor -> descriptor
                        .type(DESCRIPTOR_TYPE)
                        .name(DESCRIPTOR_PARAM))
                .addParameter(delegate -> delegate
                        .type(interfaceType)
                        .name(DELEGATE_PARAM))
                .addContent("this.")
                .addContent(DELEGATE_PARAM)
                .addContent(" = ")
                .addContent(DELEGATE_PARAM)
                .addContentLine(";")
                .update(it -> InterceptedTypeGenerator.createInvokers(it,
                                                                      definitions,
                                                                      false,
                                                                      INTERCEPT_META_PARAM,
                                                                      DESCRIPTOR_PARAM,
                                                                      DESCRIPTOR_PARAM + ".qualifiers()",
                                                                      TYPE_ANNOTATIONS_FIELD,
                                                                      DELEGATE_PARAM)));

        // and finally the create method (to be invoked by user code)
        classModel.addMethod(create -> create
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .isStatic(true)
                .returnType(interfaceType)
                .name("create")
                .addParameter(interceptMeta -> interceptMeta
                        .type(INTERCEPT_METADATA)
                        .name(INTERCEPT_META_PARAM))
                .addParameter(descriptor -> descriptor
                        .type(DESCRIPTOR_TYPE)
                        .name(DESCRIPTOR_PARAM))
                .addParameter(delegate -> delegate
                        .type(interfaceType)
                        .name(DELEGATE_PARAM))
                .addContent("return new ")
                .addContent(generatedType)
                .addContent("(")
                .addContent(INTERCEPT_META_PARAM)
                .addContentLine(",")
                .increaseContentPadding()
                .increaseContentPadding()
                .addContent(DESCRIPTOR_PARAM)
                .addContentLine(",")
                .addContent(DELEGATE_PARAM)
                .addContentLine(");")
        );

        roundContext.addGeneratedType(generatedType, classModel, interfaceType, typeInfo.originatingElementValue());

        return generatedType;
    }

    private void generateOtherMethods(ClassModel.Builder classModel, List<TypedElementInfo> otherMethods) {
        for (TypedElementInfo info : otherMethods) {
            classModel.addMethod(method -> method
                    .addAnnotation(Annotations.OVERRIDE)
                    .name(info.elementName())
                    .accessModifier(info.accessModifier())
                    .name(info.elementName())
                    .returnType(info.typeName())
                    .update(it -> info.parameterArguments().forEach(arg -> it.addParameter(param -> param.type(arg.typeName())
                            .name(arg.elementName()))))
                    .update(it -> {
                        // add throws statements
                        for (TypeName checkedException : info.throwsChecked()) {
                            it.addThrows(checkedException, "");
                        }
                    })
                    .update(it -> {
                        // body
                        if (!info.typeName().equals(TypeNames.PRIMITIVE_VOID)) {
                            it.addContent("return ");
                        }
                        it.addContent(DELEGATE_PARAM)
                                .addContent(".")
                                .addContent(info.elementName())
                                .addContent("(");

                        it.addContent(info.parameterArguments()
                                              .stream()
                                              .map(TypedElementInfo::elementName)
                                              .collect(Collectors.joining(", ")));

                        it.addContentLine(");");
                    }));
        }

    }
}
