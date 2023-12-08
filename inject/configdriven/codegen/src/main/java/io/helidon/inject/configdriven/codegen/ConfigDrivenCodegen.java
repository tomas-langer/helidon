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

package io.helidon.inject.configdriven.codegen;

import java.util.List;
import java.util.Optional;

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Method;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.inject.codegen.InjectCodegenTypes;
import io.helidon.inject.codegen.InjectionCodegenContext;
import io.helidon.inject.codegen.RoundContext;
import io.helidon.inject.codegen.spi.InjectCodegenExtension;

class ConfigDrivenCodegen implements InjectCodegenExtension {
    private static final String CONFIG_DRIVEN_RUNTIME_ID = "CONFIG_DRIVEN";
    private static final TypeName CONFIG_BEAN_FACTORY = TypeName.create("io.helidon.inject.configdriven.api.ConfigBeanFactory");
    private static final TypeName NAMED_INSTANCE_TYPE = TypeName.create("io.helidon.inject.configdriven.api.NamedInstance");
    private static final TypeName CONFIG_TYPE = TypeName.create("io.helidon.common.config.Config");
    private static final TypeName CONFIG_EXCEPTION_TYPE = TypeName.create("io.helidon.common.config.ConfigException");

    private final InjectionCodegenContext ctx;

