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

package io.helidon.inject.tools;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.runtime.ServiceBinderDefault;

/**
 * Abstract base for any codegen creator.
 */
public abstract class AbstractCreator {
    /**
     * The default java source version (this can be explicitly overridden using the builder or maven plugin).
     */
    public static final String DEFAULT_SOURCE = "21";
    /**
     * The default java target version (this can be explicitly overridden using the builder or maven plugin).
     */
    public static final String DEFAULT_TARGET = "21";

    // no special chars since this will be used as a package and class name
    static final String INJECT_FRAMEWORK_MODULE = "io.helidon.inject.runtime";
    static final String MODULE_NAME_SUFFIX = "Module";

    private final System.Logger logger = System.getLogger(getClass().getName());

    AbstractCreator() {
    }

    /**
     * Generates the {@link Activator} source code for the provided service providers. Custom
     * service providers.
     *
     * @param sp the collection of service providers
     * @return the code generated string for the service provider given
     */
    static String toActivatorCodeGen(ServiceProvider<?> sp) {
        return ServiceBinderDefault.toRootProvider(sp).descriptor().getClass().getName() + ".INSTANCE";
    }

    /**
     * Generates the {@link Activator} source code for the provided service providers.
     *
     * @param coll the collection of service providers
     * @return the code generated string for the collection of service providers given
     */
    static String toActivatorCodeGen(Collection<ServiceProvider<?>> coll) {
        return CommonUtils.toString(coll, AbstractCreator::toActivatorCodeGen, null);
    }

    static Set<TypeName> toAllContracts(Map<TypeName, Set<TypeName>> servicesToContracts) {
        Set<TypeName> result = new LinkedHashSet<>();
        servicesToContracts.forEach((serviceTypeName, cn) -> result.addAll(cn));
        return result;
    }

    /**
     * Creates the {@link CodeGenPaths} given the current batch of services to process.
     *
     * @param servicesToProcess the services to process
     * @return the payload for code gen paths
     */
    static CodeGenPaths createCodeGenPaths(ServicesToProcess servicesToProcess) {
        Path moduleInfoFilePath = servicesToProcess.lastGeneratedModuleInfoFilePath();
        if (moduleInfoFilePath == null) {
            moduleInfoFilePath = servicesToProcess.lastKnownModuleInfoFilePath();
        }
        return CodeGenPaths.builder()
                .moduleInfoPath(Optional.ofNullable((moduleInfoFilePath != null) ? moduleInfoFilePath.toString() : null))
                .build();
    }

    System.Logger logger() {
        return logger;
    }

    /**
     * Creates a codegen filer that is not reliant on annotation processing, but still capable of creating source
     * files and resources.
     *
     * @param paths          the paths for where files should be read or written.
     * @param isAnalysisOnly true if analysis only, where no code or resources will be physically written to disk
     * @return the code gen filer instance to use
     */
    CodeGenFiler createDirectCodeGenFiler(CodeGenPaths paths,
                                          boolean isAnalysisOnly) {
        AbstractFilerMessager filer = AbstractFilerMessager.createDirectFiler(paths, logger);
        return new CodeGenFiler(filer, !isAnalysisOnly);
    }
}
