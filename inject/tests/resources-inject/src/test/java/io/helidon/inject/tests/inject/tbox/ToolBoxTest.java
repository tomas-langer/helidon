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

package io.helidon.inject.tests.inject.tbox;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.helidon.common.types.TypeName;
import io.helidon.config.Config;
import io.helidon.inject.api.ActivationResult;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.Services;
import io.helidon.inject.service.Inject;
import io.helidon.inject.service.ModuleComponent;
import io.helidon.inject.service.Qualifier;
import io.helidon.inject.testing.InjectionTestingSupport;
import io.helidon.inject.tests.inject.ASerialProviderImpl;
import io.helidon.inject.tests.inject.ClassNamedY;
import io.helidon.inject.tests.inject.TestingSingleton;
import io.helidon.inject.tests.inject.provider.FakeConfig;
import io.helidon.inject.tests.inject.provider.FakeServer;
import io.helidon.inject.tests.inject.stacking.CommonContract;
import io.helidon.inject.tests.inject.tbox.impl.BigHammer;
import io.helidon.inject.tests.inject.tbox.impl.HandSaw;
import io.helidon.inject.tests.inject.tbox.impl.MainToolBox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.helidon.common.types.TypeName.create;
import static io.helidon.inject.testing.InjectionTestingSupport.resetAll;
import static io.helidon.inject.testing.InjectionTestingSupport.testableServices;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Expectation here is that the annotation processor ran, and we can use standard injection and di registry services, etc.
 */
class ToolBoxTest {
    Config config = InjectionTestingSupport.basicTestableConfig();
    InjectionServices injectionServices;
    Services services;

    @BeforeEach
    void setUp() {
        setUp(config);
    }

    void setUp(Config config) {
        this.injectionServices = testableServices(config);
        this.services = injectionServices.services();
    }

    @AfterEach
    void tearDown() {
        resetAll();
    }

    @Test
    void sanity() {
        assertNotNull(injectionServices);
        assertNotNull(services);
    }

    @Test
    void toolbox() {
        List<ServiceProvider<Awl>> blanks = services.lookupAll(Awl.class);
        List<String> desc = blanks.stream().map(ServiceProvider::description).collect(Collectors.toList());
        // note that order matters here
        assertThat(desc,
                   contains("AwlImpl:INIT"));

        List<ServiceProvider<ToolBox>> allToolBoxes = services.lookupAll(ToolBox.class);
        desc = allToolBoxes.stream().map(ServiceProvider::description).collect(Collectors.toList());
        assertThat(desc,
                   contains("MainToolBox:INIT"));

        ToolBox toolBox = allToolBoxes.get(0).get();
        assertThat(toolBox.getClass(), equalTo(MainToolBox.class));
        MainToolBox mtb = (MainToolBox) toolBox;
        assertThat(mtb.postConstructCallCount, equalTo(1));
        assertThat(mtb.preDestroyCallCount, equalTo(0));
        assertThat(mtb.setterCallCount, equalTo(1));
        List<Supplier<Tool>> allTools = mtb.toolsInBox();
        desc = allTools.stream().map(Object::toString).collect(Collectors.toList());
        assertThat(desc,
                   contains("SledgeHammer:INIT",
                            "TableSaw:INIT",
                            "AwlImpl:INIT",
                            "HandSaw:INIT",
                            "Screwdriver:ACTIVE",
                            "BigHammer:INIT",
                            "LittleHammer:INIT"));
        assertThat(mtb.screwdriver(), notNullValue());

        Supplier<Hammer> hammer = Objects.requireNonNull(toolBox.preferredHammer());
        assertThat(hammer.get(), notNullValue());
        assertThat(hammer.get(), is(hammer.get()));
        assertThat(BigHammer.class, equalTo(hammer.get().getClass()));
        desc = allTools.stream().map(Object::toString).collect(Collectors.toList());
        assertThat(desc,
                   contains("SledgeHammer:INIT",
                            "TableSaw:INIT",
                            "AwlImpl:INIT",
                            "HandSaw:INIT",
                            "Screwdriver:ACTIVE",
                            "BigHammer:ACTIVE",
                            "LittleHammer:INIT"));

        desc = (((MainToolBox) toolBox).allHammers()).stream().map(Object::toString).collect(Collectors.toList());
        assertThat(desc,
                   contains("SledgeHammer:INIT",
                            "BigHammer:ACTIVE",
                            "LittleHammer:INIT"));
        assertThat(((ServiceProvider<?>) ((MainToolBox) toolBox).bigHammer()).description(),
                equalTo("BigHammer:ACTIVE"));
    }

    @Test
    void testClasses() {
        assertThat(services.lookupFirst(TestingSingleton.class),
                   notNullValue());
    }

    /**
     * This assumes {@link io.helidon.inject.codegen.InjectOptions#AUTO_ADD_NON_CONTRACT_INTERFACES} has
     * been enabled - see pom.xml
     */
    @Test
    void autoExternalContracts() {
        List<ServiceProvider<Serializable>> allSerializable = services.lookupAll(Serializable.class);
        List<String> desc = allSerializable.stream().map(ServiceProvider::description).collect(Collectors.toList());
        // note that order matters here
        assertThat(desc,
                contains("ASerialProviderImpl:INIT", "Screwdriver:INIT"));
    }

