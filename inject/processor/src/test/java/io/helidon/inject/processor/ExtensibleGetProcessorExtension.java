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

package io.helidon.inject.processor;

import java.util.Collection;
import java.util.Set;

import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectTypes;
import io.helidon.inject.processor.spi.HelidonProcessorExtension;
import io.helidon.inject.processor.spi.HelidonProcessorExtensionProvider;
import io.helidon.inject.tools.TypeNames;

/**
 * For Testing (service loaded).
 */
public class ExtensibleGetProcessorExtension implements HelidonProcessorExtension, HelidonProcessorExtensionProvider {

    public static final TypeName EXTENSBILE_GET = TypeName.create("io.helidon.inject.processor.testsubjects.ExtensibleGET");
    private InjectionProcessingContext ctx;

    /**
     * Service loader based constructor.
     *
     * @deprecated this is a Java ServiceLoader implementation and the constructor should not be used directly
     */
    @Deprecated
    public ExtensibleGetProcessorExtension() {
    }

    @Override // ExtensionProvider
    public HelidonProcessorExtension create(InjectionProcessingContext ctx) {
        this.ctx = ctx;
        return this;
    }

    @Override // ExtensionProvider
    public Collection<TypeName> supportedTypes() {
        return Set.of(EXTENSBILE_GET);
    }

    @Override // Extension
    public boolean process(RoundContext roundContext) {
        Collection<TypeInfo> types = roundContext.annotatedTypes(EXTENSBILE_GET);

        for (TypeInfo type : types) {
            generate(type);
        }

        return true;
    }

    private void generate(TypeInfo typeInfo) {
        TypeName processedType = typeInfo.typeName();
        TypeName generatedType = generatedType(processedType);

        TypeName providerType = provider(processedType);
        ClassModel.Builder classModel = ClassModel.builder()
                .type(generatedType)
                .addAnnotation(Annotation.create(TypeNames.JAKARTA_SINGLETON_TYPE))
                .addAnnotation(Annotation.create(TypeNames.NAMED, EXTENSBILE_GET.className()))
                .addField(target -> target.isFinal(true)
                        .name("target")
                        .type(providerType))
                .addConstructor(ctor -> ctor.addAnnotation(Annotation.create(TypeNames.JAKARTA_INJECT_TYPE))
                        .addParameter(target -> target.type(providerType)
                                .name("target"))
                        .addLine("this.target = target;"))
                .addMethod(get -> get.name("get" + processedType.className())
                        .returnType(providerType)
                        .addLine("return target;"));

        ctx.addClass(processedType, generatedType, classModel);
    }

    private TypeName provider(TypeName processedType) {
        return TypeName.builder(InjectTypes.JAKARTA_PROVIDER)
                .addTypeArgument(processedType)
                .build();
    }

    private TypeName generatedType(TypeName processedType) {
        return TypeName.builder(processedType)
                .className(processedType.classNameWithEnclosingNames().replace('.', '_') + "__ExtensibleGet")
                .build();
    }

}
