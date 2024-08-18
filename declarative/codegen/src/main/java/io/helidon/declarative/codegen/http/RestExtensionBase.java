/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.declarative.codegen.http;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Field;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.declarative.codegen.FieldNames;
import io.helidon.declarative.codegen.http.model.ComputedHeader;
import io.helidon.declarative.codegen.http.model.HeaderValue;
import io.helidon.declarative.codegen.http.model.HttpAnnotated;
import io.helidon.declarative.codegen.http.model.HttpMethod;

import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_CONSUMES_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER_NAME;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER_NAMES;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER_PARAM_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER_VALUES;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_MEDIA_TYPE;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_METHOD;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_PATH_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_PRODUCES_ANNOTATION;

/**
 * A base class for HTTP Rest extensions (server and client).
 */
public abstract class RestExtensionBase {
    /**
     * Constructor with no side effects.
     */
    protected RestExtensionBase() {
    }

    /**
     * Add a constant for each header name.
     *
     * @param annotations annotations of the current element
     * @param constants   header name constants
     */
    protected void addHeaderNameConstants(List<Annotation> annotations,
                                          FieldNames<String> constants) {
        for (Annotation annotation : annotations) {
            if (annotation.typeName().equals(HTTP_HEADER_PARAM_ANNOTATION)) {
                annotation.stringValue().ifPresent(constants::add);
            }
        }
    }

    /**
     * Add a constant for each computed header name.
     *
     * @param headers   lis of computed headers
     * @param constants header name constants
     */
    protected void addComputedHeaderConstants(List<ComputedHeader> headers,
                                              FieldNames<String> constants) {
        headers.stream()
                .map(ComputedHeader::name)
                .forEach(constants::add);
    }

    /**
     * Add a constant for each HTTP Method, unless it is a built-in method.
     * For example {@code GET} will not have a constant created, while {@code LIST} would.
     *
     * @param method    HTTP method
     * @param constants HTTP method constants
     */
    protected void addMethodConstant(HttpMethod method,
                                     FieldNames<String> constants) {
        if (!method.builtIn()) {
            constants.add(method.name());
        }
    }

    /**
     * Add a constant field for each HTTP method constant.
     *
     * @param classModel class model to add constant to
     * @param constants  HTTP method constants (mapping of constant name to HTTP method)
     */
    protected void httpMethodConstants(ClassModel.Builder classModel, FieldNames<String> constants) {
        constants.forEach((method, constant) -> {
            classModel.addField(headerValue -> headerValue
                    .update(this::privateConstant)
                    .type(HTTP_METHOD)
                    .name(constant)
                    .addContent(HTTP_METHOD)
                    .addContent(".create(\"")
                    .addContent(method)
                    .addContent("\")"));
        });
    }

    /**
     * Add a constant field for each HTTP header value.
     *
     * @param classModel class model to add constant to
     * @param constants  HTTP header value constants (mapping of constant name to a header value)
     */
    protected void headerValueConstants(ClassModel.Builder classModel, FieldNames<HeaderValue> constants) {
        constants.forEach((header, constant) -> {
            classModel.addField(headerValue -> headerValue
                    .update(this::privateConstant)
                    .type(HTTP_HEADER)
                    .name(constant)
                    .addContent(HTTP_HEADER_VALUES)
                    .addContent(".create(\"")
                    .addContent(header.name())
                    .addContent("\", \"")
                    .addContent(header.value())
                    .addContent("\")"));
        });
    }

    /**
     * Add a constant field for each HTTP header name.
     *
     * @param classModel class model to add constant to
     * @param constants  HTTP header name constants (mapping of constant name to a header name)
     */
    protected void headerNameConstants(ClassModel.Builder classModel, FieldNames<String> constants) {
        constants.forEach((name, constant) -> {
            classModel.addField(headerValue -> headerValue
                    .update(this::privateConstant)
                    .type(HTTP_HEADER_NAME)
                    .name(constant)
                    .addContent(HTTP_HEADER_NAMES)
                    .addContent(".create(\"")
                    .addContent(name)
                    .addContent("\")"));
        });
    }

    /**
     * Add a constant field for each HTTP media type.
     *
     * @param classModel class model to add constant to
     * @param constants  HTTP media type constants (mapping of constant name to an HTTP media type string)
     */
    protected void mediaTypeConstants(ClassModel.Builder classModel, FieldNames<String> constants) {
        constants.forEach((value, constant) -> {
            classModel.addField(headerValue -> headerValue
                    .update(this::privateConstant)
                    .type(HTTP_MEDIA_TYPE)
                    .name(constant)
                    .addContent(HTTP_MEDIA_TYPE)
                    .addContent(".create(\"")
                    .addContent(value)
                    .addContent("\")"));
        });
    }

