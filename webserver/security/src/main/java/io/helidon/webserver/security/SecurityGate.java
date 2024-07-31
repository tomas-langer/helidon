package io.helidon.webserver.security;

import java.util.Optional;

import io.helidon.security.SecurityContext;
import io.helidon.security.SecurityLevel;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/**
 * Used from generated types.
 */
public interface SecurityGate {
    void handleSecurity(ServerRequest req, ServerResponse res, SecurityContext securityContext);

    boolean isAuthenticated();
    boolean isAuthorized();

    SecurityLevel securityLevel();

    boolean authenticationOptional();


    default Optional<String> authenticator() {
        return Optional.empty();
    }
}
