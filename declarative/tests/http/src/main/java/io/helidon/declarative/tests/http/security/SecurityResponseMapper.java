package io.helidon.declarative.tests.http.security;

import io.helidon.security.SecurityResponse;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.ServerResponse;

/**
 * A {@link SecurityResponse} mapper that is called when a security error is
 * encountered. Gives a chance for applications to craft a more informative
 * response to the user as to the cause of the error.
 */
@FunctionalInterface
@Service.Contract
public interface SecurityResponseMapper {
    /**
     * Called when a security response is aborted due to a security problem (e.g. authentication
     * failure). Handles control to the application to construct the response returned to
     * the client. Security providers can provide context to mappers using the Helidon
     * context mechanism.
     *
     * @param serverResponse   the web server response
     *                         (never call {@link io.helidon.webserver.http.ServerResponse#send(java.lang.Object)} as part of
     *                         handling; return appropriate return message
     * @param securityResponse the security response
     * @param message          message to be written to the response
     * @return new message to be written to the response, never {@code null}
     * @see io.helidon.common.context.Contexts#context()
     */
    String aborted(ServerResponse serverResponse,
                   SecurityResponse securityResponse,
                   String message);
}
