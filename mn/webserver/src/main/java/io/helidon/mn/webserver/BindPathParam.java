package io.helidon.mn.webserver;

import java.util.Optional;

import javax.inject.Singleton;

import io.helidon.annotation.http.PathParam;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.annotation.AnnotatedArgumentBinder;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;

@Singleton
@Internal
public class BindPathParam<T> implements AnnotatedArgumentBinder<PathParam, T, HttpExchange> {

    private final ConversionService<?> conversionService;

    public BindPathParam(ConversionService<?> conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Class<PathParam> getAnnotationType() {
        return PathParam.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, HttpExchange source) {
        Argument<T> argument = context.getArgument();
        AnnotationMetadata annotationMetadata = argument.getAnnotationMetadata();

        String name = annotationMetadata.stringValue(PathParam.class)
                .orElse(argument.getName());

        String param = source.request().path().param(name);

        if (param == null) {
            param = annotationMetadata.stringValue(Bindable.class, "defaultValue")
                    .orElse(null);
        }
        if (param == null) {
            return BindingResult.UNSATISFIED;
        }

        Optional<T> value = conversionService.convert(param, argument.getType());
        return () -> value;
    }
}
