package io.helidon.di.webserver;

import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.type.Argument;

public interface HttpParameterBinder<T> extends ArgumentBinder<T, HttpExchange> {
    boolean supports(Argument<?> argument);
}
