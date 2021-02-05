package io.helidon.examples.mn.basics;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import io.helidon.common.Reflected;

import io.micronaut.core.annotation.Introspected;

@Reflected
@Introspected
public class Greeting {
    @NotBlank
    @Pattern(regexp = "\\w+")
    private String message;

    public Greeting() {
    }

    public Greeting(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
