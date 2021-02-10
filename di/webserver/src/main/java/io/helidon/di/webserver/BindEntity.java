package io.helidon.di.webserver;

import java.util.Optional;

import javax.inject.Singleton;

import io.helidon.di.annotation.http.Entity;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.bind.annotation.AnnotatedArgumentBinder;
import io.micronaut.core.convert.ArgumentConversionContext;

@Singleton
@Internal
@Order(100)
class BindEntity<T> implements AnnotatedArgumentBinder<Entity, T, HttpExchange> {

    @Override
    public Class<Entity> getAnnotationType() {
        return Entity.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, HttpExchange source) {
        return () -> (Optional<T>) source.entity();
    }
}
