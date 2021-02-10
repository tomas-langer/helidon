package io.helidon.di.webserver;

import java.util.Optional;

import javax.inject.Singleton;

import io.helidon.webserver.ServerResponse;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;

@Singleton
@Internal
@Order(80)
class BindServerResponse implements HttpParameterBinder<ServerResponse> {

    @Override
    public BindingResult<ServerResponse> bind(ArgumentConversionContext<ServerResponse> context, HttpExchange source) {
        return () -> Optional.of(source.response());
    }

    @Override
    public boolean supports(Argument<?> argument) {
        return argument.getType() == ServerResponse.class;
    }
}
