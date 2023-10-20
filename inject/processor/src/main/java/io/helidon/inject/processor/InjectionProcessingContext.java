package io.helidon.inject.processor;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.types.TypeName;

public interface InjectionProcessingContext {
    ProcessingContext ctx();

    TypeName serviceDescriptorType(TypeName serviceType);

    void addServiceDescriptor(TypeName serviceClassType, TypeName serviceDescriptorType, ClassModel.Builder descriptorBuilder);
}
