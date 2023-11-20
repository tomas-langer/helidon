package io.helidon.inject.processor;

import java.util.Optional;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.types.TypeName;

public interface InjectionProcessingContext {
    ProcessingContext ctx();

    TypeName serviceDescriptorType(TypeName serviceType);

    void addServiceDescriptor(TypeName serviceClassType, TypeName serviceDescriptorType, ClassModel.Builder descriptorBuilder);

    Optional<ClassModel.Builder> serviceDescriptor(TypeName serviceClass);

    void addClass(TypeName triggerType, TypeName typeName, ClassModel.Builder generate);

    Optional<ClassModel.Builder> generatedType(TypeName typeName);

    HandlingScope scope();
}
