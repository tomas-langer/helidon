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
public class RetryMethodCreator extends FtMethodCreatorBase implements CustomAnnotationTemplateCreator {
    private static final String RETRY_ANNOTATION = "io.helidon.nima.faulttolerance.FaultTolerance.Retry";

    /**
     * Default constructor used by the {@link java.util.ServiceLoader}.
     */
    public RetryMethodCreator() {
    }

    @Override
    public Set<String> annoTypes() {
        return Set.of(RETRY_ANNOTATION);
    }

    @Override
    public Optional<CustomAnnotationTemplateResponse> create(CustomAnnotationTemplateRequest request) {
        TypeInfo enclosingType = request.enclosingTypeInfo();

        if (!ElementKind.METHOD.name().equals(request.targetElement().elementTypeKind())) {
            // we are only interested in methods, not in classes
            throw new ToolsException(RETRY_ANNOTATION + " can only be defined on methods");
        }

        String classname = className(request.annoTypeName(), enclosingType.typeName(), request.targetElement().elementName());
        TypeName generatedTypeName = TypeNameDefault.create(enclosingType.typeName().packageName(), classname);

        GenericTemplateCreator genericTemplateCreator = request.genericTemplateCreator();
        GenericTemplateCreatorRequest genericCreatorRequest = GenericTemplateCreatorRequestDefault.builder()
                .customAnnotationTemplateRequest(request)
                .template(Templates.loadTemplate("retry-method.java.hbs"))
                .generatedTypeName(generatedTypeName)
                .overrideProperties(addProperties(request, enclosingType))
                .build();
        return genericTemplateCreator.create(genericCreatorRequest);
    }

    private Map<String, Object> addProperties(CustomAnnotationTemplateRequest request, TypeInfo enclosingType) {
        RetryDef retry = new RetryDef();
        TypedElementName targetElement = request.targetElement();
        Map<String, Object> response = new HashMap<>();
        String beanType = enclosingType.typeName().name();

        // http.methodName - name of the method in source code (not HTTP Method)
        retry.methodName = targetElement.elementName();

        AnnotationAndValue retryAnnotation = getAnnotation(targetElement.annotations(),
                                                           RETRY_ANNOTATION,
                                                           beanType);
        retry.retryName = retryAnnotation.value("name").filter(Predicate.not(String::isBlank)).orElse(null);
        if (retry.retryName != null) {
            retry.retryNamed = true;
        }
        retry.applyOn = retryAnnotation.value("applyOn")
                .filter(Predicate.not(String::isBlank))
                .map(it -> List.of(it.split(",")))
                .orElseGet(List::of);
        retry.skipOn = retryAnnotation.value("skipOn")
                .filter(Predicate.not(String::isBlank))
                .map(it -> List.of(it.split(",")))
                .orElseGet(List::of);

        // method parameters
        retry.paramTypes = request.targetElementArgs()
                .stream()
                .map(TypedElementName::typeName)
                .map(TypeName::name)
                .toList();

        retry.calls = retryAnnotation.value("calls")
                .map(Integer::parseInt)
                .orElse(3);

        retry.timeUnit = retryAnnotation.value("timeUnit")
                .orElse("MILLIS");

        retry.delay = retryAnnotation.value("delayTime")
                .map(Long::parseLong)
                .orElse(200L);

        retry.overallTimeout = retryAnnotation.value("overallTimeout")
                .map(Long::parseLong)
                .orElse(1000L);

        retry.delayFactor = retryAnnotation.value("delayFactor")
                .map(Double::parseDouble)
                .orElse(1.0);

        retry.jitter = retryAnnotation.value("jitterTime")
                .map(Long::parseLong)
                .orElse(-1L);

        if (retry.jitter < 0 || retry.delayFactor > 0) {
            retry.isDelaying = true;
            retry.delayFactor = retry.delayFactor < 0 ? 2 : retry.delayFactor;
        }

        if (retry.retryNamed) {
            retry.customName = retry.retryName + "-" + UUID.randomUUID();
        } else {
            // this is not fully random, but we may only get conflict on the same type, same method name
            retry.customName = beanType + "." + retry.methodName + "-" + System.identityHashCode(beanType);
        }

        response.put("retry", retry);
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
    public static class RetryDef {
        // name of the method that is annotated
        private String methodName;
        private List<String> applyOn;
        private List<String> skipOn;
        private List<String> paramTypes;
        // is this a named retry (non empty name on retry annotation)
        private boolean retryNamed;
        private String retryName;
        // name to use when named and not found, or when creating a new instance for unnamed
        private String customName;

        // common
        private long overallTimeout;
        private String timeUnit;
        private long delay;
        private int calls;

        // is delaying policy - if not, uses jitter
        private boolean isDelaying;
        private double delayFactor;

        // jitter policy
        private long jitter;

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

        public boolean isRetryNamed() {
            return retryNamed;
        }

        public String getRetryName() {
            return retryName;
        }

        public String getCustomName() {
            return customName;
        }

        public long getOverallTimeout() {
            return overallTimeout;
        }

        public String getTimeUnit() {
            return timeUnit;
        }

        public long getDelay() {
            return delay;
        }

        public int getCalls() {
            return calls;
        }

        public boolean isDelaying() {
            return isDelaying;
        }

        public double getDelayFactor() {
            return delayFactor;
        }

        public long getJitter() {
            return jitter;
        }
    }
}
