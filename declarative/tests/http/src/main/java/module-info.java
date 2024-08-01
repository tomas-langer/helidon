module io.helidon.declarative.tests.http {
    requires io.helidon.http;
    requires io.helidon.common.media.type;
    requires io.helidon.webserver;
    requires io.helidon.service.inject;
    requires jakarta.json;
    requires io.helidon.config.yaml;
    requires io.helidon.webserver.security;

    exports io.helidon.declarative.tests.http;
}