    @Test
    void providerTest() {
        Serializable s1 = services.lookupFirst(Serializable.class).get();
        assertThat(s1, notNullValue());
        assertThat(ASerialProviderImpl.class + " is a higher weight and should have been returned for " + String.class,
                   String.class, equalTo(s1.getClass()));
        assertThat(services.lookupFirst(Serializable.class).get(), not(s1));
    }

    @Test
    void modules() {
        List<ServiceProvider<ModuleComponent>> allModules = services.lookupAll(ModuleComponent.class);
        List<String> desc = allModules.stream()
                .map(it -> it.id() + ":" + it.currentActivationPhase())
                .toList();
        // note that order matters here
        // there is now config module as active as well
        assertThat("ensure that Annotation Processors are enabled in the tools module meta-inf/services",
                   desc, contains("io.helidon.config.HelidonInjection__ModuleComponent:ACTIVE",
                                  "io.helidon.inject.configdriven.runtime.ConfigDrivenInjectModule:ACTIVE",
                                  "io.helidon.inject.tests.inject.HelidonInjection__ModuleComponent:ACTIVE",
                                  "io.helidon.inject.tests.inject.TestHelidonInjection__ModuleComponent:ACTIVE"));
        List<String> names = allModules.stream()
                .sorted()
                .map(ServiceProvider::get)
                .map(ModuleComponent::name)
                .toList();
        assertThat(names,
                   contains("io.helidon.config",
                            "io.helidon.inject.configdriven.runtime",
                            "unknown/io.helidon.inject.tests.inject",
                            "unknown/io.helidon.inject.tests.inject/test"));
    }

    @Test
    void innerClassesCanBeGenerated() {
        FakeServer.Builder s1 = services.lookupFirst(FakeServer.Builder.class).get();
        assertThat(s1, notNullValue());
        assertThat(services.lookupFirst(FakeServer.Builder.class).get(), is(s1));

        FakeConfig.Builder c1 = services.lookupFirst(FakeConfig.Builder.class).get();
        assertThat(c1, notNullValue());
        assertThat(services.lookupFirst(FakeConfig.Builder.class).get(), is(c1));
    }

    /**
     * Targets {@link AbstractSaw} with derived classes of
     * {@link HandSaw} and {@link TableSaw} found in different packages.
     */
    @Test
    void hierarchyOfInjections() {
        List<ServiceProvider<AbstractSaw>> saws = services.lookupAll(AbstractSaw.class);
        List<String> desc = saws.stream().map(ServiceProvider::description).collect(Collectors.toList());
        // note that order matters here
        assertThat(desc,
                   contains("TableSaw:INIT", "HandSaw:INIT"));
        for (ServiceProvider<AbstractSaw> saw : saws) {
            saw.get().verifyState();
        }
    }

    /**
     * This tests the presence of module(s) + application(s) to handle all bindings, with effectively no lookups.
     */
    @Test
    void runlevel() {
        assertThat("we start with 1 because of config driven",
                   injectionServices.metrics().orElseThrow().lookupCount().orElseThrow(),
                   equalTo(1));
        List<ServiceProvider<?>> runLevelServices = services
                .lookupAll(ServiceInfoCriteria.builder().runLevel(Inject.RunLevel.STARTUP).build(), true);
        List<String> desc = runLevelServices.stream().map(ServiceProvider::description).collect(Collectors.toList());
        assertThat(desc,
                   contains("TestingSingleton:INIT"));

        runLevelServices.forEach(sp -> Objects.requireNonNull(sp.get(), sp + " failed on get()"));
        assertThat("activation should not triggered one new lookup from startup",
                   injectionServices.metrics().orElseThrow().lookupCount().orElseThrow(),
                   equalTo(2));
        desc = runLevelServices.stream().map(ServiceProvider::description).collect(Collectors.toList());
        assertThat(desc,
                   contains("TestingSingleton:ACTIVE"));
    }

    /**
     * This assumes the presence of module(s) + application(s) to handle all bindings, with effectively no lookups!
     */
    @Test
    void noServiceActivationRequiresLookupWhenApplicationIsPresent() {
        // there are two lookups - ConfigDrivenRegistry looks for Config, and we look for all services
        List<ServiceProvider<?>> allServices = services
                .lookupAll(ServiceInfoCriteria.builder().build(), true);
        allServices.stream()
                .filter(it -> !it.isProvider())
                .forEach(sp -> {
                    sp.get();
                    assertThat("activation should not have triggered any lookups (for singletons): "
                                       + sp + " triggered lookups", injectionServices.metrics()
                                       .flatMap(metrics -> metrics.lookupCount()).orElseThrow(),
                               equalTo(2));
                });
    }

