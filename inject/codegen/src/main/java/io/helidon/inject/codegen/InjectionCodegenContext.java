package io.helidon.inject.codegen;

import java.util.List;
import java.util.Optional;

import io.helidon.codegen.ClassCode;
import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.common.types.TypeName;

public interface InjectionCodegenContext extends CodegenContext {
    static InjectionCodegenContext create(CodegenContext context) {
        return new InjectionCodegenContextImpl(context);
    }
    /**
     * Service descriptor of a type that is already created. This allows extensions with lower weight to update
     * the code generated descriptor after it was generated.
     *
     * @param serviceType type of the service (the implementation class we generate descriptor for)
     * @return the builder of class model, if the service has a descriptor
     */
    Optional<ClassModel.Builder> descriptor(TypeName serviceType);

    /**
     * Add a new service descriptor.
     *
     * @param serviceType         type of the service (the implementation class we generate descriptor for)
     * @param descriptorType      type of the service descriptor
     * @param descriptor          descriptor class model
     * @param originatingElements possible originating elements (such as Element in APT, or ClassInfo in classpath scanning)
     * @throws java.lang.IllegalStateException if an attempt is done to register a new descriptor for the same type
     */
    void addDescriptor(TypeName serviceType,
                       TypeName descriptorType,
                       ClassModel.Builder descriptor,
                       Object... originatingElements);

    /**
     * Add a new class to be code generated.
     *
     * @param type                type of the new class
     * @param newClass            builder of the new class
     * @param mainTrigger         a type that caused this, may be the processor itself, if not bound to any type
     * @param originatingElements possible originating elements  (such as Element in APT, or ClassInfo in classpath scanning)
     */
    void addType(TypeName type, ClassModel.Builder newClass, TypeName mainTrigger, Object... originatingElements);

    /**
     * Class for a type.
     *
     * @param type type of the generated type
     * @return class model of the new type if any
     */
    Optional<ClassModel.Builder> type(TypeName type);

    /**
     * Create a descriptor type for a service.
     *
     * @param serviceType type of the service
     * @return type of the service descriptor to be generated
     */
    TypeName descriptorType(TypeName serviceType);

    /**
     * All newly generated types.
     *
     * @return list of types and their source class model
     */
    List<ClassCode> types();

    /**
     * All newly generated descriptors.
     *
     * @return list of descriptors and their source class model
     */
    List<ClassCode> descriptors();
}
