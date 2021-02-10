package io.helidon.di.webserver;

import javax.inject.Singleton;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;

@Singleton
@Internal
@Order(100)
class BindThrowable implements HttpParameterBinder<Throwable> {

    @Override
    public BindingResult<Throwable> bind(ArgumentConversionContext<Throwable> context, HttpExchange source) {
        return source::throwable;
    }

    @Override
    public boolean supports(Argument<?> argument) {
        return Throwable.class.isAssignableFrom(argument.getType());
    }
}
