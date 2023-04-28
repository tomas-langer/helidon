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
import java.util.function.Predicate;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

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
 * Annotation processor that generates a service for each method annotated with fallback annotation.
 * Service provider implementation of a {@link io.helidon.pico.tools.spi.CustomAnnotationTemplateCreator}.
 */
public class FallbackMethodCreator extends FtMethodCreatorBase implements CustomAnnotationTemplateCreator {
    private static final String FALLBACK_ANNOTATION = "io.helidon.nima.faulttolerance.FaultTolerance.Fallback";

    /**
     * Default constructor used by the {@link java.util.ServiceLoader}.
     */
    public FallbackMethodCreator() {
    }

    @Override
    public Set<String> annoTypes() {
        return Set.of(FALLBACK_ANNOTATION);
    }

    @Override
    public Optional<CustomAnnotationTemplateResponse> create(CustomAnnotationTemplateRequest request) {
        TypeInfo enclosingType = request.enclosingTypeInfo();

        if (!ElementKind.METHOD.name().equals(request.targetElement().elementTypeKind())) {
            // we are only interested in methods, not in classes
            throw new ToolsException(FALLBACK_ANNOTATION + " can only be defined on methods");
        }

        String classname = className(request.annoTypeName(), enclosingType.typeName(), request.targetElement().elementName());
        TypeName generatedTypeName = TypeNameDefault.create(enclosingType.typeName().packageName(), classname);

        GenericTemplateCreator genericTemplateCreator = request.genericTemplateCreator();
        GenericTemplateCreatorRequest genericCreatorRequest = GenericTemplateCreatorRequestDefault.builder()
                .customAnnotationTemplateRequest(request)
                .template(Templates.loadTemplate("fallback-method.java.hbs"))
                .generatedTypeName(generatedTypeName)
                .overrideProperties(addProperties(request, enclosingType))
                .build();
        return genericTemplateCreator.create(genericCreatorRequest);
    }

    private Map<String, Object> addProperties(CustomAnnotationTemplateRequest request, TypeInfo enclosingType) {
        FallbackDef fallback = new FallbackDef();
        fallback.beanType = enclosingType.typeName().name();

        TypedElementName targetElement = request.targetElement();
        Map<String, Object> response = new HashMap<>();
        TypeName returnType = targetElement.typeName();
        if ("void".equals(returnType.className())) {
            fallback.returnType = "Void";
            fallback.returnVoid = true;
        } else {
            fallback.returnType = returnType.name();
        }

        // http.methodName - name of the method in source code (not HTTP Method)
        fallback.methodName = targetElement.elementName();

        AnnotationAndValue fallbackAnnotation = getAnnotation(targetElement.annotations(),
                                                              FALLBACK_ANNOTATION,
                                                              fallback.beanType);
        fallback.fallbackName = fallbackAnnotation.value()
                .orElseThrow(() -> new ToolsException("Missing value on " + FALLBACK_ANNOTATION
                                                              + " on type: " + fallback.beanType));
        fallback.applyOn = fallbackAnnotation.value("applyOn")
                .filter(Predicate.not(String::isBlank))
                .map(it -> List.of(it.split(",")))
                .orElseGet(List::of);
        fallback.skipOn = fallbackAnnotation.value("skipOn")
                .filter(Predicate.not(String::isBlank))
                .map(it -> List.of(it.split(",")))
                .orElseGet(List::of);

        // method parameters
        fallback.paramTypes = request.targetElementArgs()
                .stream()
                .map(TypedElementName::typeName)
                .map(TypeName::name)
                .toList();

        // now we need to locate the fallback method
        // we do have enclosing type, so we need to iterate through its elements and find the matching method(s)
        List<TypedElementName> matchingMethodsByName = enclosingType.elementInfo()
                .stream()
                .filter(it -> TypeInfo.KIND_METHOD.equals(it.elementTypeKind()))
                .filter(it -> fallback.fallbackName.equals(it.elementName()))
                .toList();

        // TODO: cannot query method parameters, return type, and modifiers from enclosing type for now
        if (matchingMethodsByName.isEmpty()) {
            throw new ToolsException("Could not find matching fallback method for name " + fallback.fallbackName + " in"
                                             + enclosingType.typeName());
        }
        /*
        boolean found = false;
        // matches by name, but not by return type or parameters
        List<BadCandidate> badCandidates = new ArrayList<>();
        for (TypedElementName matchingMethodByName : matchingMethodsByName) {
            // now we need to find a method that matches parameters, or has one more parameter of Throwable
            ExecutableElement method = (ExecutableElement) matchingMethodByName;
            if (!method.getReturnType().toString().equals(fallback.getReturnType())) {
                badCandidates.add(new BadCandidate(method, "Same name, different return types"));
                continue;
            }

            List<? extends VariableElement> parameters = method.getParameters();
            if (fallback.paramTypes.size() != parameters.size() && fallback.paramTypes.size() != parameters.size() - 1) {
                badCandidates.add(new BadCandidate(method, "Same name, wrong number of parameters"));
                continue;
            }
            boolean goodCandidate = true;
            for (int i = 0; i < fallback.paramTypes.size(); i++) {
                if (!fallback.paramTypes.get(i).equals(parameters.get(i).asType().toString())) {
                    badCandidates.add(new BadCandidate(method, "Same name, different parameter types at index " + i));
                    goodCandidate = false;
                    break;
                }
            }
            if (!goodCandidate) {
                continue;
            }
            if (fallback.paramTypes.size() == parameters.size()) {
                // this is a good candidate, let's use it (we may still find a better candidate with Throwable)
                fallback.fallbackStatic = method.getModifiers().contains(Modifier.STATIC);
                fallback.fallbackAcceptsThrowable = false;
                found = true;
            } else {
                // check last parameter
                if (parameters.get(parameters.size() - 1).asType().toString().equals("java.lang.Throwable")) {
                    // best candidate
                    fallback.fallbackStatic = method.getModifiers().contains(Modifier.STATIC);
                    fallback.fallbackAcceptsThrowable = true;
                    found = true;
                    break;
                }
                badCandidates.add(new BadCandidate(method, "Same name, last parameter is not java.lang.Throwable"));
            }
        }

        if (!found) {
            throw new ToolsException("Could not find matching fallback method for name " + fallback.fallbackName + ","
                                             + " following bad candidates found: " + badCandidates);
        }
        */

        response.put("fallback", fallback);
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

    private record BadCandidate(ExecutableElement element, String reason) {
    }

    /**
     * Needed for template processing.
     * Do not use.
     */
    @Deprecated(since = "1.0.0")
    public static class FallbackDef {
        // name of the method that is annotated
        private String methodName;
        // return type of the annotated method
        private String returnType;
        private boolean returnVoid;
        // type of the bean that hosts the annotated method
        private String beanType;

        private List<String> applyOn;
        private List<String> skipOn;
        private List<String> paramTypes;

        private String fallbackName;
        private boolean fallbackStatic;
        private boolean fallbackAcceptsThrowable;

        public String getMethodName() {
            return methodName;
        }

        public String getReturnType() {
            return returnType;
        }

        public boolean isReturnVoid() {
            return returnVoid;
        }

        public String getBeanType() {
            return beanType;
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

        public boolean isFallbackStatic() {
            return fallbackStatic;
        }

        public String isFallbackName() {
            return fallbackName;
        }

        public boolean isFallbackAcceptsThrowable() {
            return fallbackAcceptsThrowable;
        }
    }
}