    ConfigDrivenCodegen(InjectionCodegenContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void process(RoundContext roundContext) {
        // everything should be done via the default annotation processor for injection (as we map config driven to injection)
        // we just need to add a few things here
        for (TypeInfo typeInfo : roundContext.annotatedTypes(ConfigDrivenAnnotation.TYPE)) {
            Optional<ClassModel.Builder> foundModel = ctx.descriptor(typeInfo.typeName());
            if (foundModel.isEmpty()) {
                throw new CodegenException("Class descriptor should have been generated by Injection extension, but is empty",
                                           typeInfo.originatingElement().orElse(typeInfo.typeName()));
            }
            ClassModel.Builder classModel = foundModel.get();

            /*
                must use a different runtime ID, so we use a different activator
             */
            classModel.addMethod(runtimeId -> runtimeId.name("runtimeId")
                    .addAnnotation(Annotations.OVERRIDE)
                    .returnType(TypeNames.STRING)
                    .addContentLine("return \"" + CONFIG_DRIVEN_RUNTIME_ID + "\";"));

            /*
                must implement the ConfigBeanFactory, so we can create instances of config beans
             */
            ConfigDrivenAnnotation cdAnnotation = ConfigDrivenAnnotation.create(typeInfo);
            // the type must be either a valid prototype, or a prototype blueprint (in case this is the same module)
            if ("<error>".equals(cdAnnotation.configBeanType().className())) {
                throw new CodegenException("The config bean type must be set to the Blueprint type if they are within the same "
                                                   + "module! Failed on: " + typeInfo.typeName().resolvedName(),
                                           typeInfo.originatingElement().orElse(typeInfo.typeName()));
            }
            TypeInfo configBeanTypeInfo = ctx.typeInfo(cdAnnotation.configBeanType())
                    .orElseThrow();
            ConfigBean configBean = ConfigBean.create(configBeanTypeInfo);
            TypeName configBeanType = configBean.typeName();

            // implements ConfigBeanFactory<ConfigBeanType>
            classModel.addInterface(TypeName.builder(CONFIG_BEAN_FACTORY)
                                            .addTypeArgument(configBeanType)
                                            .build());
            classModel.addField(cbType -> cbType.isFinal(true)
                    .isStatic(true)
                    .accessModifier(AccessModifier.PRIVATE)
                    .name("CB_TYPE")
                    .type(TypeNames.HELIDON_TYPE_NAME)
                    .addContentCreate(configBeanType.genericTypeName()));

            // Class<ConfigBeanType> configBeanType()
            classModel.addMethod(beanTypeMethod -> beanTypeMethod
                    .name("configBeanType")
                    .addAnnotation(Annotations.OVERRIDE)
                    .returnType(TypeNames.HELIDON_TYPE_NAME)
                    .addContentLine("return CB_TYPE;"));
            // List<NamedInstance<T>> createConfigBeans(Config config)
            classModel.addMethod(createBeansMethod -> createBeansMethod
                    .name("createConfigBeans")
                    .addAnnotation(Annotations.OVERRIDE)
                    .returnType(listOfNamedInstances(configBeanType))
                    .addParameter(configParam -> configParam
                            .name("config")
                            .type(CONFIG_TYPE))
                    .update(it -> createConfigBeansMethodBody(it, configBean)));

            classModel.addMethod(drivesActivationMethod -> drivesActivationMethod
                    .name("drivesActivation")
                    .addAnnotation(Annotations.OVERRIDE)
                    .returnType(TypeNames.PRIMITIVE_BOOLEAN)
                    .addContentLine("return " + cdAnnotation.activateByDefault() + ";"));
        }
    }

    private TypeName listOfNamedInstances(TypeName configBeanType) {
        return TypeName.builder(TypeNames.LIST)
                .addTypeArgument(TypeName.builder(NAMED_INSTANCE_TYPE)
                                         .addTypeArgument(configBeanType)
                                         .build())
                .build();
    }

    private void createConfigBeansMethodBody(Method.Builder method,
                                             ConfigBean configBean) {

        /*
        Now we have all the information we need
        - type that is annotated as @ConfigDriven
        - which type drives it
        - config prefix for the type driving it
        - if repeatable etc.
         */
        String prefix = configBean.configPrefix();
        boolean atLeastOne = configBean.annotation().atLeastOne();
        boolean repeatable = configBean.annotation().repeatable();
        boolean wantDefault = configBean.annotation().wantDefault();

        method.addContentLine("var beanConfig = config.get(\"" + prefix + "\");");
        method.addContentLine("if (!beanConfig.exists()) {");

        // the config does not exist (always returns)
        if (atLeastOne) {
            // throw an exception, we need at least one instance
            method.addContent("throw new ")
                    .addContent(CONFIG_EXCEPTION_TYPE)
                    .addContent("(\"");
            if (repeatable) {
                method.addContent("Expecting list of configurations");
            } else {
                method.addContent("Expecting configuration");
            }
            method.addContent(" at \\\"");
            method.addContent(configBean.configPrefix());
            method.addContentLine("\\\"\");");
        } else if (wantDefault) {
            method.addContent("return ")
                    .addContent(List.class)
                    .addContent(".of(new ")
                    .addContent(NAMED_INSTANCE_TYPE)
                    .addContent("<>(")
                    .addContent(configBean.typeName())
                    .addContentLine(".create(")
                    .addContent(CONFIG_TYPE)
                    .addContent(".empty()), ")
                    .addContent(InjectCodegenTypes.INJECT_NAMED)
                    .addContentLine(".DEFAULT_NAME));");
        } else {
            method.addContent("return ")
                    .addContent(List.class)
                    .addContent(".of();");
        }
        method.addContentLine("}"); // end of if config does not exist

        // the bean config does exist
        if (repeatable) {
            method.addContent("return createRepeatableBeans(beanConfig, ")
                    .addContent(String.valueOf(wantDefault))
                    .addContent(", ")
                    .addContent(configBean.typeName().resolvedName())
                    .addContentLine("::create);");
        } else {
            method.addContentLine("if (beanConfig.isList()) {")
                    .addContent("throw new ")
                    .addContent(CONFIG_EXCEPTION_TYPE)
                    .addContent("(\"Expecting a single node at \\\"")
                    .addContent(configBean.configPrefix())
                    .addContentLine("\\\", but got a list\");")
                    .addContentLine("}");
            method.addContent("return ")
                    .addContent(List.class)
                    .addContent(".of(new ")
                    .addContent(NAMED_INSTANCE_TYPE)
                    .addContent("<>(")
                    .addContent(configBean.typeName())
                    .addContent(".create(beanConfig), ")
                    .addContent(InjectCodegenTypes.INJECT_NAMED)
                    .addContentLine(".DEFAULT_NAME));");
        }
    }
}
