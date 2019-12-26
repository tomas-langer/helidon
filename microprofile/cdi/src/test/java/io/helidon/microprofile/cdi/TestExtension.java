/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.microprofile.cdi;

import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import io.helidon.config.Config;

/**
 * Testing class.
 */
public class TestExtension implements Extension {
    static final String BUILD_TIME_INIT = "bti";
    static final String BUILD_TIME_BEFORE_DESTROYED = "btbd";
    static final String BUILD_TIME_DESTROYED = "btd";
    static final String RUNTIME_INIT = "rti";
    static final String RUNTIME_BEFORE_DESTROYED = "rtbd";
    static final String RUNTIME_DESTROYED = "rtd";
    static final String APPLICATION_INIT = "ai";
    static final String APPLICATION_BEFORE_DESTROYED = "abd";
    static final String APPLICATION_DESTROYED = "ad";

    private final List<String> events = new LinkedList<>();
    private Config runtimeConfig;

    // must be public so it works with java 11 (do not want to open this module to weld)
    public void registerBeans(@Observes BeforeBeanDiscovery bbd) {
        bbd.addAnnotatedType(TestBean.class, "unit-test-bean");
        bbd.addAnnotatedType(TestBean2.class, "unit-test-bean2");
    }

    public void buildTimeInit(@Observes @Initialized(BuildTimeScoped.class) Object event) {
        events.add(BUILD_TIME_INIT);
    }

    public void buildTimeBeforeDestroyed(@Observes @BeforeDestroyed(BuildTimeScoped.class) Object event) {
        events.add(BUILD_TIME_BEFORE_DESTROYED);
    }

    public void buildTimeDestroyed(@Observes @Destroyed(BuildTimeScoped.class) Object event) {
        events.add(BUILD_TIME_DESTROYED);
    }

    public void runTimeInit(@Observes @Initialized(RuntimeScoped.class) Config config) {
        events.add(RUNTIME_INIT);
        runtimeConfig = config;
    }

    public void runTimeBeforeDestroyed(@Observes @BeforeDestroyed(RuntimeScoped.class) Object event) {
        events.add(RUNTIME_BEFORE_DESTROYED);
    }

    public void runTimeDestroyed(@Observes @Destroyed(RuntimeScoped.class) Object event) {
        events.add(RUNTIME_DESTROYED);
    }

    public void applicationInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        events.add(APPLICATION_INIT);
    }

    public void applicationBeforeDestroyed(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
        events.add(APPLICATION_BEFORE_DESTROYED);
    }

    public void applicationDestroyed(@Observes @Destroyed(ApplicationScoped.class) Object event) {
        events.add(APPLICATION_DESTROYED);
    }

    List<String> events() {
        return events;
    }

    Config runtimeConfig() {
        return runtimeConfig;
    }
}
