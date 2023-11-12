package io.helidon.common.codegen;

import java.util.Map;
import java.util.Set;

import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.Modifier;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

public final class TypesCodeGen {
    private static final String ANNOTATION_CLASS = "@" + Annotation.class.getName() + "@";
    private static final String TYPE_NAME_CLASS = "@" + TypeName.class.getName() + "@";
    private static final String ELEMENT_CLASS = "@" + TypedElementInfo.class.getName() + "@";
    private static final String ELEMENT_KIND_CLASS = "@" + ElementKind.class.getName() + "@";
    private static final String MODIFIER_CLASS = "@" + Modifier.class.getName() + "@";
    private static final String ACCESS_MODIFIER_CLASS = "@" + AccessModifier.class.getName() + "@";

    private TypesCodeGen() {
    }

    /**
     * Create a code representation that can be used by Helidon Class Model.
     *
     * @param annotation annotation
     * @return code that can be passed to Helidon class model
     */
    public static String toCreate(Annotation annotation) {
        Map<String, Object> values = annotation.values();
        if (values.isEmpty()) {
            return ANNOTATION_CLASS + ".create(" + toCreate(annotation.typeName(), false) + ")";
        }
        StringBuilder result = new StringBuilder(ANNOTATION_CLASS)
                .append(".builder()")
                .append(".typeName(")
                .append(toCreate(annotation.typeName(), false))
                .append(")");

        annotation.values()
                .keySet()
                .forEach(propertyName -> {
                    result.append(".putValue(\"")
                            .append(propertyName)
                            .append("\", \"")
                            .append(annotation.stringValue().get())
                            .append("\")");
                });

        return result.append(".build()")
                .toString();
    }

    public static String toCreate(TypeName typeName, boolean withTypeArguments) {
        if (withTypeArguments) {
            return TYPE_NAME_CLASS + ".create(\"" + typeName.resolvedName() + "\")";
        }
        return TYPE_NAME_CLASS + ".create(\"" + typeName.fqName() + "\")";
    }

    public static String toCreate(TypedElementInfo element) {
        StringBuilder result = new StringBuilder(ELEMENT_CLASS)
                .append(".builder()")
                .append(".elementTypeKind(")
                .append(ELEMENT_KIND_CLASS)
                .append(".")
                .append(element.elementTypeKind())
                .append(")")
                .append(".typeName(")
                .append(toCreate(element.typeName(), true))
                .append(")");

        if (element.elementTypeKind() != ElementKind.CONSTRUCTOR) {
            result.append(".elementName(\"")
                    .append(element.elementName())
                    .append("\")");
        }

        for (Annotation annotation : element.annotations()) {
            result.append(".addAnnotation(")
                    .append(toCreate(annotation))
                    .append(")");
        }

        AccessModifier accessModifier = element.accessModifier();
        if (accessModifier != AccessModifier.PACKAGE_PRIVATE) {
            result.append(".accessModifier(")
                    .append(ACCESS_MODIFIER_CLASS)
                    .append(".")
                    .append(accessModifier)
                    .append(")");
        }

        Set<Modifier> modifiers = element.modifiers();
        for (Modifier modifier : modifiers) {
            result.append("addModifier(")
                    .append(MODIFIER_CLASS)
                    .append(".")
                    .append(modifier)
                    .append(")");
        }

        for (TypedElementInfo parameterArgument : element.parameterArguments()) {
            result.append(".addParameterArgument(")
                    .append(toCreate(parameterArgument))
                    .append(")");
        }

        return result.append(".build()").toString();
    }
}
