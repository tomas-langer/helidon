package io.helidon.mn.webserver;

import java.util.Optional;

import javax.inject.Singleton;

import io.helidon.webserver.ServerRequest;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;

@Singleton
@Internal
public class BindServerRequest implements HttpParameterBinder<ServerRequest> {

    @Override
    public BindingResult<ServerRequest> bind(ArgumentConversionContext<ServerRequest> context, HttpExchange source) {
        return () -> Optional.of(source.request());
    }

    @Override
    public boolean supports(Argument<?> argument) {
        return argument.getType() == ServerRequest.class;
    }
}
