package io.helidon.di.security;

import io.helidon.di.webserver.ServiceOrder;

import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;

public class InterceptSecured implements MethodInterceptor<Object, Object> {
    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        System.out.println("Intercepting security: " + context.getMethodName());
        return context.proceed();
    }

    @Override
    public int getOrder() {
        return ServiceOrder.SECURITY;
    }
}
