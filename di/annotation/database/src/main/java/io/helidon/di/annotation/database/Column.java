package io.helidon.di.annotation.database;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface Column {
    /**
     * Name of the column (case insensitive by default) to read from the database row.
     *
     * @return name of the column, defaults to parameter name
     */
    String value() default "";
}
