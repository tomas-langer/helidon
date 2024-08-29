package io.helidon.declarative.tests.http.security;

import io.helidon.builder.api.Prototype;
import io.helidon.common.types.TypeName;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

@Prototype.Blueprint
interface HttpSecurityInterceptorContextBlueprint {
    ServerRequest request();
    ServerResponse response();
    TypeName serviceType();
}
