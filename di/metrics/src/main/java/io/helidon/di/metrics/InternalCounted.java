package io.helidon.di.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.micronaut.aop.Around;
import io.micronaut.context.annotation.Type;
import io.micronaut.core.annotation.Internal;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Around
@Type(InterceptCounted.class)
@Internal
// this class must be public, as otherwise it is ignored and interceptor is not triggered
public @interface InternalCounted {
}
