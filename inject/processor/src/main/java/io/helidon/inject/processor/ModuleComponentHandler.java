package io.helidon.inject.processor;

import java.util.Set;

import io.helidon.common.processor.CopyrightHandler;
import io.helidon.common.processor.GeneratedAnnotationHandler;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.processor.classmodel.Javadoc;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;

class ModuleComponentHandler {
    private static final String MODULE_NAME = "HelidonInjection__ModuleComponent";
    private static final TypeName MODULE_COMPONENT_TYPE = TypeName.create("io.helidon.inject.api.ModuleComponent");
    private static final TypeName SERVICE_BINDER_TYPE = TypeName.create("io.helidon.inject.api.ServiceBinder");
    private static final TypeName GENERATOR = TypeName.create(ModuleComponentHandler.class);
    private static final TypeName OPTIONAL_STRING = TypeName.builder(TypeNames.OPTIONAL)
            .addTypeArgument(TypeNames.STRING)
            .build();

    private ModuleComponentHandler() {
    }

    static ClassCode createClassModel(Set<TypeName> generatedServiceDescriptors,
                                      String moduleName,
                                      String packageName) {
        TypeName newType = TypeName.builder()
                .packageName(packageName)
                .className(MODULE_NAME)
                .build();

        ClassModel.Builder builder = ClassModel.builder()
                .type(newType)
                .addInterface(MODULE_COMPONENT_TYPE)
                .isFinal(true)
                .description("Generated ModuleComponent, loaded by ServiceLoader.");

        // copyright
        builder.copyright(CopyrightHandler.copyright(GENERATOR,
                                              GENERATOR,
                                              newType));

        // @Generated
        builder.addAnnotation(GeneratedAnnotationHandler.create(GENERATOR,
                                                              GENERATOR,
                                                              newType,
                                                              "1",
                                                              ""));

        // constructor
        builder.addConstructor(constructor -> constructor.addAnnotation(it -> it.type(Deprecated.class))
                .javadoc(Javadoc.builder()
                                 .addLine("Constructor for ServiceLoader.")
                                 .addTag("deprecated", "for use by Java ServiceLoader, do not use directly")
                                 .build()));

        // Optional<String> named()
        builder.addField(name -> name.name("NAME")
                .type(TypeNames.STRING)
                .isStatic(true)
                .isFinal(true)
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .defaultValue("\"" + moduleName + "\""));
        builder.addMethod(named -> named.name("named")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(OPTIONAL_STRING)
                .addLine("return @java.util.Optional@.of(NAME);"));

        // to String
        builder.addMethod(named -> named.name("toString")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addLine("return NAME + \":\" + getClass().getName();"));


        // configure
        builder.addMethod(configure -> configure.name("configure")
                .addAnnotation(Annotations.OVERRIDE)
                .addParameter(binder -> binder.name("binder")
                        .type(SERVICE_BINDER_TYPE))
                .update(methodBody -> {
                    for (TypeName generatedServiceDescriptor : generatedServiceDescriptors) {
                        methodBody.addLine("binder.bind(@" +generatedServiceDescriptor.declaredName() + "@.INSTANCE);");
                    }
                }));

        return new ClassCode(newType, builder, generatedServiceDescriptors.toArray(new TypeName[0]));
    }
}
