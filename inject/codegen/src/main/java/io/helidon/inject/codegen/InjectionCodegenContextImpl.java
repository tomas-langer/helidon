package io.helidon.inject.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.helidon.codegen.ClassCode;
import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenContextDelegate;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.common.types.TypeName;

class InjectionCodegenContextImpl extends CodegenContextDelegate implements InjectionCodegenContext {
    private final List<ClassCode> descriptors = new ArrayList<>();
    private final List<ClassCode> nonDescriptors = new ArrayList<>();

    InjectionCodegenContextImpl(CodegenContext context) {
        super(context);
    }

    @Override
    public Optional<ClassModel.Builder> descriptor(TypeName serviceType) {
        Objects.requireNonNull(serviceType);

        for (ClassCode classModel : descriptors) {
            if (classModel.mainTrigger().equals(serviceType)) {
                return Optional.of(classModel.classModel());
            }
        }
        return Optional.empty();
    }

    @Override
    public void addDescriptor(TypeName serviceType,
                              TypeName descriptorType,
                              ClassModel.Builder descriptor,
                              Object... originatingElements) {
        Objects.requireNonNull(serviceType);
        Objects.requireNonNull(descriptorType);
        Objects.requireNonNull(descriptor);

        descriptors.add(new ClassCode(descriptorType, descriptor, serviceType, originatingElements));
    }

    @Override
    public void addType(TypeName type, ClassModel.Builder newClass, TypeName mainTrigger, Object... originatingElements) {
        nonDescriptors.add(new ClassCode(type, newClass, mainTrigger, originatingElements));
    }

    @Override
    public Optional<ClassModel.Builder> type(TypeName type) {
        for (ClassCode classCode : nonDescriptors) {
            if (classCode.newType().equals(type)) {
                return Optional.of(classCode.classModel());
            }
        }
        for (ClassCode classCode : descriptors) {
            if (classCode.newType().equals(type)) {
                return Optional.of(classCode.classModel());
            }
        }
        return Optional.empty();
    }

    @Override
    public TypeName descriptorType(TypeName serviceType) {
        // type is generated in the same package with a name suffix

        return TypeName.builder()
                .packageName(serviceType.packageName())
                .className(descriptorClassName(serviceType))
                .build();
    }

    @Override
    public List<ClassCode> types() {
        return nonDescriptors;
    }

    @Override
    public List<ClassCode> descriptors() {
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