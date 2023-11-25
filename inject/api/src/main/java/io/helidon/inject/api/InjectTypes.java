package io.helidon.inject.api;

import io.helidon.common.types.TypeName;

import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

public final class InjectTypes {

    /**
     * Jakarta legacy {@code javax.inject.Provider} type.
     */
    public static final TypeName JAVAX_PROVIDER = TypeName.create("javax.inject.Provider");
    /**
     * Jakarta {@link jakarta.inject.Provider} type.
     */
    public static final TypeName JAKARTA_PROVIDER = TypeName.create(Provider.class);
    /**
     * Jakarta {@link jakarta.inject.Singleton} type.
     */
    public static final TypeName SINGLETON = TypeName.create(Singleton.class);
    /**
     * Jakarta {@link jakarta.inject.Named} type
     */
    public static final TypeName JAKARTA_NAMED = TypeName.create(Named.class);
    /**
     * Injection {@link io.helidon.inject.api.InjectionPointProvider} type.
     */
    public static final TypeName INJECTION_POINT_PROVIDER = TypeName.create(InjectionPointProvider.class);
    /**
     * Injection {@link io.helidon.inject.api.ServiceProvider} type.
     */
    public static final TypeName INJECTION_SERVICE_PROVIDER = TypeName.create(ServiceProvider.class);

    private InjectTypes() {
    }
}
