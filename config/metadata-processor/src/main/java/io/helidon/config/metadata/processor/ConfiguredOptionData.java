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

package io.helidon.config.metadata.processor;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.config.metadata.processor.TypeHandlerBase.UNCONFIGURED_OPTION;
import static io.helidon.config.metadata.processor.TypeHandlerBase.javadoc;
import static io.helidon.config.metadata.processor.UsedTypes.DEPRECATED;
import static io.helidon.config.metadata.processor.UsedTypes.DESCRIPTION;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_ALLOWED_VALUE;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_ALLOWED_VALUES;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_CONFIGURED;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_DEFAULT;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_DEFAULT_BOOLEAN;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_DEFAULT_CODE;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_DEFAULT_DOUBLE;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_DEFAULT_INT;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_DEFAULT_LONG;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_DEFAULT_METHOD;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_PROVIDER;
import static io.helidon.config.metadata.processor.UsedTypes.OPTION_REQUIRED;
import static java.util.function.Predicate.not;

final class ConfiguredOptionData {
    private final List<AllowedValue> allowedValues = new LinkedList<>();

    private boolean configured = true;
    private String name;
    private TypeName type;
    private String description;
    private boolean required;
    private String defaultValue;
    private boolean experimental;
    private boolean provider;
    private boolean deprecated;
    private boolean merge;
    private String kind = "VALUE";

    // create from @ConfiguredOption in config-metadata
    static ConfiguredOptionData createMeta(ProcessingEnvironment aptEnv, Annotation option) {
        ConfiguredOptionData result = new ConfiguredOptionData();

        option.booleanValue("configured").ifPresent(result::configured);
        option.stringValue("key").filter(not(String::isBlank)).ifPresent(result::name);
        option.stringValue("description").filter(not(String::isBlank)).ifPresent(result::description);
        option.stringValue().filter(not(UNCONFIGURED_OPTION::equals)).ifPresent(result::defaultValue);
        option.booleanValue("experimental").ifPresent(result::experimental);
        option.booleanValue("required").ifPresent(result::required);
        option.booleanValue("mergeWithParent").ifPresent(result::merge);
        option.typeValue("type").ifPresent(result::type);
        option.stringValue("kind").ifPresent(result::kind);
        option.booleanValue("provider").ifPresent(result::provider);
        option.booleanValue("deprecated").ifPresent(result::deprecated);
        option.annotationValues("allowedValues")
                .or(() -> option.annotationValue("allowedValue").map(List::of))
                .stream()
                .flatMap(List::stream)
                .map(AllowedValue::create)
                .forEach(result::addAllowedValue);

        if (result.allowedValues.isEmpty()) {
            // if enum, fill this in
            Elements aptElements = aptEnv.getElementUtils();
            TypeElement typeElement = aptElements
                    .getTypeElement(option.typeValue("type").orElse(TypeNames.STRING).fqName());
            if (typeElement != null && typeElement.getKind() == ElementKind.ENUM) {
                result.allowedValues.addAll(allowedValues(aptElements, typeElement));
            }
        }

        return result;
    }

