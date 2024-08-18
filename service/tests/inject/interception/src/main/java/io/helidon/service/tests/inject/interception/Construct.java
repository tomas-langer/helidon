package io.helidon.service.tests.inject.interception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.helidon.service.inject.api.Interception;

@Interception.Trigger
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.CONSTRUCTOR)
@interface Construct {
}
