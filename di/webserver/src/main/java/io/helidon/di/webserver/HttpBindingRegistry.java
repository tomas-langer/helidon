package io.helidon.di.webserver;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.bind.annotation.AnnotatedArgumentBinder;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.core.type.Argument;

@Singleton
@Internal
class HttpBindingRegistry implements ArgumentBinderRegistry<HttpExchange> {
    private final Map<String, ArgumentBinder<?, HttpExchange>> annotatedBinders = new HashMap<>();
    private final List<HttpParameterBinder<?>> typedBinders = new LinkedList<>();

    protected HttpBindingRegistry(List<? extends AnnotatedArgumentBinder<?, ?, HttpExchange>> annotatedBinders,
                                  List<? extends HttpParameterBinder<?>> typedBinders) {

        for (AnnotatedArgumentBinder<?, ?, HttpExchange> binder : annotatedBinders) {
            this.annotatedBinders.put(binder.getAnnotationType().getName(), binder);
        }
        this.typedBinders.addAll(typedBinders);
        OrderUtil.sort(this.typedBinders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<ArgumentBinder<T, HttpExchange>> findArgumentBinder(Argument<T> argument, HttpExchange source) {
        Optional<? extends ArgumentBinder<?, HttpExchange>> binder = argument.getAnnotationMetadata()
                .getDeclaredAnnotationNameByStereotype(Bindable.class.getName())
                .flatMap(annot -> Optional.ofNullable(annotatedBinders.get(annot)));

        if (binder.isPresent()) {
            return (Optional<ArgumentBinder<T, HttpExchange>>) binder;
        }

        for (HttpParameterBinder<?> typedBinder : typedBinders) {
            if (typedBinder.supports(argument)) {
                return Optional.of((ArgumentBinder<T, HttpExchange>) typedBinder);
            }
        }
        return Optional.empty();
    }
}
