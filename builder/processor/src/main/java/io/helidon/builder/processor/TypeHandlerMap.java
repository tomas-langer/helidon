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

package io.helidon.builder.processor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Field;
import io.helidon.codegen.classmodel.InnerClass;
import io.helidon.codegen.classmodel.Javadoc;
import io.helidon.codegen.classmodel.Method;
import io.helidon.codegen.classmodel.TypeArgument;
import io.helidon.common.types.TypeName;

import static io.helidon.builder.processor.Types.STRING_TYPE;
import static io.helidon.codegen.CodegenUtil.capitalize;
import static io.helidon.common.types.TypeNames.LIST;
import static io.helidon.common.types.TypeNames.MAP;
import static io.helidon.common.types.TypeNames.OBJECT;
import static io.helidon.common.types.TypeNames.SET;

class TypeHandlerMap extends TypeHandler {
    private static final TypeName SAME_GENERIC_TYPE = TypeName.createFromGenericDeclaration("TYPE");
    private final TypeName actualType;
    private final TypeName implTypeName;
    private final boolean sameGeneric;

    TypeHandlerMap(String name, String getterName, String setterName, TypeName declaredType, boolean sameGeneric) {
        super(name, getterName, setterName, declaredType);
        this.sameGeneric = sameGeneric;

        this.implTypeName = collectionImplType(MAP);
        if (declaredType.typeArguments().size() < 2) {
            this.actualType = STRING_TYPE;
        } else {
            this.actualType = declaredType.typeArguments().get(1);
        }
    }

    @Override
    Field.Builder fieldDeclaration(AnnotationDataOption configured, boolean isBuilder, boolean alwaysFinal) {
        Field.Builder builder = super.fieldDeclaration(configured, isBuilder, true);
        if (isBuilder && !configured.hasDefault()) {
            builder.defaultValue("new " + ClassModel.TYPE_TOKEN + implTypeName.fqName() + ClassModel.TYPE_TOKEN + "<>()");
        }
        return builder;
    }

    @Override
    String toDefaultValue(List<String> defaultValues,
                          List<Integer> defaultInts,
                          List<Long> defaultLongs,
                          List<Double> defaultDoubles,
                          List<Boolean> defaultBooleans) {

        if (defaultValues != null) {
            if (defaultValues.size() % 2 != 0) {
                throw new IllegalArgumentException("Default value for a map does not have even number of entries:"
                                                           + defaultValues);
            }

            String[] defaults = new String[defaultValues.size()];
            for (int i = 1; i < defaultValues.size(); i = i + 2) {
                defaults[i - 1] = "\"" + defaultValues.get(i - 1) + "\"";
                defaults[i] = super.toDefaultValue(defaultValues.get(i));
            }

            return "java.util.Map.of(" + String.join(", ", defaults) + ")";
        }

        return null;
    }

    @Override
    TypeName actualType() {
        return actualType;
    }

    @Override
    void generateFromConfig(Method.Builder method,
                            AnnotationDataOption configured,
                            FactoryMethods factoryMethods) {
        method.addLine(configGet(configured)
                               + ".asNodeList().ifPresent(nodes -> nodes.forEach"
                               + "(node -> "
                               + name() + ".put(node.get(\"name\").asString().orElse(node.name()), node"
                               + generateFromConfig(factoryMethods)
                               + ".get())));");
    }

    @Override
    TypeName argumentTypeName() {
        return TypeName.builder(MAP)
                .addTypeArgument(toWildcard(declaredType().typeArguments().get(0)))
                .addTypeArgument(toWildcard(declaredType().typeArguments().get(1)))
                .build();
    }

