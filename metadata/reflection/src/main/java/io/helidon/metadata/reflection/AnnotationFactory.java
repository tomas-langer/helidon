package io.helidon.metadata.reflection;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;

public final class AnnotationFactory {
    private AnnotationFactory() {
    }

    public static List<Annotation> create(AnnotatedElement annotated) {
        List<Annotation> annotations = new ArrayList<>();
        java.lang.annotation.Annotation[] declared = annotated.getDeclaredAnnotations();
        for (java.lang.annotation.Annotation annotation : declared) {
            annotations.add(create(annotation));
        }
        return List.copyOf(annotations);
    }

    public static Annotation create(java.lang.annotation.Annotation annotation) {
        TypeName type = TypeName.create(annotation.annotationType());
        var set = new HashSet<TypeName>();
        set.add(TypeNames.INHERITED);
        set.add(TypeNames.TARGET);
        set.add(TypeNames.RETENTION);
        set.add(TypeNames.DOCUMENTED);
        set.remove(type);

        // it must return, as we removed our type from the processed type
        return createAnnotation(annotation, set)
                .orElseThrow();

    }

    // basically the same semantics as in `AptAnnotationFactory` (and scan based annotation factory)
    private static Optional<Annotation> createAnnotation(java.lang.annotation.Annotation annotation,
                                                         Set<TypeName> processedTypes) {
        TypeName type = TypeName.create(annotation.annotationType());

        if (processedTypes.contains(type)) {
            // this was already processed when handling this annotation, no need to add it
            return Optional.empty();
        }

        Annotation.Builder builder = Annotation.builder();

        Stream.of(annotation.annotationType()
              .getDeclaredAnnotations())
                .map(it -> {
                    var newProcessed = new HashSet<>(processedTypes);
                    newProcessed.add(type);
                    return createAnnotation(it, newProcessed);
                })
                .flatMap(Optional::stream)
                .forEach(builder::addMetaAnnotation);

        return Optional.of(builder
                                   .typeName(type)
                                   .values(extractAnnotationValues(annotation))
                                   .build());
    }

    private static Map<String, Object> extractAnnotationValues(java.lang.annotation.Annotation annotation) {
        Map<String, Object> result = new LinkedHashMap<>();

        Stream.of(annotation.annotationType()
                .getDeclaredMethods())
                .filter(it -> Modifier.isPublic(it.getModifiers()))
                .forEach(method -> {
                    String name = method.getName();
                    Object value;
                    try {
                        value = method.invoke(annotation);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to invoke annotation method, cannot analyze it", e);
                    }
                    result.put(name, value);
                });

        return result;
    }
}
