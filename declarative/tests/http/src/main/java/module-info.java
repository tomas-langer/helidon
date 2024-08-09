module io.helidon.declarative.tests.http {
    requires io.helidon.http;
    requires io.helidon.common.media.type;
    requires io.helidon.webserver;
    requires io.helidon.service.inject;
    requires jakarta.json;
    requires io.helidon.config.yaml;
    requires io.helidon.webserver.security;
    requires io.helidon.metrics.api;
    // todo remove - now needed for interceptors
    requires io.helidon.metrics;
    requires io.helidon.faulttolerance;

    exports io.helidon.declarative.tests.http;
}