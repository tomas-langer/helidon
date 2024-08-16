package io.helidon.declarative.tests.http;

import io.helidon.http.Http;
import io.helidon.webclient.api.RestClient;

@RestClient.Endpoint
public interface GreetEndpointClient extends GreetEndpointApi {
    /**
     * Return a worldly greeting message.
     */
    @Http.GET
    @Http.Produces("text/plain")
    String getDefaultMessageHandlerPlain();
}