    /**
     * Extract HTTP Method from its annotation.
     *
     * @param element              annotated element
     * @param httpMethodAnnotation Http method annotation
     * @return http method abstraction for code generation
     */
    protected HttpMethod httpMethodFromAnnotation(TypedElementInfo element, Annotation httpMethodAnnotation) {
        String method = httpMethodAnnotation.stringValue()
                .map(String::toUpperCase)
                .orElseThrow(() -> new CodegenException("Could not find @HttpMethod meta annotation for method "
                                                                + element.elementName(),
                                                        element.originatingElement().orElseGet(element::typeName)));
        return switch (method) {
            case "GET", "PUT", "POST", "DELETE", "HEAD", "OPTIONS", "PATCH", "TRACE" -> new HttpMethod(method, true);
            default -> new HttpMethod(method, false);
        };
    }

    /**
     * Find path in the annotations and set it on the builder.
     *
     * @param annotations all element annotations
     * @param builder     element builder
     */
    protected void path(List<Annotation> annotations, HttpAnnotated.BuilderBase<?, ?> builder) {
        Annotations.findFirst(HTTP_PATH_ANNOTATION, annotations)
                .flatMap(Annotation::stringValue)
                .ifPresent(builder::path);
    }

    /**
     * Find consumes in the annotations and set it on the builder.
     *
     * @param annotations all element annotations
     * @param builder     element builder
     */
    protected void consumes(List<Annotation> annotations, HttpAnnotated.BuilderBase<?, ?> builder) {
        Annotations.findFirst(HTTP_CONSUMES_ANNOTATION, annotations)
                .flatMap(Annotation::stringValues)
                .ifPresent(builder::consumes);
    }

    /**
     * Find produces in the annotations and set it on the builder.
     *
     * @param annotations all element annotations
     * @param builder     element builder
     */
    protected void produces(List<Annotation> annotations, HttpAnnotated.BuilderBase<?, ?> builder) {
        Annotations.findFirst(HTTP_PRODUCES_ANNOTATION, annotations)
                .flatMap(Annotation::stringValues)
                .ifPresent(builder::produces);
    }

    /**
     * Find headers in the annotations and set it on the builder.
     *
     * @param annotations            all element annotations
     * @param builder                element builder
     * @param repeatedAnnotationType type of the annotation to be found (repeated)
     * @param singleAnnotationType   type of the annotation to be found (single)
     */
    protected void headers(List<Annotation> annotations,
                           HttpAnnotated.BuilderBase<?, ?> builder,
                           TypeName repeatedAnnotationType,
                           TypeName singleAnnotationType) {
        AtomicBoolean found = new AtomicBoolean(false);
        Annotations.findFirst(repeatedAnnotationType, annotations)
                .flatMap(Annotation::annotationValues)
                .stream()
                .flatMap(List::stream)
                .forEach(headerAnnotation -> {
                    found.set(true);
                    String name = headerAnnotation.stringValue("name").orElseThrow();
                    String value = headerAnnotation.stringValue("value").orElseThrow();
                    builder.addHeader(new HeaderValue(name, value));
                });

        if (!found.get()) {
            Annotations.findFirst(singleAnnotationType, annotations)
                    .ifPresent(headerAnnotation -> {
                        String name = headerAnnotation.stringValue("name").orElseThrow();
                        String value = headerAnnotation.stringValue("value").orElseThrow();
                        builder.addHeader(new HeaderValue(name, value));
                    });
        }
    }

    /**
     * Find computed headers in the annotations and set it on the builder.
     *
     * @param annotations            all element annotations
     * @param builder                element builder
     * @param repeatedAnnotationType type of the annotation to be found (repeated)
     * @param singleAnnotationType   type of the annotation to be found (single)
     */
    protected void computedHeaders(List<Annotation> annotations,
                                   HttpAnnotated.BuilderBase<?, ?> builder,
                                   TypeName repeatedAnnotationType,
                                   TypeName singleAnnotationType) {
        AtomicBoolean found = new AtomicBoolean(false);
        Annotations.findFirst(repeatedAnnotationType, annotations)
                .flatMap(Annotation::annotationValues)
                .stream()
                .flatMap(List::stream)
                .forEach(headerAnnotation -> {
                    found.set(true);
                    String name = headerAnnotation.stringValue("name").orElseThrow();
                    TypeName producer = headerAnnotation.typeValue("producerClass").orElseThrow();
                    builder.addComputedHeader(new ComputedHeader(name, producer));
                });
        if (!found.get()) {
            Annotations.findFirst(singleAnnotationType, annotations)
                    .ifPresent(headerAnnotation -> {
                        String name = headerAnnotation.stringValue("name").orElseThrow();
                        TypeName producer = headerAnnotation.typeValue("producerClass").orElseThrow();
                        builder.addComputedHeader(new ComputedHeader(name, producer));
                    });
        }
    }

    /**
     * Create a type name that is a supplier of the provided type.
     *
     * @param type type to supply
     * @return supplier type
     */
    protected TypeName supplierOf(TypeName type) {
        return TypeName.builder(TypeNames.SUPPLIER)
                .addTypeArgument(type)
                .build();
    }

    /**
     * Update a field builder to make it a {@code private static final} field.
     *
     * @param builder builder to update
     */
    protected void privateConstant(Field.Builder builder) {
        builder.accessModifier(AccessModifier.PRIVATE)
                .isStatic(true)
                .isFinal(true);
    }
}