    @SuppressWarnings("checkstyle:MethodLength") // will be shorter when we switch to class model
    @Override
    void setters(InnerClass.Builder classBuilder,
                 AnnotationDataOption configured,
                 FactoryMethods factoryMethod,
                 TypeName returnType,
                 Javadoc blueprintJavadoc) {

        declaredSetter(classBuilder, configured, returnType, blueprintJavadoc);
        declaredSetterAdd(classBuilder, configured, returnType, blueprintJavadoc);

        if (factoryMethod.createTargetType().isPresent()) {
            // factory method
            FactoryMethods.FactoryMethod fm = factoryMethod.createTargetType().get();
            String name = name();
            String argumentName = name + "Config";
            classBuilder.addMethod(builder -> {
                builder.name(name + "Config")
                        .description(blueprintJavadoc.content())
                        .accessModifier(setterAccessModifier(configured))
                        .addDescriptionLine("This method keeps existing values, then puts all new values into the map.")
                        .addParameter(param -> param.name(argumentName)
                                .type(fm.argumentType())
                                .description(blueprintJavadoc.returnDescription()))
                        .addJavadocTag("see", "#" + getterName() + "()")
                        .returnType(returnType, "updated builder instance")
                        .typeName(Objects.class)
                        .addLine(".requireNonNull(" + argumentName + ");")
                        .addLine("this." + name + ".clear();")
                        .add("this." + name + ".putAll(")
                        .typeName(fm.typeWithFactoryMethod().genericTypeName())
                        .addLine("." + fm.createMethodName() + "(" + argumentName + "));")
                        .addLine("return self();");
            });
        }

        TypeName keyType = declaredType().typeArguments().get(0);

        if (configured.singular() && isCollection(actualType())) {
            // value is a collection as well, we need to generate `add` methods for adding a single value, and adding
            // collection values
            // builder.addValue(String key, String value)
            // builder.addValues(String key, Set<String> values)
            String singularName = configured.singularName();
            setterAddValueToCollection(classBuilder,
                                       configured,
                                       singularName,
                                       keyType,
                                       actualType().typeArguments().get(0),
                                       returnType,
                                       blueprintJavadoc);

            setterAddValuesToCollection(classBuilder,
                                        configured,
                                        "add" + capitalize(name()),
                                        keyType,
                                        returnType,
                                        blueprintJavadoc);
        }
        if (configured.singular()) {
            // Builder putValue(String key, String value)
            String singularName = configured.singularName();
            String methodName = "put" + capitalize(singularName);

            Method.Builder method = Method.builder()
                    .name(methodName)
                    .accessModifier(setterAccessModifier(configured))
                    .returnType(returnType, "updated builder instance")
                    .description(blueprintJavadoc.content())
                    .addDescriptionLine("This method adds a new value to the map, or replaces it if the key already exists.")
                    .addJavadocTag("see", "#" + getterName() + "()");
            if (sameGeneric) {
                sameGenericArgs(method, keyType, singularName, actualType());
            } else {
                method.addParameter(param -> param.name("key")
                                .type(keyType)
                                .description("key to add or replace"))
                        .addParameter(param -> param.name(singularName)
                                .type(actualType())
                                .description("new value for the key"));
            }
            method.typeName(Objects.class)
                    .addLine(".requireNonNull(key);")
                    .typeName(Objects.class)
                    .addLine(".requireNonNull(" + singularName + ");")
                    .add("this." + name() + ".put(key, ");
            secondArgToPut(method, actualType(), singularName);
            method.addLine(");")
                    .addLine("return self();");

            classBuilder.addMethod(method);

            if (factoryMethod.builder().isPresent()) {
                FactoryMethods.FactoryMethod fm = factoryMethod.builder().get();
                TypeName builderType;
                if (fm.factoryMethodReturnType().className().equals("Builder")) {
                    builderType = fm.factoryMethodReturnType();
                } else {
                    builderType = TypeName.create(fm.factoryMethodReturnType().fqName() + ".Builder");
                }
                classBuilder.addMethod(builder -> builder.name(methodName)
                        .accessModifier(setterAccessModifier(configured))
                        .returnType(returnType, "updated builder instance")
                        .description(blueprintJavadoc.content())
                        .addDescriptionLine("This method adds a new value to the map, or replaces it if the key already exists.")
                        .addJavadocTag("see", "#" + getterName() + "()")
                        .addParameter(param -> param.name("key")
                                .type(keyType)
                                .description("key to add or replace"))
                        .addParameter(param -> param.name("consumer")
                                .type(TypeName.builder()
                                              .type(Consumer.class)
                                              .addTypeArgument(builderType)
                                              .build())
                                .description("builder consumer to create new value for the key"))
                        .typeName(Objects.class)
                        .addLine(".requireNonNull(key);")
                        .typeName(Objects.class)
                        .addLine(".requireNonNull(consumer);")
                        .add("var builder = ")
                        .typeName(fm.typeWithFactoryMethod().genericTypeName())
                        .addLine("." + fm.createMethodName() + "();")
                        .addLine("consumer.accept(builder);")
                        .addLine("this." + methodName + "(key, builder.build());")
                        .addLine("return self();"));
            }
        }
    }

