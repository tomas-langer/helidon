package io.helidon.di.webserver;

import javax.inject.Singleton;

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;

@Singleton
@Internal
@Order(200)
class BindBeans implements HttpParameterBinder<Object> {

    private final BeanContext beanContext;

    protected BindBeans(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    @Override
    public BindingResult<Object> bind(ArgumentConversionContext<Object> context, HttpExchange source) {
        return () -> beanContext.findBean(context.getArgument().getType());
    }

    @Override
    public boolean supports(Argument<?> argument) {
        return beanContext.containsBean(argument.getType());
    }
}
