package io.helidon.builder.codegen;

import io.helidon.codegen.classmodel.Javadoc;
import io.helidon.common.types.TypeName;

record CustomConstant(TypeName declaringType, TypeName fieldType, String name, Javadoc javadoc) {
}
