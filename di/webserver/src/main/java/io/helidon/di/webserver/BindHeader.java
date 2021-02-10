package io.helidon.di.webserver;

import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

import io.helidon.di.annotation.http.HeaderParam;
import io.helidon.common.http.Parameters;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.bind.annotation.AnnotatedArgumentBinder;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;

@Singleton
@Internal
@Order(100)
class BindHeader<T> implements AnnotatedArgumentBinder<HeaderParam, T, HttpExchange> {

    private final ConversionService<?> conversionService;

    public BindHeader(ConversionService<?> conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Class<HeaderParam> getAnnotationType() {
        return HeaderParam.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, HttpExchange source) {
        Argument<T> argument = context.getArgument();
        AnnotationMetadata annotationMetadata = argument.getAnnotationMetadata();

        String name = annotationMetadata.stringValue(HeaderParam.class)
                .orElse(argument.getName());

        Parameters parameters = source.request().headers();

        List<String> values = parameters.all(name);
        if (values.isEmpty()) {
            values = annotationMetadata.stringValue(Bindable.class, "defaultValue")
                    .map(List::of)
                    .orElseGet(List::of);
        }

        if (values.isEmpty()) {
            return BindingResult.UNSATISFIED;
        }
        Class<T> type = argument.getType();
        Optional<T> value;

        if (Iterable.class.isAssignableFrom(type)) {
            value = conversionService.convert(values, type);
        } else {
            value = conversionService.convert(values.iterator().next(), type);
        }
        return () -> value;
    }
}
