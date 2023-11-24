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

package io.helidon.builder.processor;

import java.util.Optional;
import java.util.function.Predicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import io.helidon.codegen.apt.AptContext;
import io.helidon.codegen.apt.AptTypeInfoFactory;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;

interface BlueprintProcessingContext {
    static BlueprintProcessingContext create(AptContext ctx) {
        return new ProcessingContextProcessor(ctx);
    }

    Optional<TypeInfo> toTypeInfo(TypeName returnType);

    Optional<String> javadoc(Element typeName);

    class ProcessingContextProcessor implements BlueprintProcessingContext {
        private final AptContext ctx;

        ProcessingContextProcessor(AptContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public Optional<String> javadoc(Element element) {
            Elements elementUtils = ctx.aptEnv().getElementUtils();

            String javadoc = elementUtils.getDocComment(element);
            return Optional.ofNullable(javadoc).filter(Predicate.not(String::isBlank));
        }

        @Override
        public Optional<TypeInfo> toTypeInfo(TypeName returnType) {
            TypeElement typeElement = ctx.aptEnv().getElementUtils().getTypeElement(returnType.boxed().fqName());
            if (typeElement == null) {
                return Optional.empty();
            }
            return AptTypeInfoFactory.create(ctx, typeElement);
        }
    }
}