    @Override
    protected void declaredSetter(InnerClass.Builder classBuilder,
                                  AnnotationDataOption configured,
                                  TypeName returnType,
                                  Javadoc blueprintJavadoc) {
        // declared type (such as Map<String, String>) - replace content
        classBuilder.addMethod(builder -> builder.name(setterName())
                .returnType(returnType, "updated builder instance")
                .description(blueprintJavadoc.content())
                .addDescriptionLine("This method replaces all values with the new ones.")
                .addJavadocTag("see", "#" + getterName() + "()")
                .addParameter(param -> param.name(name())
                        .type(argumentTypeName())
                        .description(blueprintJavadoc.returnDescription()))
                .accessModifier(setterAccessModifier(configured))
                .typeName(Objects.class)
                .addLine(".requireNonNull(" + name() + ");")
                .addLine("this." + name() + ".clear();")
                .addLine("this." + name() + ".putAll(" + name() + ");")
                .addLine("return self();"));
    }

    private void sameGenericArgs(Method.Builder method,
                                 TypeName keyType,
                                 String value,
                                 TypeName valueType) {

        String typeDeclaration;
        TypeName genericTypeBase;
        TypeName resolvedKeyType;
        TypeName resolvedValueType;

        if (keyType.typeArguments().isEmpty()) {
            /*
            Map<Object, List<Object>>
            <TYPE extends Object> put(TYPE, List<TYPE>)
             */
            // this is good
            genericTypeBase = keyType;
            resolvedKeyType = SAME_GENERIC_TYPE;
        } else if (keyType.typeArguments().size() == 1) {
            /*
            Map<Class<Provider>, Provider>
            <TYPE extends Provider> put(Class<TYPE>, List<TYPE>)
             */
            // this is also good
            TypeName typeArg = keyType.typeArguments().get(0);
            if (typeArg.wildcard()) {
                // ?, or ? extends Something
                if (typeArg.generic()) {
                    genericTypeBase = OBJECT;
                } else {
                    genericTypeBase = TypeName.builder(typeArg)
                            .wildcard(false)
                            .build();
                }
            } else {
                genericTypeBase = typeArg;
            }
            resolvedKeyType = TypeName.builder(keyType)
                    .typeArguments(List.of(SAME_GENERIC_TYPE))
                    .build();
        } else {
            throw new IllegalArgumentException("Property " + name() + " with type " + declaredType().fqName() + " is annotated"
                                                       + " with @SameGeneric, yet the key generic type cannot be determined."
                                                       + " Either the key must be a simple type, or a type with one type"
                                                       + " argument.");
        }

        method.addGenericArgument(TypeArgument.builder()
                                          .token("TYPE")
                                          .bound(genericTypeBase)
                                          .description("Type to correctly map key and value")
                                          .build());

        // now resolve value
        if (valueType.typeArguments().isEmpty()) {
            if (!genericTypeBase.equals(valueType)) {
                throw new IllegalArgumentException("Property " + name() + " with type " + declaredType().fqName() + " is "
                                                           + "annotated"
                                                           + " with @SameGeneric, yet the type of value is not the"
                                                           + " same as type found on key: " + genericTypeBase.fqName());
            }
            resolvedValueType = SAME_GENERIC_TYPE;
        } else if (valueType.typeArguments().size() == 1) {
            if (!genericTypeBase.equals(valueType.typeArguments().get(0))) {
                throw new IllegalArgumentException("Property " + name() + " with type " + declaredType().fqName() + " is "
                                                           + "annotated"
                                                           + " with @SameGeneric, yet type of value is not the"
                                                           + " same as type found on key: " + genericTypeBase.fqName());
            }
            resolvedValueType = TypeName.builder(valueType)
                    .typeArguments(List.of(SAME_GENERIC_TYPE))
                    .build();
        } else {
            throw new IllegalArgumentException("Property " + name() + " with type " + declaredType().fqName() + " is annotated"
                                                       + " with @SameGeneric, yet the value generic type cannot be determined."
                                                       + " Either the value must be a simple type, or a type with one type"
                                                       + " argument.");
        }

        method.addParameter(param -> param.name("key")
                        .type(resolvedKeyType)
                        .description("key to add or replace"))
                .addParameter(param -> param.name(value)
                        .type(resolvedValueType)
                        .description("new value for the key"));
    }

