/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 */
package io.helidon.tests.integration.nativeimage.mp1;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * A custom health check.
 */
@Liveness
@ApplicationScoped
public class Healthcheck implements HealthCheck {
    @Inject
    @ConfigProperty(name = "app.message")
    private String message;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("message")
                .state(true)
                .withData("app.message", message)
                .build();
    }
}
