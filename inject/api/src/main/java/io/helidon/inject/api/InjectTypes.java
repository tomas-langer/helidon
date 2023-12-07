package io.helidon.inject.api;

import io.helidon.common.types.TypeName;
import io.helidon.inject.service.Inject;

public final class InjectTypes {
    /**
     * Helidon {@link io.helidon.inject.service.Inject.Singleton}.
     */
    public static final TypeName SINGLETON = TypeName.create(Inject.Singleton.class);
    /**
     * Helidon {@link io.helidon.inject.service.Inject.Named}.
     */
    public static final TypeName NAMED = TypeName.create(Inject.Named.class);
    /**
     * Helidon {link io.helidon.inject.api.InjectionPointProvider}.
     */
    public static final TypeName INJECTION_POINT_PROVIDER = TypeName.create(InjectionPointProvider.class);
    /**
     * Helidon {@link io.helidon.inject.api.ServiceProvider}.
     */
    public static final TypeName SERVICE_PROVIDER = TypeName.create(ServiceProvider.class);

    private InjectTypes() {
    }
}
