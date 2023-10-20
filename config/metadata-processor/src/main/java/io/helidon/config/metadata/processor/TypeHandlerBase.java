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

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.processor.TypeInfoFactory;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.config.metadata.processor.UsedTypes.COMMON_CONFIG;
import static io.helidon.config.metadata.processor.UsedTypes.CONFIG;

abstract class TypeHandlerBase {
    static final String UNCONFIGURED_OPTION = "io.helidon.config.metadata.ConfiguredOption.UNCONFIGURED";
    private static final Pattern JAVADOC_CODE = Pattern.compile("\\{@code (.*?)}");
    private static final Pattern JAVADOC_LINK = Pattern.compile("\\{@link (.*?)}");
    private final ProcessingContext ctx;

    TypeHandlerBase(ProcessingContext ctx) {
        this.ctx = ctx;
    }

    static Predicate<TypedElementInfo> isMine(TypeName type) {
        TypeName withoutGenerics = type.genericTypeName();
        return info -> info.enclosingType().map(TypeName::genericTypeName).map(withoutGenerics::equals).orElse(true);
    }

    // exactly one parameter - either common config, or Helidon config
    static boolean hasConfigParam(TypedElementInfo info) {
        List<TypedElementInfo> arguments = info.parameterArguments();
        if (arguments.size() != 1) {
            return false;
        }
        TypeName argumentType = arguments.getFirst().typeName();
        return CONFIG.equals(argumentType) || COMMON_CONFIG.equals(argumentType);
    }

    static Element elementFor(Elements aptElements, TypedElementInfo elementInfo) {
        return elementInfo.enclosingType()
                .map(TypeName::fqName)
                .map(aptElements::getTypeElement)
                .orElse(null);
    }

    /*
       Method name is camel case (such as maxInitialLineLength)
       result is dash separated and lower cased (such as max-initial-line-length).
       Note that this same method was created in ConfigUtils in common-config, but since this
       module should not have any dependencies in it a copy was left here as well.
        */
    static String toConfigKey(String methodName) {
        StringBuilder result = new StringBuilder();

        char[] chars = methodName.toCharArray();
        for (char aChar : chars) {
            if (Character.isUpperCase(aChar)) {
                if (result.isEmpty()) {
                    result.append(Character.toLowerCase(aChar));
                } else {
                    result.append('-')
                            .append(Character.toLowerCase(aChar));
                }
            } else {
                result.append(aChar);
            }
        }

        return result.toString();
    }

    static String javadoc(String docComment) {
        if (docComment == null) {
            return "";
        }

        String javadoc = docComment;
        int index = javadoc.indexOf("@param");
        if (index > -1) {
            javadoc = docComment.substring(0, index);
        }
        // replace all {@code xxx} with 'xxx'
        javadoc = JAVADOC_CODE.matcher(javadoc).replaceAll(it -> '`' + it.group(1) + '`');
        // replace all {@link ...} with just the name
        javadoc = JAVADOC_LINK.matcher(javadoc).replaceAll(it -> it.group(1));

        return javadoc.trim();
    }

    String key(TypedElementInfo elementInfo, ConfiguredOptionData configuredOption) {
        String name = configuredOption.name();
        if (name == null || name.isBlank()) {
            return toConfigKey(elementInfo.elementName());
        }
        return name;
    }

    String description(TypedElementInfo elementInfo, ConfiguredOptionData configuredOption) {
        String desc = configuredOption.description();
        if (desc == null) {
            return javadoc(elementInfo.description().orElse(null));
        }
        return desc;
    }

    String defaultValue(String defaultValue) {
        return UNCONFIGURED_OPTION.equals(defaultValue) ? null : defaultValue;
    }

    List<ConfiguredOptionData.AllowedValue> allowedValues(ConfiguredOptionData configuredOption, TypeName type) {
        if (type.equals(configuredOption.type()) || !configuredOption.allowedValues().isEmpty()) {
            // this was already processed due to an explicit type defined in the annotation
            // or allowed values explicitly configured in annotation
            return configuredOption.allowedValues();
        }
        return allowedValues(type);
    }

    Optional<TypeInfo> typeInfo(TypeName typeName, Predicate<TypedElementInfo> predicate) {
        return TypeInfoFactory.create(ctx, typeName);
    }

    ProcessingEnvironment aptEnv() {
        return ctx.aptEnv();
    }

    Messager aptMessager() {
        return ctx.aptEnv().getMessager();
    }

    Elements aptElements() {
        return ctx.aptEnv().getElementUtils();
    }

    List<TypeName> params(TypedElementInfo info) {
        return info.parameterArguments()
                .stream()
                .map(TypedElementInfo::typeName)
                .toList();
    }

    void addInterfaces(ConfiguredType type, TypeInfo typeInfo, TypeName requiredAnnotation) {
        for (TypeInfo interfaceInfo : typeInfo.interfaceTypeInfo()) {
            if (interfaceInfo.hasAnnotation(requiredAnnotation)) {
                type.addInherited(interfaceInfo.typeName());
            } else {
                addSuperClasses(type, interfaceInfo, requiredAnnotation);
            }
        }
    }

    void addSuperClasses(ConfiguredType type, TypeInfo typeInfo, TypeName requiredAnnotation) {
        Optional<TypeInfo> foundSuperType = typeInfo.superTypeInfo();
        if (foundSuperType.isEmpty()) {
            return;
        }
        TypeInfo superClass = foundSuperType.get();

        while (true) {
            if (superClass.hasAnnotation(requiredAnnotation)) {
                // we only care about the first one. This one should reference its superclass/interfaces
                // if they are configured as well
                type.addInherited(superClass.typeName());
                return;
            }

            foundSuperType = superClass.superTypeInfo();
            if (foundSuperType.isEmpty()) {
                return;
            }
            superClass = foundSuperType.get();
        }
    }

    private List<ConfiguredOptionData.AllowedValue> allowedValues(TypeName type) {
        TypeElement typeElement = aptElements().getTypeElement(type.fqName());
        if (typeElement != null && typeElement.getKind() == ElementKind.ENUM) {
            return typeElement.getEnclosedElements()
                    .stream()
                    .filter(element -> element.getKind().equals(ElementKind.ENUM_CONSTANT))
                    .map(element -> new ConfiguredOptionData.AllowedValue(element.toString(),
                                                                          javadoc(aptElements().getDocComment(element))))
                    .toList();
        }
        return List.of();
    }
}
