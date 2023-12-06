package io.helidon.inject.codegen;

import java.util.Set;

import io.helidon.codegen.ClassCode;
import io.helidon.codegen.CodegenScope;
import io.helidon.codegen.CodegenUtil;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Javadoc;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;

class ModuleComponentHandler {
    private static final String MODULE_NAME = "HelidonInjection__ModuleComponent";
    private static final TypeName MODULE_COMPONENT_TYPE = TypeName.create("io.helidon.inject.api.ModuleComponent");
    private static final TypeName SERVICE_BINDER_TYPE = TypeName.create("io.helidon.inject.api.ServiceBinder");
    private static final TypeName GENERATOR = TypeName.create(ModuleComponentHandler.class);

    private ModuleComponentHandler() {
    }

    static ClassCode createClassModel(CodegenScope scope,
                                      Set<TypeName> generatedServiceDescriptors,
                                      String moduleName,
                                      String packageName) {
        TypeName newType = TypeName.builder()
                .packageName(packageName)
                .className(scope.prefix() + MODULE_NAME)
                .build();

        ClassModel.Builder builder = ClassModel.builder()
                .type(newType)
                .addInterface(MODULE_COMPONENT_TYPE)
                .isFinal(true)
                .description("Generated ModuleComponent, loaded by ServiceLoader.");

        // copyright
        builder.copyright(CodegenUtil.copyright(GENERATOR,
                                                GENERATOR,
                                                newType));

        // @Generated
        builder.addAnnotation(CodegenUtil.generatedAnnotation(GENERATOR,
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

        // String name()
        builder.addField(name -> name.name("NAME")
                .type(TypeNames.STRING)
                .isStatic(true)
                .isFinal(true)
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .defaultValue("\"" + moduleName + "\""));
        builder.addMethod(named -> named.name("name")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContentLine("return NAME;"));

        // to String
        builder.addMethod(named -> named.name("toString")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContentLine("return NAME + \":\" + getClass().getName();"));

        // configure
        builder.addMethod(configure -> configure.name("configure")
                .addAnnotation(Annotations.OVERRIDE)
                .addParameter(binder -> binder.name("binder")
                        .type(SERVICE_BINDER_TYPE))
                .update(methodBody -> {
                    for (TypeName generatedServiceDescriptor : generatedServiceDescriptors) {
                        methodBody.addContent("binder.bind(")
                                .addContent(generatedServiceDescriptor.genericTypeName())
                                .addContentLine(".INSTANCE);");
                    }
                }));

        return new ClassCode(newType, builder, GENERATOR, (Object[]) generatedServiceDescriptors.toArray(new TypeName[0]));
    }
}
