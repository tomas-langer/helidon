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

/**
 * Injection Annotation Processor module.
 */
module io.helidon.inject.processor {

    exports io.helidon.inject.processor.spi;
    exports io.helidon.inject.processor;

    requires io.helidon.common.processor.classmodel;
    requires io.helidon.builder.api;
    requires io.helidon.common.processor;
    requires io.helidon.common;
    requires io.helidon.common.codegen;

    requires transitive io.helidon.inject.tools;
    requires transitive java.compiler;

    uses io.helidon.inject.processor.spi.InjectionAnnotationProcessorObserver;
    uses io.helidon.inject.processor.spi.HelidonProcessorExtensionProvider;
    uses io.helidon.common.processor.spi.AnnotationMapperProvider;
    uses io.helidon.common.processor.spi.ElementMapperProvider;
    uses io.helidon.common.processor.spi.TypeMapperProvider;

    provides io.helidon.common.processor.spi.AnnotationMapperProvider
            with io.helidon.inject.processor.JavaxAnnotationMapperProvider,
                 io.helidon.inject.processor.ApplicationScopedMapperProvider;

    provides io.helidon.inject.processor.spi.HelidonProcessorExtensionProvider
            with io.helidon.inject.processor.InjectionProcessorExtensionProvider;

    provides javax.annotation.processing.Processor with
            io.helidon.inject.processor.HelidonAnnotationProcessor,
            io.helidon.inject.processor.UnsupportedConstructsProcessor;

}
