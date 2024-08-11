package io.helidon.codegen;

import java.time.Duration;

import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

/**
 * Validation utilities.
 */
public final class Validator {
    /**
     * Validate a duration annotation on a method, field, or constructor.
     *
     * @param enclosingType  type that owns the element
     * @param element        annotated element
     * @param annotationType type of annotation
     * @param property       property of annotation
     * @param value          actual value read from the annotation property
     * @return the value
     * @throws io.helidon.codegen.CodegenException with correct source element describing the problem
     */
    public static String validateDuration(TypeName enclosingType,
                                          TypedElementInfo element,
                                          TypeName annotationType,
                                          String property,
                                          String value) {
        try {
            Duration.parse(value);
            return value;
        } catch (Exception e) {
            throw new CodegenException("Duration expression of annotation " + annotationType.fqName() + "."
                                               + property + "(): "
                                               + "\"" + value + "\" cannot be parsed. Duration expects an"
                                               + " expression such as 'PT1S' (1 second), 'PT0.1S' (tenth of a second)."
                                               + " Please check javadoc of " + Duration.class.getName() + " class.",
                                       e,
                                       element.originatingElement().orElseGet(() -> enclosingType.fqName() + "."
                                               + element.elementName()));
        }
    }

    /**
     * Validate a duration annotation on a type.
     *
     * @param type           annotated type
     * @param annotationType type of annotation
     * @param property       property of annotation
     * @param value          actual value read from the annotation property
     * @return the value
     * @throws io.helidon.codegen.CodegenException with correct source element describing the problem
     */
    public static String validateDuration(TypeInfo type,
                                          TypeName annotationType,
                                          String property,
                                          String value) {
        try {
            Duration.parse(value);
            return value;
        } catch (Exception e) {
            throw new CodegenException("Duration expression of annotation " + annotationType.fqName() + "."
                                               + property + "(): "
                                               + "\"" + value + "\" cannot be parsed. Duration expects an"
                                               + " expression such as 'PT1S' (1 second), 'PT0.1S' (tenth of a second)."
                                               + " Please check javadoc of " + Duration.class.getName() + " class.",
                                       e,
                                       type.originatingElement().orElseGet(type::typeName));
        }
    }
}
