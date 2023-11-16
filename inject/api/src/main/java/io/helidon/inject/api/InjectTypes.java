package io.helidon.inject.api;

import io.helidon.common.types.TypeName;

public final class InjectTypes {

    /**
     * Jakarta legacy {@code javax.inject.Provider} type.
     */
    public static final TypeName JAVAX_PROVIDER = TypeName.create("javax.inject.Provider");
    /**
     * Jakarta {@link jakarta.inject.Provider} type.
     */
    public static final TypeName JAKARTA_PROVIDER = TypeName.create("jakarta.inject.Provider");
    /**
     * Injection {@link io.helidon.inject.api.InjectionPointProvider} type.
     */
    public static final TypeName INJECTION_POINT_PROVIDER = TypeName.create("io.helidon.inject.api.InjectionPointProvider");
    /**
     * Injection {@link io.helidon.inject.api.ServiceProvider} type.
     */
    public static final TypeName INJECTION_SERVICE_PROVIDER = TypeName.create("io.helidon.inject.api.ServiceProvider");

    private InjectTypes() {
    }
}