    @Test
    void startupAndShutdownCallsPostConstructAndPreDestroy() {
        assertThat(TestingSingleton.postConstructCount(), equalTo(0));
        assertThat(TestingSingleton.preDestroyCount(), equalTo(0));

        List<ServiceProvider<CommonContract>> allInterceptedBefore = services.lookupAll(CommonContract.class);
        assertThat(allInterceptedBefore.size(), greaterThan(0));
        assertThat(TestingSingleton.postConstructCount(), equalTo(0));
        assertThat(TestingSingleton.preDestroyCount(), equalTo(0));
        allInterceptedBefore.forEach(ServiceProvider::get);

        TestingSingleton testingSingletonFromLookup = services.lookup(TestingSingleton.class).get();
        assertThat(testingSingletonFromLookup, notNullValue());
        assertThat(TestingSingleton.postConstructCount(), equalTo(1));
        assertThat(TestingSingleton.preDestroyCount(), equalTo(0));

        Map<TypeName, ActivationResult> map = injectionServices.shutdown().orElseThrow();
        Map<TypeName, String> report = map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          e -> e.getValue().startingActivationPhase().toString()
                                                  + "->" + e.getValue().finishingActivationPhase()));
        assertThat(report, hasEntry(create("io.helidon.inject.tests.inject.HelidonInjection__Application"), "ACTIVE->DESTROYED"));
        assertThat(report, hasEntry(create("io.helidon.inject.tests.inject.HelidonInjection__ModuleComponent"), "ACTIVE->DESTROYED"));
        assertThat(report, hasEntry(create("io.helidon.inject.tests.inject.TestHelidonInjection__Application"), "ACTIVE->DESTROYED"));
        assertThat(report, hasEntry(create("io.helidon.inject.tests.inject.TestHelidonInjection__ModuleComponent"), "ACTIVE->DESTROYED"));
        assertThat(report, hasEntry(create("io.helidon.inject.tests.inject.stacking.MostOuterCommonContractImpl"), "ACTIVE->DESTROYED"));
        assertThat(report, hasEntry(create("io.helidon.inject.tests.inject.stacking.OuterCommonContractImpl"), "ACTIVE->DESTROYED"));
        assertThat(report, hasEntry(create("io.helidon.inject.tests.inject.stacking.CommonContractImpl"), "ACTIVE->DESTROYED"));
        assertThat(report, hasEntry(create("io.helidon.inject.tests.inject.TestingSingleton"), "ACTIVE->DESTROYED"));
        assertThat(report, hasEntry(create("io.helidon.inject.configdriven.runtime.ConfigDrivenInjectModule"), "ACTIVE->DESTROYED"));
        assertThat(report, hasEntry(create("io.helidon.config.HelidonInjection__ModuleComponent"), "ACTIVE->DESTROYED"));

        assertThat(report + " : expected 10 services to be present", report.size(), equalTo(10));

        assertThat(TestingSingleton.postConstructCount(), equalTo(1));
        assertThat(TestingSingleton.preDestroyCount(), equalTo(1));

        assertThat(injectionServices.metrics().orElseThrow().lookupCount().orElse(0), equalTo(0));

        tearDown();
        setUp();
        TestingSingleton testingSingletonFromLookup2 = injectionServices.services().lookup(TestingSingleton.class).get();
        assertThat(testingSingletonFromLookup2, not(testingSingletonFromLookup));

        map = injectionServices.shutdown().orElseThrow();
        report = map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          e2 -> e2.getValue().startingActivationPhase().toString()
                                                  + "->" + e2.getValue().finishingActivationPhase()));
        // now contains config as well
        assertThat(report.toString(), report.size(), is(10));

        tearDown();
        map = injectionServices.shutdown().orElseThrow();
        assertThat(map.toString(), map.size(), is(0));
    }

    @Test
    void knownProviders() {
        List<ServiceProvider<?>> providers = services.lookupAll(
                ServiceInfoCriteria.builder().addContract(Supplier.class).build());
        List<String> desc = providers.stream().map(ServiceProvider::description).collect(Collectors.toList());
        // note that order matters here (weight ranked)
        assertThat(desc,
                contains("ASerialProviderImpl:INIT",
                         "MyServices.MyConcreteClassContractPerRequestIPProvider:INIT",
                         "MyServices.MyConcreteClassContractPerRequestProvider:INIT",
                         "BladeProvider:INIT"));
    }

    @Test
    void classNamed() {
        List<ServiceProvider<?>> providers = services.lookupAll(
                ServiceInfoCriteria.builder()
                        .addQualifier(Qualifier.createNamed(ClassNamedY.class))
                        .build());
        List<String> desc = providers.stream().map(ServiceProvider::description).collect(Collectors.toList());
        assertThat(desc,
                   contains("YImpl:INIT",
                            "BladeProvider:INIT"));

        providers = services.lookupAll(
                ServiceInfoCriteria.builder()
                        .addQualifier(Qualifier.createNamed(ClassNamedY.class.getName()))
                        .build());
        List<String> desc2 = providers.stream().map(ServiceProvider::description).collect(Collectors.toList());
        assertThat(desc2,
                   equalTo(desc));
    }

}
