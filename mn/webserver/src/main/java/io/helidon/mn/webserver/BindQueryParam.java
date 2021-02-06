package io.helidon.mn.webserver;

import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

import io.helidon.common.http.Parameters;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.annotation.AnnotatedArgumentBinder;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import jakarta.ws.rs.QueryParam;

@Singleton
@Internal
public class BindQueryParam<T> implements AnnotatedArgumentBinder<QueryParam, T, HttpExchange> {

    private final ConversionService<?> conversionService;

    public BindQueryParam(ConversionService<?> conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Class<QueryParam> getAnnotationType() {
        return QueryParam.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, HttpExchange source) {
        Argument<T> argument = context.getArgument();
        AnnotationMetadata annotationMetadata = argument.getAnnotationMetadata();

        String name = annotationMetadata.stringValue(QueryParam.class)
                .orElse(argument.getName());

        Parameters parameters = source.request().queryParams();

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
