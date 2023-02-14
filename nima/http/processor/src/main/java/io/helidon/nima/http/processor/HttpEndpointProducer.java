/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.nima.http.processor;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.lang.model.element.ElementKind;

import io.helidon.builder.processor.spi.TypeInfo;
import io.helidon.builder.types.DefaultTypeName;
import io.helidon.builder.types.TypeName;
import io.helidon.common.http.Path;
import io.helidon.pico.QualifierAndValue;
import io.helidon.pico.tools.CustomAnnotationTemplateCreator;
import io.helidon.pico.tools.CustomAnnotationTemplateRequest;
import io.helidon.pico.tools.CustomAnnotationTemplateResponse;

/**
 * Annotation processor that generates a service for each class annotated with {@link io.helidon.common.http.Path} annotation.
 * Service provider implementation of a {@link io.helidon.pico.tools.CustomAnnotationTemplateCreator}.
 */
public class HttpEndpointProducer implements CustomAnnotationTemplateCreator {
    /**
     * Default constructor used by the {@link java.util.ServiceLoader}.
     */
    public HttpEndpointProducer() {
    }

    @Override
    public Set<Class<? extends Annotation>> annoTypes() {
        return Set.of(Path.class);
    }

    @Override
    public Optional<CustomAnnotationTemplateResponse> create(CustomAnnotationTemplateRequest request) {
        TypeInfo enclosingType = request.enclosingTypeInfo();
        if (!ElementKind.CLASS.name().equals(enclosingType.typeKind())) {
            // we are only interested in classes, not in methods
            return Optional.empty();
        }

        String classname = enclosingType.typeName().className() + "_GeneratedService";
        TypeName generatedType = DefaultTypeName.create(enclosingType.typeName().packageName(), classname);

        Supplier<String> templateSupplier = () -> Templates.loadTemplate("nima", "http-endpoint.java.hbs");

        return request.templateHelperTools().produceStandardCodeGenResponse(request,
                                                                            generatedType,
                                                                            templateSupplier,
                                                                            it -> addProperties(request, it));
    }

    private Map<String, Object> addProperties(CustomAnnotationTemplateRequest request,
                                              Map<String, Object> currentProperties) {
        Map<String, Object> response = new HashMap<>(currentProperties);

        Set<QualifierAndValue> qualifiers = request.serviceInfo().qualifiers();
        for (QualifierAndValue qualifier : qualifiers) {
            if (qualifier.qualifierTypeName().equals(Path.class.getName())) {
                response.put("http", new HttpDef(qualifier.value().orElse("/")));
                break;
            }
        }

        return response;
    }

    /**
     * Needed for template processing.
     * Do not use.
     */
    @Deprecated(since = "4.0.0")
    public record HttpDef(String path) {
    }
}
