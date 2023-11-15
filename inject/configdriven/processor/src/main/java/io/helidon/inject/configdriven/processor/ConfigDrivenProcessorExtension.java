package io.helidon.inject.configdriven.processor;

import java.util.Optional;

import io.helidon.common.processor.TypeInfoFactory;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.processor.classmodel.Method;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.inject.processor.InjectionProcessingContext;
import io.helidon.inject.processor.RoundContext;
import io.helidon.inject.processor.spi.HelidonProcessorExtension;
import io.helidon.inject.tools.ToolsException;

class ConfigDrivenProcessorExtension implements HelidonProcessorExtension {
    private static final String CONFIG_DRIVEN_RUNTIME_ID = "CONFIG_DRIVEN";
    private static final TypeName CONFIG_BEAN_FACTORY = TypeName.create("io.helidon.inject.configdriven.api.ConfigBeanFactory");
    private static final TypeName NAMED_INSTANCE_TYPE = TypeName.create("io.helidon.inject.configdriven.api.NamedInstance");
    private static final TypeName CONFIG_TYPE = TypeName.create("io.helidon.common.config.Config");

    private final InjectionProcessingContext ctx;

    ConfigDrivenProcessorExtension(InjectionProcessingContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean process(RoundContext roundContext) {
        // everything should be done via the default annotation processor for injection (as we map config driven to injection)
        // we just need to add a few things here
        for (TypeInfo typeInfo : roundContext.annotatedTypes(ConfigDrivenAnnotation.TYPE)) {
            Optional<ClassModel.Builder> foundModel = ctx.serviceDescriptor(typeInfo.typeName());
            if (foundModel.isEmpty()) {
                throw new IllegalStateException("Class descriptor should have been generated by Injection extension, but is "
                                                        + "empty");
            }
            ClassModel.Builder classModel = foundModel.get();

            /*
                must use a different runtime ID, so we use a different activator
             */
            classModel.addMethod(runtimeId -> runtimeId.name("runtimeId")
                    .addAnnotation(Annotations.OVERRIDE)
                    .returnType(TypeNames.STRING)
                    .addLine("return \"" + CONFIG_DRIVEN_RUNTIME_ID + "\";"));

            /*
                must implement the ConfigBeanFactory, so we can create instances of config beans
             */
            ConfigDrivenAnnotation cdAnnotation = ConfigDrivenAnnotation.create(typeInfo);
            // the type must be either a valid prototype, or a prototype blueprint (in case this is the same module)
            if ("<error>".equals(cdAnnotation.configBeanType().className())) {
                throw new ToolsException("The config bean type must be set to the Blueprint type if they are within the same "
                                                 + "module! Failed on: " + typeInfo.typeName().resolvedName());
            }
            TypeInfo configBeanTypeInfo = TypeInfoFactory.create(ctx.ctx(), cdAnnotation.configBeanType())
                    .orElseThrow();
            ConfigBean configBean = ConfigBean.create(configBeanTypeInfo);
            TypeName configBeanType = configBean.typeName();

            // implements ConfigBeanFactory<ConfigBeanType>
            classModel.addInterface(TypeName.builder(CONFIG_BEAN_FACTORY)
                                            .addTypeArgument(configBeanType)
                                            .build());
            // Class<ConfigBeanType> configBeanType()
            classModel.addMethod(beanTypeMethod -> beanTypeMethod
                    .name("configBeanType")
                    .addAnnotation(Annotations.OVERRIDE)
                    .returnType(TypeName.builder()
                                        .type(Class.class)
                                        .addTypeArgument(configBeanType)
                                        .build())
                    .addLine("return @" + configBeanType.fqName() + "@.class;"));
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
                    .addLine("return " + cdAnnotation.activateByDefault() + ";"));
        }
        return false;
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

        method.addLine("var beanConfig = config.get(\"" + prefix + "\");");
        method.addLine("if (!beanConfig.exists()) {");

        // the config does not exist (always returns)
        if (atLeastOne) {
            // throw an exception, we need at least one instance
            method.add("throw new @io.helidon.common.config.ConfigException@(\"");
            if (repeatable) {
                method.add("Expecting list of configurations");
            } else {
                method.add("Expecting configuration");
            }
            method.add(" at \\\"");
            method.add(configBean.configPrefix());
            method.addLine("\\\"\");");
        } else if (wantDefault) {
            method.add("return @java.util.List@.of(new @io.helidon.inject.configdriven.api.NamedInstance@<>(");
            method.add(configBean.typeName().resolvedName());
            method.addLine(".create(@io.helidon.common.config.Config@.empty()), "
                                   + "@io.helidon.inject.configdriven.api.NamedInstance@.DEFAULT_NAME));");
        } else {
            method.addLine("return List.of();");
        }
        method.addLine("}"); // end of if config does not exist

        // the bean config does exist
        if (repeatable) {
            method.add("return createRepeatableBeans(beanConfig, ")
                    .add(String.valueOf(wantDefault))
                    .add(", ")
                    .add(configBean.typeName().resolvedName())
                    .addLine("::create);");
        } else {
            method.addLine("if (beanConfig.isList()) {")
                    .add("throw new @io.helidon.common.config.ConfigException@(\"Expecting a single node at \\\"")
                    .add(configBean.configPrefix())
                    .addLine("\\\", but got a list\");")
                    .addLine("}");
            method.add("return @java.util.List@.of(new @")
                    .add(NAMED_INSTANCE_TYPE.resolvedName())
                    .add("@<>(@")
                    .add(configBean.typeName().resolvedName())
                    .add("@.create(beanConfig), @")
                    .add(NAMED_INSTANCE_TYPE.resolvedName())
                    .addLine("@.DEFAULT_NAME));");
        }
    }

    private TypeName listOfNamedInstances(TypeName configBeanType) {
        return TypeName.builder(TypeNames.LIST)
                .addTypeArgument(TypeName.builder(NAMED_INSTANCE_TYPE)
                                         .addTypeArgument(configBeanType)
                                         .build())
                .build();
    }

}
