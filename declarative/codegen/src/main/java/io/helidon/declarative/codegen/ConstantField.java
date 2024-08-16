package io.helidon.declarative.codegen;

import io.helidon.codegen.classmodel.Field;
import io.helidon.common.types.AccessModifier;

public class ConstantField {
    public static void privateConstant(Field.Builder builder) {
        builder.accessModifier(AccessModifier.PRIVATE)
                .isStatic(true)
                .isFinal(true);
    }
}
