package io.helidon.inject.processor;

import java.util.ArrayList;
import java.util.List;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.types.TypeName;

class InjectionProcessingContextImpl implements InjectionProcessingContext {
    private final List<ClassCode> classModels = new ArrayList<>();
    private final ProcessingContext ctx;

    InjectionProcessingContextImpl(ProcessingContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void addServiceDescriptor(TypeName serviceClassType,
                                     TypeName serviceDescriptorType,
                                     ClassModel.Builder descriptorBuilder) {
        classModels.add(new ClassCode(serviceDescriptorType, descriptorBuilder, serviceClassType));
    }

    @Override
    public ProcessingContext ctx() {
        return ctx;
    }

    @Override
    public TypeName serviceDescriptorType(TypeName serviceType) {
        // type is generated in the same package with a name suffix

        return TypeName.builder()
                .packageName(serviceType.packageName())
                .className(descriptorClassName(serviceType))
                .build();
    }

    List<ClassCode> classModels() {
        return classModels;
    }

    private static String descriptorClassName(TypeName typeName) {
        // for MyType.MyService -> MyType_MyService__ServiceDescriptor

        List<String> enclosing = typeName.enclosingNames();
        String namePrefix;
        if (enclosing.isEmpty()) {
            namePrefix = "";
        } else {
            namePrefix = String.join("_", enclosing) + "_";
        }
        return namePrefix
                + typeName.className()
                + "__ServiceDescriptor";
    }

}
