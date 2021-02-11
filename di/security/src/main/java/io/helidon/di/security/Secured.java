package io.helidon.di.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.helidon.security.AuditEvent;

import io.micronaut.aop.Around;
import io.micronaut.context.annotation.Type;
import io.micronaut.core.annotation.Internal;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Around
@Type(InterceptSecured.class)
@Internal
public @interface Secured {
    boolean authenticationRequired() default false;
    boolean authenticationOptional() default false;
    String authenticationProvider() default "";
    boolean authorizationRequired() default false;
    boolean authorizeAll() default false;
    boolean authorizationExplicit() default false;
    String authorizationProvider() default "";
    boolean audited() default false;
    String auditEventType() default "";
    String auditMessageFormat() default "";

    AuditEvent.AuditSeverity auditOkSeverity() default AuditEvent.AuditSeverity.SUCCESS;
    AuditEvent.AuditSeverity auditErrorSeverity() default AuditEvent.AuditSeverity.ERROR;

}
