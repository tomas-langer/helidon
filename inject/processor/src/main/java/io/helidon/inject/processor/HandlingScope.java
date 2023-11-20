package io.helidon.inject.processor;

import static io.helidon.common.processor.GeneratorTools.capitalize;

public record HandlingScope(String name) {
    boolean isProduction() {
        return name.isBlank();
    }

    String prefix() {
        if(isProduction()) {
            return "";
        }
        return capitalize(name);
    }
}
