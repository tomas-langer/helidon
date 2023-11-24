package io.helidon.inject.codegen;

import io.helidon.common.types.TypeName;

public final class InjectUtil {
    private InjectUtil() {
    }

    /**
     * Returns true if the provided type name is a {@code Provider<>} type.
     *
     * @param typeName the type name to check
     * @return true if the provided type is a provider type.
     */
    public static boolean isProvider(TypeName typeName) {
        return  typeName.equals(InjectCodegenTypes.INJECT_PROVIDER)
                || typeName.equals(InjectCodegenTypes.JAVAX_INJECT_PROVIDER)
                || typeName.equals(InjectCodegenTypes.HELIDON_INJECTION_POINT_PROVIDER)
                || typeName.equals(InjectCodegenTypes.HELIDON_SERVICE_PROVIDER);
    }

}
