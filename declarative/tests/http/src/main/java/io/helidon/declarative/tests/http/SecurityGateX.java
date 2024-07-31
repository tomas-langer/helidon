package io.helidon.declarative.tests.http;

import java.lang.annotation.Annotation;
import java.util.List;

import io.helidon.common.config.Config;
import io.helidon.security.EndpointConfig;
import io.helidon.security.SecurityContext;
import io.helidon.security.SecurityLevel;
import io.helidon.security.SubjectType;
import io.helidon.security.abac.role.RoleValidator;
import io.helidon.service.inject.api.Injection;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.security.SecurityGate;

@Injection.Named(SecurityGateX.NAME)
class SecurityGateX implements SecurityGate {
    static final String NAME = "io.helidon.declarative.tests.http.GreetEndpoint.updateGreetingHandlerReturningCurrent(jakarta"
            + ".json.JsonObject,io.helidon.security.SecurityContext)";

    private static final System.Logger LOGGER = System.getLogger(SecurityGateX.class.getName());

    private static final SecurityLevel SECURITY_LEVEL = SecurityLevel.create("io.helidon.declarative.tests.http.GreetEndpoint")
            .withMethodName("updateGreetingHandlerReturningCurrent")
            .addMethodAnnotation(new RoleValidator.Roles() {
                @Override
                public String[] value() {
                    return new String[] {"admin"};
                }

                @Override
                public SubjectType subjectType() {
                    return SubjectType.USER;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return RoleValidator.Roles.class;
                }
            })
            .build();
    private final Config config;

    SecurityGateX(Config config) {
        this.config = config;
    }

    public boolean isAuthenticated() {
        return true;
    }

    public boolean isAuthorized() {
        return true;
    }

    @Override
    public void handleSecurity(ServerRequest request, ServerResponse response, SecurityContext securityContext) {
        if (!config.get(NAME).get("enabled").asBoolean().orElse(false)) {
            LOGGER.log(System.Logger.Level.TRACE, "Security gate not enabled.");
            return;
        }

        // if requiresAuthentication && requiresAuthorization
        // if either, just check the one that is required
        if (securityContext.isAuthenticated() && securityContext.isAuthorized()) {
            // if this was handled through configuration of Security feature path, ignore this
            return;
        }

        securityContext.endpointConfig(EndpointConfig.builder()
                                               .securityLevels(List.of(SECURITY_LEVEL))
                                               .build());

        // let's say we require both
        if (!securityContext.isAuthenticated()) {

        }

        if (!securityContext.isAuthorized()) {

        }
    }
}
