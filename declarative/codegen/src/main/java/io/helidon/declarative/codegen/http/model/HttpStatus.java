package io.helidon.declarative.codegen.http.model;

import java.util.Optional;

public record HttpStatus(int code, Optional<String> reason) {
}