    private void setterAddValueToCollection(InnerClass.Builder classBuilder,
                                            AnnotationDataOption configured,
                                            String singularName,
                                            TypeName keyType,
                                            TypeName valueType,
                                            TypeName returnType,
                                            Javadoc blueprintJavadoc) {
        String methodName = "add" + capitalize(singularName);
        TypeName implType = collectionImplType(actualType());

        classBuilder.addMethod(builder -> builder.name(methodName)
                .accessModifier(setterAccessModifier(configured))
                .addParameter(param -> param.name("key")
                        .type(keyType)
                        .description("key to add to"))
                .addParameter(param -> param.name(singularName)
                        .type(valueType)
                        .description("additional value for the key"))
                .description(blueprintJavadoc.content())
                .addDescriptionLine("This method adds a new value to the map value, or creates a new value.")
                .addJavadocTag("see", "#" + getterName() + "()")
                .returnType(returnType, "updated builder instance")
                .typeName(Objects.class)
                .addLine(".requireNonNull(key);")
                .typeName(Objects.class)
                .addLine(".requireNonNull(" + singularName + ");")
                .addLine("this." + name() + ".compute(key, (k, v) -> {")
                .add("v = v == null ? new ")
                .typeName(implType)
                .add("<>() : new ")
                .typeName(implType)
                .addLine("<>(v);")
                .addLine("v.add(" + singularName + ");")
                .addLine("return v;")
                .decreasePadding()
                .addLine("});")
                .addLine("return self();"));
    }

    private void setterAddValuesToCollection(InnerClass.Builder classBuilder,
                                             AnnotationDataOption configured,
                                             String methodName,
                                             TypeName keyType,
                                             TypeName returnType,
                                             Javadoc blueprintJavadoc) {
        TypeName implType = collectionImplType(actualType());
        String name = name();

        classBuilder.addMethod(builder -> builder.name(methodName)
                .accessModifier(setterAccessModifier(configured))
                .addParameter(param -> param.name("key")
                        .type(keyType)
                        .description("key to add to"))
                .addParameter(param -> param.name(name)
                        .type(actualType())
                        .description("additional values for the key"))
                .description(blueprintJavadoc.content())
                .addDescriptionLine("This method adds a new value to the map value, or creates a new value.")
                .addJavadocTag("see", "#" + getterName() + "()")
                .returnType(returnType, "updated builder instance")
                .typeName(Objects.class)
                .addLine(".requireNonNull(key);")
                .typeName(Objects.class)
                .addLine(".requireNonNull(" + name + ");")
                .addLine("this." + name + ".compute(key, (k, v) -> {")
                .add("v = v == null ? new ")
                .typeName(implType)
                .add("<>() : new ")
                .typeName(implType)
                .addLine("<>(v);")
                .addLine("v.addAll(" + name + ");")
                .addLine("return v;")
                .decreasePadding()
                .addLine("});")
                .addLine("return self();"));
    }

    private void declaredSetterAdd(InnerClass.Builder classBuilder, AnnotationDataOption configured,
                                   TypeName returnType,
                                   Javadoc blueprintJavadoc) {
        // declared type - add content
        classBuilder.addMethod(builder -> builder.name("add" + capitalize(name()))
                .returnType(returnType, "updated builder instance")
                .description(blueprintJavadoc.content())
                .addDescriptionLine("This method keeps existing values, then puts all new values into the map.")
                .addJavadocTag("see", "#" + getterName() + "()")
                .addParameter(param -> param.name(name())
                        .type(argumentTypeName())
                        .description(blueprintJavadoc.returnDescription()))
                .accessModifier(setterAccessModifier(configured))
                .typeName(Objects.class)
                .addLine(".requireNonNull(" + name() + ");")
                .addLine("this." + name() + ".putAll(" + name() + ");")
                .addLine("return self();"));
    }

    private void secondArgToPut(Method.Builder method, TypeName typeName, String singularName) {
        TypeName genericTypeName = typeName.genericTypeName();
        if (genericTypeName.equals(LIST)) {
            method.typeName(List.class).add(".copyOf(" + singularName + ")");
        } else if (genericTypeName.equals(SET)) {
            method.typeName(Set.class).add(".copyOf(" + singularName + ")");
        } else if (genericTypeName.equals(MAP)) {
            method.typeName(Map.class).add(".copyOf(" + singularName + ")");
        } else {
            method.add(singularName);
        }
    }

    private boolean isCollection(TypeName typeName) {
        if (typeName.typeArguments().size() != 1) {
            return false;
        }
        TypeName genericTypeName = typeName.genericTypeName();
        if (genericTypeName.equals(LIST)) {
            return true;
        }
        return genericTypeName.equals(SET);
    }

}
