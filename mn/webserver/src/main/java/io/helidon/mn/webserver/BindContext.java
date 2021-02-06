package io.helidon.mn.webserver;

import java.util.Optional;

import javax.inject.Singleton;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.annotation.AnnotatedArgumentBinder;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import jakarta.ws.rs.core.Context;

@Singleton
@Internal
public class BindContext<T> implements AnnotatedArgumentBinder<Context, T, HttpExchange> {

    private final ConversionService<?> conversionService;

    public BindContext(ConversionService<?> conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Class<Context> getAnnotationType() {
        return Context.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, HttpExchange source) {
        Argument<T> argument = context.getArgument();
        Class<T> type = argument.getType();
        Optional<T> value;

        return BindingResult.UNSATISFIED;
    }
}
