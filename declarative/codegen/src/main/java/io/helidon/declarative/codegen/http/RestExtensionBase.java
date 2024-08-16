package io.helidon.declarative.codegen.http;

import java.util.List;

import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeInfo;
import io.helidon.declarative.codegen.ConstantField;
import io.helidon.declarative.codegen.Constants;
import io.helidon.declarative.codegen.http.model.ComputedHeader;
import io.helidon.declarative.codegen.http.model.HeaderValue;
import io.helidon.declarative.codegen.http.model.HttpMethod;
import io.helidon.service.codegen.ServiceCodegenTypes;

import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER_NAME;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER_NAMES;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER_PARAM_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER_VALUES;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_MEDIA_TYPE;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_METHOD;

public abstract class RestExtensionBase {
    protected void addHeaderNameConstants(List<Annotation> annotations,
                                          Constants<String> constants) {
        for (Annotation annotation : annotations) {
            if (annotation.typeName().equals(HTTP_HEADER_PARAM_ANNOTATION)) {
                annotation.stringValue().ifPresent(constants::add);
            }
        }
    }

    protected void addComputedHeaderConstants(List<ComputedHeader> headers,
                                              Constants<String> constants) {
        headers.stream()
                .map(ComputedHeader::name)
                .forEach(constants::add);
    }

    protected void addMethodConstant(HttpMethod method,
                                     Constants<String> constants) {
        if (!method.builtIn()) {
            constants.add(method.name());
        }
    }

    protected void httpMethodConstants(ClassModel.Builder classModel, Constants<String> constants) {
        constants.forEach((method, constant) -> {
            classModel.addField(headerValue -> headerValue
                    .update(ConstantField::privateConstant)
                    .type(HTTP_METHOD)
                    .name(constant)
                    .addContent(HTTP_METHOD)
                    .addContent(".create(\"")
                    .addContent(method)
                    .addContent("\")"));
        });
    }

    protected void headerValueConstants(ClassModel.Builder classModel, Constants<HeaderValue> constants) {
        constants.forEach((header, constant) -> {
            classModel.addField(headerValue -> headerValue
                    .update(ConstantField::privateConstant)
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

    protected void headerNameConstants(ClassModel.Builder classModel, Constants<String> constants) {
        constants.forEach((name, constant) -> {
            classModel.addField(headerValue -> headerValue
                    .update(ConstantField::privateConstant)
                    .type(HTTP_HEADER_NAME)
                    .name(constant)
                    .addContent(HTTP_HEADER_NAMES)
                    .addContent(".create(\"")
                    .addContent(name)
                    .addContent("\")"));
        });
    }

    protected void mediaTypeConstants(ClassModel.Builder classModel, Constants<String> constants) {
        constants.forEach((value, constant) -> {
            classModel.addField(headerValue -> headerValue
                    .update(ConstantField::privateConstant)
                    .type(HTTP_MEDIA_TYPE)
                    .name(constant)
                    .addContent(HTTP_MEDIA_TYPE)
                    .addContent(".create(\"")
                    .addContent(value)
                    .addContent("\")"));
        });
    }
}