    // create from Option annotations in builder-api
    static ConfiguredOptionData createBuilder(TypedElementInfo element) {
        ConfiguredOptionData result = new ConfiguredOptionData();

        Optional<Annotation> optionConfigured = element.findAnnotation(OPTION_CONFIGURED);
        optionConfigured.flatMap(Annotation::stringValue).filter(not(String::isBlank))
                .ifPresent(result::name);
        optionConfigured.flatMap(it -> it.booleanValue("merge"))
                .ifPresent(result::merge);

        element.findAnnotation(DESCRIPTION).flatMap(Annotation::stringValue).ifPresent(result::description);
        element.findAnnotation(OPTION_REQUIRED).ifPresent(it -> result.required(true));
        element.findAnnotation(OPTION_PROVIDER).ifPresent(it -> result.provider(true));
        element.findAnnotation(DEPRECATED).ifPresent(it -> result.deprecated(true));
        element.findAnnotation(OPTION_ALLOWED_VALUES)
                .flatMap(Annotation::annotationValues)
                .or(() -> element.findAnnotation(OPTION_ALLOWED_VALUE).map(List::of))
                .stream()
                .flatMap(List::stream)
                .map(AllowedValue::create)
                .forEach(result::addAllowedValue);

        Optional<Annotation> defaultValues = element.findAnnotation(OPTION_DEFAULT)
                .or(() -> element.findAnnotation(OPTION_DEFAULT_INT))
                .or(() -> element.findAnnotation(OPTION_DEFAULT_BOOLEAN))
                .or(() -> element.findAnnotation(OPTION_DEFAULT_LONG))
                .or(() -> element.findAnnotation(OPTION_DEFAULT_DOUBLE));
        if (defaultValues.isPresent()) {
            List<String> strings = defaultValues.get().stringValues().orElseGet(List::of);
            result.defaultValue(String.join(", ", strings));
        } else if (element.hasAnnotation(OPTION_DEFAULT_METHOD)) {
            Annotation annotation = element.annotation(OPTION_DEFAULT_METHOD);
            TypeName type = annotation.typeValue("type")
                    .filter(Predicate.not(OPTION_DEFAULT_METHOD::equals))
                    .or(element::enclosingType)
                    .orElse(null);
            String value = annotation.stringValue().orElse(null);
            if (value != null) {
                // this should always be true, as it is mandatory
                if (type == null) {
                    result.defaultValue(value + "()");
                } else {
                    result.defaultValue(type.fqName() + "." + value + "()");
                }
            }
        } else if (element.hasAnnotation(OPTION_DEFAULT_CODE)) {
            element.annotation(OPTION_DEFAULT_CODE).stringValue().ifPresent(result::defaultValue);
        }

        return result;
    }

    List<AllowedValue> allowedValues() {
        return allowedValues;
    }

    String name() {
        return name;
    }

    TypeName type() {
        return type;
    }

    String description() {
        return description;
    }

    boolean optional() {
        return !required;
    }

    String defaultValue() {
        return defaultValue;
    }

    boolean experimental() {
        return experimental;
    }

    boolean provider() {
        return provider;
    }

    boolean deprecated() {
        return deprecated;
    }

    boolean merge() {
        return merge;
    }

    String kind() {
        return kind;
    }

    boolean configured() {
        return configured;
    }

    void type(TypeName type) {
        this.type = type;
    }

    void name(String name) {
        this.name = name;
    }

    void description(String description) {
        this.description = description;
    }

    void required(boolean required) {
        this.required = required;
    }

    void defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    void experimental(boolean experimental) {
        this.experimental = experimental;
    }

    void provider(boolean provider) {
        this.provider = provider;
    }

    void deprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    void merge(boolean merge) {
        this.merge = merge;
    }

    void kind(String kind) {
        this.kind = kind;
    }

    void addAllowedValue(AllowedValue value) {
        this.allowedValues.add(value);
    }

    void configured(boolean configured) {
        this.configured = configured;
    }

    private static List<AllowedValue> allowedValues(Elements aptElements, TypeElement typeElement) {
        if (typeElement != null && typeElement.getKind() == ElementKind.ENUM) {
            return typeElement.getEnclosedElements()
                    .stream()
                    .filter(element -> element.getKind().equals(ElementKind.ENUM_CONSTANT))
                    .map(element -> new AllowedValue(element.toString(), javadoc(aptElements.getDocComment(element))))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    static final class AllowedValue {
        private String value;
        private String description;

        private AllowedValue() {
        }

        AllowedValue(String value, String description) {
            this.value = value;
            this.description = description;
        }

        String value() {
            return value;
        }

        String description() {
            return description;
        }

        private static AllowedValue create(Annotation annotation) {
            AllowedValue result = new AllowedValue();

            annotation.stringValue().ifPresent(it -> result.value = it);
            annotation.stringValue("description").filter(not(String::isBlank)).ifPresent(it -> result.description = it);

            return result;
        }
    }
}
