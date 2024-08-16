package io.helidon.declarative.codegen.http;

import java.util.List;

import io.helidon.codegen.CodegenException;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.declarative.codegen.http.model.ComputedHeader;
import io.helidon.declarative.codegen.http.model.HeaderValue;
import io.helidon.declarative.codegen.http.model.HttpAnnotated;
import io.helidon.declarative.codegen.http.model.HttpMethod;

import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_CONSUMES_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_PATH_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_PRODUCES_ANNOTATION;

public class RestUtil {
    public static HttpMethod httpMethodFromAnnotation(TypedElementInfo element, Annotation httpMethodAnnotation) {
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

    public static void path(List<Annotation> annotations, HttpAnnotated.BuilderBase<?, ?> builder) {
        Annotations.findFirst(HTTP_PATH_ANNOTATION, annotations)
                .flatMap(Annotation::stringValue)
                .ifPresent(builder::path);
    }

    public static void consumes(List<Annotation> annotations, HttpAnnotated.BuilderBase<?, ?> builder) {
        Annotations.findFirst(HTTP_CONSUMES_ANNOTATION, annotations)
                .flatMap(Annotation::stringValues)
                .ifPresent(builder::consumes);
    }

    public static void produces(List<Annotation> annotations, HttpAnnotated.BuilderBase<?, ?> builder) {
        Annotations.findFirst(HTTP_PRODUCES_ANNOTATION, annotations)
                .flatMap(Annotation::stringValues)
                .ifPresent(builder::produces);
    }

    public static void headers(List<Annotation> typeAnnotations,
                               HttpAnnotated.BuilderBase<?, ?> builder,
                               TypeName annotationType) {
        Annotations.findFirst(annotationType, typeAnnotations)
                .flatMap(Annotation::annotationValues)
                .stream()
                .flatMap(List::stream)
                .forEach(headerAnnotation -> {
                    String name = headerAnnotation.stringValue("name").orElseThrow();
                    String value = headerAnnotation.stringValue("value").orElseThrow();
                    builder.addHeader(new HeaderValue(name, value));
                });
    }

    public static void computedHeaders(List<Annotation> annotations,
                                       HttpAnnotated.BuilderBase<?, ?> builder,
                                       TypeName annotationType) {
        Annotations.findFirst(annotationType, annotations)
                .flatMap(Annotation::annotationValues)
                .stream()
                .flatMap(List::stream)
                .forEach(headerAnnotation -> {
                    String name = headerAnnotation.stringValue("name").orElseThrow();
                    TypeName producer = headerAnnotation.typeValue("producerClass").orElseThrow();
                    boolean required = headerAnnotation.booleanValue("required").orElse(true);
                    builder.addComputedHeader(new ComputedHeader(name, producer, required));
                });
    }

    public static TypeName supplierOf(TypeName endpointType) {
        return TypeName.builder(TypeNames.SUPPLIER)
                .addTypeArgument(endpointType)
                .build();
    }
}
