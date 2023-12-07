package io.helidon.inject.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.helidon.builder.api.Prototype;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;

class QualifierSupport {
    static final class CustomMethods {
        /**
         * The type name for {@link io.helidon.inject.service.Inject.ClassNamed}.
         */
        private static final TypeName CLASS_NAMED = TypeName.create(Inject.ClassNamed.class);

        private CustomMethods() {
        }

        /**
         * Creates a qualifier from an annotation.
         *
         * @param qualifierType the qualifier type
         * @return qualifier
         */
        @Prototype.FactoryMethod
        static Qualifier create(Class<? extends java.lang.annotation.Annotation> qualifierType) {
            Objects.requireNonNull(qualifierType);
            TypeName typeName = TypeName.create(qualifierType);
            TypeName qualifierTypeName = maybeNamed(typeName);
            return Qualifier.builder().typeName(qualifierTypeName).build();
        }

        /**
         * Creates a qualifier with a value from an annotation.
         *
         * @param qualifierType the qualifier type
         * @param value         the value property
         * @return qualifier
         */
        @Prototype.FactoryMethod
        static Qualifier create(Class<? extends java.lang.annotation.Annotation> qualifierType, String value) {
            Objects.requireNonNull(qualifierType);
            TypeName typeName = TypeName.create(qualifierType);
            TypeName qualifierTypeName = maybeNamed(typeName);
            return Qualifier.builder()
                    .typeName(qualifierTypeName)
                    .putValue("value", value)
                    .build();
        }

        /**
         * Creates a qualifier from an annotation.
         *
         * @param annotation the qualifier annotation
         * @return qualifier
         */
        @Prototype.FactoryMethod
        static Qualifier create(Annotation annotation) {
            Objects.requireNonNull(annotation);
            if (annotation instanceof Qualifier qualifier) {
                return qualifier;
            }
            return Qualifier.builder()
                    .typeName(maybeNamed(annotation.typeName()))
                    .values(removeEmptyProperties(annotation.values()))
                    .build();
        }

        /**
         * Creates a {@link io.helidon.inject.service.Inject.Named} qualifier.
         *
         * @param name the name
         * @return named qualifier
         */
        @Prototype.FactoryMethod
        static Qualifier createNamed(String name) {
            Objects.requireNonNull(name);
            return Qualifier.builder()
                    .typeName(Inject.Named.TYPE_NAME)
                    .value(name)
                    .build();
        }

        /**
         * Creates a {@link io.helidon.inject.service.Inject.Named} qualifier.
         *
         * @param name the name
         * @return named qualifier
         */
        @Prototype.FactoryMethod
        static Qualifier createNamed(Inject.Named name) {
            Objects.requireNonNull(name);
            Qualifier.Builder builder = Qualifier.builder()
                    .typeName(Inject.Named.TYPE_NAME);
            if (!name.value().isEmpty()) {
                builder.value(name.value());
            }
            return builder.build();
        }

        /**
         * Creates a {@link io.helidon.inject.service.Inject.Named} qualifier.
         *
         * @param name the name
         * @return named qualifier
         */
        @Prototype.FactoryMethod
        static Qualifier createNamed(Inject.ClassNamed name) {
            Objects.requireNonNull(name);
            return Qualifier.builder()
                    .typeName(Inject.Named.TYPE_NAME)
                    .value(name.value().getName())
                    .build();
        }

        /**
         * Creates a {@link io.helidon.inject.service.Inject.Named} qualifier from a class name.
         *
         * @param className class whose name will be used
         * @return named qualifier
         */
        @Prototype.FactoryMethod
        static Qualifier createNamed(Class<?> className) {
            Objects.requireNonNull(className);
            return Qualifier.builder()
                    .typeName(Inject.Named.TYPE_NAME)
                    .value(className.getName())
                    .build();
        }

        private static TypeName maybeNamed(TypeName qualifierType) {
            if (CLASS_NAMED.equals(qualifierType)) {
                return Inject.Named.TYPE_NAME;
            }
            return qualifierType;
        }

        private static Map<String, Object> removeEmptyProperties(Map<String, Object> values) {
            HashMap<String, Object> result = new HashMap<>(values);
            result.entrySet().removeIf(entry -> {
                Object value = entry.getValue();
                return value instanceof String str && str.isBlank();
            });
            return result;
        }
    }
}
