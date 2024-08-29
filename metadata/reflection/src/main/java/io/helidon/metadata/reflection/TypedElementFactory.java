package io.helidon.metadata.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

public final class TypedElementFactory {
    private TypedElementFactory() {
    }

    public static TypedElementInfo create(Method method) {
        int modifiers = method.getModifiers();

        var builder = TypedElementInfo.builder()
                .kind(ElementKind.METHOD)
                .annotations(AnnotationFactory.create(method))
                .accessModifier(accessModifier(modifiers))
                .elementModifiers(methodModifiers(method, modifiers))
                .throwsChecked(checkedExceptions(method))
                .typeName(TypeName.create(method.getGenericReturnType()))
                .elementTypeAnnotations(AnnotationFactory.create(method.getAnnotatedReturnType()))
                .elementName(method.getName())
                .originatingElement(method)
                .enclosingType(TypeName.create(method.getDeclaringClass()));

        return builder.build();
    }

    private static Set<TypeName> checkedExceptions(Method method) {
        return Stream.of(method.getExceptionTypes())
                .filter(Exception.class::isAssignableFrom)
                .map(TypeName::create)
                .collect(Collectors.toSet());
    }

    private static Set<io.helidon.common.types.Modifier> methodModifiers(Method method, int modifiers) {
        Set<io.helidon.common.types.Modifier> result = EnumSet.noneOf(io.helidon.common.types.Modifier.class);

        elementModifiers(modifiers, result);

        if (method.isDefault()) {
            result.add(io.helidon.common.types.Modifier.DEFAULT);
        }
        if (Modifier.isAbstract(modifiers)) {
            result.add(io.helidon.common.types.Modifier.ABSTRACT);
        }
        if (Modifier.isSynchronized(modifiers)) {
            result.add(io.helidon.common.types.Modifier.SYNCHRONIZED);
        }
        if (Modifier.isNative(modifiers)) {
            result.add(io.helidon.common.types.Modifier.NATIVE);
        }

        return result;
    }

    private static void elementModifiers( int modifiers, Set<io.helidon.common.types.Modifier> result) {
        if (Modifier.isFinal(modifiers)) {
            result.add(io.helidon.common.types.Modifier.FINAL);
        }
        if (Modifier.isStatic(modifiers)) {
            result.add(io.helidon.common.types.Modifier.STATIC);
        }
    }

    private static AccessModifier accessModifier(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return AccessModifier.PUBLIC;
        }
        if (Modifier.isProtected(modifiers)) {
            return AccessModifier.PROTECTED;
        }
        if (Modifier.isPrivate(modifiers)) {
            return AccessModifier.PRIVATE;
        }
        return AccessModifier.PACKAGE_PRIVATE;
    }
}
