/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.nima.faulttolerance.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import javax.lang.model.element.ElementKind;

import io.helidon.common.types.AnnotationAndValue;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNameDefault;
import io.helidon.common.types.TypedElementName;
import io.helidon.pico.tools.CustomAnnotationTemplateRequest;
import io.helidon.pico.tools.CustomAnnotationTemplateResponse;
import io.helidon.pico.tools.GenericTemplateCreator;
import io.helidon.pico.tools.GenericTemplateCreatorRequest;
import io.helidon.pico.tools.GenericTemplateCreatorRequestDefault;
import io.helidon.pico.tools.ToolsException;
import io.helidon.pico.tools.spi.CustomAnnotationTemplateCreator;

/**
 * Annotation processor that generates a service for each method annotated with retry annotation.
 * Service provider implementation of a {@link io.helidon.pico.tools.spi.CustomAnnotationTemplateCreator}.
 */
public class CircuitBreakerMethodCreator extends FtMethodCreatorBase implements CustomAnnotationTemplateCreator {
    private static final String FT_ANNOTATION = "io.helidon.nima.faulttolerance.FaultTolerance.CircuitBreaker";

    /**
     * Default constructor used by the {@link java.util.ServiceLoader}.
     */
    public CircuitBreakerMethodCreator() {
    }

    @Override
    public Set<String> annoTypes() {
        return Set.of(FT_ANNOTATION);
    }

    @Override
    public Optional<CustomAnnotationTemplateResponse> create(CustomAnnotationTemplateRequest request) {
        TypeInfo enclosingType = request.enclosingTypeInfo();

        if (!ElementKind.METHOD.name().equals(request.targetElement().elementTypeKind())) {
            // we are only interested in methods, not in classes
            throw new ToolsException(FT_ANNOTATION + " can only be defined on methods");
        }

        String classname = className(request.annoTypeName(), enclosingType.typeName(), request.targetElement().elementName());
        TypeName generatedTypeName = TypeNameDefault.create(enclosingType.typeName().packageName(), classname);

        GenericTemplateCreator genericTemplateCreator = request.genericTemplateCreator();
        GenericTemplateCreatorRequest genericCreatorRequest = GenericTemplateCreatorRequestDefault.builder()
                .customAnnotationTemplateRequest(request)
                .template(Templates.loadTemplate("circuit-breaker-method.java.hbs"))
                .generatedTypeName(generatedTypeName)
                .overrideProperties(addProperties(request, enclosingType))
                .build();
        return genericTemplateCreator.create(genericCreatorRequest);
    }

    private Map<String, Object> addProperties(CustomAnnotationTemplateRequest request, TypeInfo enclosingType) {
        CircuitBreakerDef ftDef = new CircuitBreakerDef();
        TypedElementName targetElement = request.targetElement();
        Map<String, Object> response = new HashMap<>();
        String beanType = enclosingType.typeName().name();

        // http.methodName - name of the method in source code (not HTTP Method)
        ftDef.methodName = targetElement.elementName();

        AnnotationAndValue ftAnnotation = getAnnotation(targetElement.annotations(),
                                                        FT_ANNOTATION,
                                                        beanType);
        ftDef.name = ftAnnotation.value("name").filter(Predicate.not(String::isBlank)).orElse(null);
        if (ftDef.name != null) {
            ftDef.named = true;
        }
        if (ftDef.named) {
            ftDef.customName = ftDef.name + "-" + UUID.randomUUID();
        } else {
            ftDef.customName = beanType + "." + ftDef.methodName + "-" + System.identityHashCode(beanType);
        }

        ftDef.applyOn = ftAnnotation.value("applyOn")
                .filter(Predicate.not(String::isBlank))
                .map(it -> List.of(it.split(",")))
                .orElseGet(List::of);
        ftDef.skipOn = ftAnnotation.value("skipOn")
                .filter(Predicate.not(String::isBlank))
                .map(it -> List.of(it.split(",")))
                .orElseGet(List::of);

        // method parameters
        ftDef.paramTypes = request.targetElementArgs()
                .stream()
                .map(TypedElementName::typeName)
                .map(TypeName::name)
                .toList();

        ftDef.timeUnit = ftAnnotation.value("timeUnit")
                .orElse("SECONDS");

        ftDef.delay = ftAnnotation.value("delayTime")
                .map(Long::parseLong)
                .orElse(5L);

        ftDef.errorRatio = ftAnnotation.value("errorRatio")
                .map(Integer::parseInt)
                .orElse(60);

        ftDef.successThreshold = ftAnnotation.value("successThreshold")
                .map(Integer::parseInt)
                .orElse(1);

        ftDef.volume = ftAnnotation.value("volume")
                .map(Integer::parseInt)
                .orElse(10);

        response.put("breaker", ftDef);
        return response;
    }

    private AnnotationAndValue getAnnotation(List<AnnotationAndValue> annotations, String annotationType, String beanType) {
        for (AnnotationAndValue annotation : annotations) {
            if (annotationType.equals(annotation.typeName().name())) {
                return annotation;
            }
        }
        throw new ToolsException("Annotation " + annotationType + " must be defined on the processed type: " + beanType);
    }

    /**
     * Needed for template processing.
     * Do not use.
     */
    @Deprecated(since = "1.0.0")
    public static class CircuitBreakerDef {
        // name of the method that is annotated
        private String methodName;
        private List<String> applyOn;
        private List<String> skipOn;
        private List<String> paramTypes;
        // is this a named retry (non empty name on retry annotation)
        private boolean named;
        private String name;
        // name to use when named and not found, or when creating a new instance for unnamed
        private String customName;

        // common
        private String timeUnit;
        private long delay;
        private int errorRatio;
        private int volume;
        private int successThreshold;

        public String getMethodName() {
            return methodName;
        }

        public List<String> getApplyOn() {
            return applyOn;
        }

        public List<String> getSkipOn() {
            return skipOn;
        }

        public List<String> getParamTypes() {
            return paramTypes;
        }

        public String getCustomName() {
            return customName;
        }

        public String getTimeUnit() {
            return timeUnit;
        }

        public long getDelay() {
            return delay;
        }

        public boolean isNamed() {
            return named;
        }

        public String getName() {
            return name;
        }

        public int getErrorRatio() {
            return errorRatio;
        }

        public int getVolume() {
            return volume;
        }

        public int getSuccessThreshold() {
            return successThreshold;
        }
    }
}
