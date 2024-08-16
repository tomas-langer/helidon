package io.helidon.declarative.codegen.http.model;

import io.helidon.common.types.TypeName;

public record ComputedHeader(String name, TypeName producer, boolean required) {
}
