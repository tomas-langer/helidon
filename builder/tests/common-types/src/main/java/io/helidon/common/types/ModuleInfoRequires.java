package io.helidon.common.types;

public record ModuleInfoRequires(String module, boolean isTransitive, boolean isStatic) {
}
