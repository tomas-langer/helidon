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

package io.helidon.inject.configdriven.configuredby.test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.common.config.Config;
import io.helidon.common.types.TypeName;
import io.helidon.inject.api.Metrics;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.configdriven.configuredby.application.test.ASimpleRunLevelService;
import io.helidon.inject.service.Inject;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Designed to re-run the same tests from base, but using the application-created DI model instead.
 */
class ApplicationConfiguredByTest extends AbstractConfiguredByTest {

    /**
     * In application mode, we should not have many lookups recorded.
     */
    @Test
    void verifyMinimalLookups() {
        resetWith(io.helidon.config.Config.builder(createBasicTestingConfigSource(), createRootDefault8080TestingConfigSource())
                          .disableEnvironmentVariablesSource()
                          .disableSystemPropertiesSource()
                          .build());

        Metrics metrics = injectionServices.metrics().orElseThrow();
        Set<ServiceInfoCriteria> criteriaSearchLog = injectionServices.lookups().orElseThrow();
        Set<TypeName> contractSearchLog = criteriaSearchLog.stream()
                .flatMap(it -> it.contracts().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<TypeName> servicesSearchLog = criteriaSearchLog.stream()
                .flatMap(it -> it.serviceTypeName().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<TypeName> searchLog = new LinkedHashSet<>(contractSearchLog);
        searchLog.addAll(servicesSearchLog);

        // everything is handled by Application class
        // except for config beans, and these are handled by  ConfigDrivenInstanceProvider itself
        assertThat("Full log: " + searchLog,
                   searchLog,
                   contains(TypeName.create(Config.class)));

        // there is always a lookup for Config from config bean registry
        // nothing else should be done
        assertThat("lookup log: " + criteriaSearchLog,
                   metrics.lookupCount().orElseThrow(),
                   is(1));
    }

    @Test
    public void startupAndShutdownRunLevelServices() {
        resetWith(io.helidon.config.Config.builder(createBasicTestingConfigSource(), createRootDefault8080TestingConfigSource())
                          .disableEnvironmentVariablesSource()
                          .disableSystemPropertiesSource()
                          .build());

        Metrics metrics = injectionServices.metrics().orElseThrow();
        int startingLookupCount = metrics.lookupCount().orElseThrow();

        MatcherAssert.assertThat(ASimpleRunLevelService.getPostConstructCount(),
                                 is(0));
        assertThat(ASimpleRunLevelService.getPreDestroyCount(),
                   is(0));

        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder()
                .runLevel(Inject.RunLevel.STARTUP)
                .build();
        List<ServiceProvider<?>> startups = services.lookupAll(criteria);
        List<String> desc = startups.stream().map(ServiceProvider::description).collect(Collectors.toList());

        assertThat(desc,
                   contains(ASimpleRunLevelService.class.getSimpleName() + ":INIT"));
        startups.forEach(ServiceProvider::get);

        metrics = injectionServices.metrics().orElseThrow();
        int endingLookupCount = metrics.lookupCount().orElseThrow();
        assertThat(endingLookupCount - startingLookupCount,
                   is(1));

        assertThat(ASimpleRunLevelService.getPostConstructCount(),
                   is(1));
        assertThat(ASimpleRunLevelService.getPreDestroyCount(),
                   is(0));

        injectionServices.shutdown();
        assertThat(ASimpleRunLevelService.getPostConstructCount(),
                   is(1));
        assertThat(ASimpleRunLevelService.getPreDestroyCount(),
                   is(1));
    }

}
