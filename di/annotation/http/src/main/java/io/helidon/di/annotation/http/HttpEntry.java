package io.helidon.di.annotation.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.EntryPoint;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Meta annotation for all HTTP methods.
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@Executable(processOnStartup = true)
@EntryPoint
public @interface HttpEntry {
}
