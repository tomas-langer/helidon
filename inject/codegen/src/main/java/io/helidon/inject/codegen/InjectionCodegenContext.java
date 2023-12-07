package io.helidon.inject.codegen;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.helidon.codegen.ClassCode;
import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.ContentBuilder;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;

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

    /**
     * This provides support for replacements of types.
     *
     * @param typeName type name as required by the injection point
     * @param valueSource code with the source of the parameter as Helidon provides it (such as Supplier of type)
     * @return assignment to use for this instance, what type to use in Helidon inject, and code generator to transform to
     *          desired type
     */
    Assignment assignment(TypeName typeName, String valueSource);

    /**
     * Checks whether this type is a {@code ServiceProvider}, {@code InjectionPointProvider}, or a
     * {@link java.util.function.Supplier}.
     *
     * @param typeName type name to test
     * @return whether the type name is a provider
     */
    default boolean isProvider(TypeName typeName) {
        TypeName generic = typeName.genericTypeName();

        return generic.equals(TypeNames.SUPPLIER)
                || generic.equals(InjectCodegenTypes.SERVICE_PROVIDER)
                || generic.equals(InjectCodegenTypes.INJECTION_POINT_PROVIDER);
    }

    /**
     * Assignment for code generation. The original intended purpose is to support {@code Provider} from javax and jakarta
     * without a dependency (or need to understand it) in the generator code.
     *
     * @param usedType type to use as the injected type using only Helidon supported types
     *                 (i.e. {@link java.util.function.Supplier} instead of jakarta {@code Provider}
     * @param codeGenerator code generator that creates appropriate type required by the target
     */
    record Assignment(TypeName usedType, Consumer<ContentBuilder<?>> codeGenerator) {
    }
}
