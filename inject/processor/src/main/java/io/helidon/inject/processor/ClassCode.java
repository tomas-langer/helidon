package io.helidon.inject.processor;

import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.types.TypeName;

/**
 * A code generated type.
 *
 * @param newType the type that is to be created
 * @param classModel class code
 * @param originatingTypes type causing this code generation
 */
record ClassCode(TypeName newType, ClassModel.Builder classModel, TypeName... originatingTypes) {
}
