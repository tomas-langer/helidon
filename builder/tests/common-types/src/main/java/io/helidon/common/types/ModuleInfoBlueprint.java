package io.helidon.common.types;

import java.util.List;
import java.util.Map;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Module info.
 */
@Prototype.Blueprint
interface ModuleInfoBlueprint {
    /**
     * Name of the module.
     *
     * @return module name
     */
    String name();

    /**
     * Whether this module is declared as open module.
     *
     * @return whether this module is open
     */
    @Option.DefaultBoolean(false)
    boolean isOpen();

    /**
     * Declared dependencies of the module.
     *
     * @return list of requires
     */
    @Option.Singular
    List<ModuleInfoRequires> requires();

    /**
     * Exports of the module.
     *
     * @return list of exported packages
     */
    @Option.Singular
    List<String> exports();

    /**
     * Used service loader providers.
     *
     * @return list of used provider interfaces
     */
    @Option.Singular
    List<TypeName> uses();

    /**
     * Map of provider interfaces to provider implementations provided by this module.
     *
     * @return map of interface to implementations
     */
    @Option.Singular
    Map<TypeName, List<TypeName>> provides();

    /**
     * Map of opened packages to modules (if any).
     *
     * @return map of package to modules
     */
    @Option.Singular
    Map<String, List<String>> opens();

}
