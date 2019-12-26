/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 */

import javax.enterprise.inject.se.SeContainerInitializer;

import io.helidon.microprofile.cdi.HelidonContainerInitializer;

/**
 * CDI implementation enhancements for Helidon MP.
 */
module io.helidon.microprofile.cdi {
    uses javax.enterprise.inject.spi.Extension;
    requires java.logging;
    requires cdi.api;

    requires io.helidon.common;
    requires io.helidon.config;

    requires weld.core.impl;
    requires weld.spi;
    requires weld.environment.common;
    requires weld.se.core;
    requires io.helidon.common.context;
    requires javax.inject;

    exports io.helidon.microprofile.cdi;

    provides SeContainerInitializer with HelidonContainerInitializer;
}
