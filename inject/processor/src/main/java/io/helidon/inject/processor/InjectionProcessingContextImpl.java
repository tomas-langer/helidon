package io.helidon.inject.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.types.TypeName;

class InjectionProcessingContextImpl implements InjectionProcessingContext {
    private final List<ClassCode> descriptors = new ArrayList<>();
    private final List<ClassCode> nonDescriptors = new ArrayList<>();
    private final ProcessingContext ctx;

    InjectionProcessingContextImpl(ProcessingContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void addServiceDescriptor(TypeName serviceClassType,
                                     TypeName serviceDescriptorType,
                                     ClassModel.Builder descriptorBuilder) {
        descriptors.add(new ClassCode(serviceDescriptorType, descriptorBuilder, serviceClassType));
    }


    @Override
    public void addClass(TypeName triggerType, TypeName typeName, ClassModel.Builder classModel) {
        nonDescriptors.add(new ClassCode(typeName, classModel, triggerType));
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

    @Override
    public Optional<ClassModel.Builder> serviceDescriptor(TypeName serviceClass) {
        for (ClassCode classModel : descriptors) {
            for (TypeName typeName : classModel.originatingTypes()) {
                if (typeName.equals(serviceClass)) {
                    return Optional.ofNullable(classModel.classModel());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ClassModel.Builder> generatedType(TypeName typeName) {
        for (ClassCode classCode : nonDescriptors) {
            if (classCode.newType().equals(typeName)) {
                return Optional.of(classCode.classModel());
            }
        }
        for (ClassCode classCode : descriptors) {
            if (classCode.newType().equals(typeName)) {
                return Optional.of(classCode.classModel());
            }
        }
        return Optional.empty();
    }

    List<ClassCode> otherClassModels() {
        return nonDescriptors;
    }

    List<ClassCode> descriptorClassModels() {
        return descriptors;
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
