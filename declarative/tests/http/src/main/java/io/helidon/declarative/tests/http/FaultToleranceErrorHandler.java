package io.helidon.declarative.tests.http;

import java.util.Map;

import io.helidon.faulttolerance.CircuitBreakerOpenException;
import io.helidon.faulttolerance.FaultToleranceException;
import io.helidon.http.Status;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.ErrorHandler;
import io.helidon.webserver.http.spi.ErrorHandlerProvider;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;

@Service.Provider
public class FaultToleranceErrorHandler implements ErrorHandlerProvider<FaultToleranceException> {
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Map.of());

    @Override
    public Class<FaultToleranceException> errorType() {
        return FaultToleranceException.class;
    }

    @Override
    public ErrorHandler<FaultToleranceException> create() {
        return (req, res, t) -> {
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", t.getMessage())
                    .build();

            res.status(Status.SERVICE_UNAVAILABLE_503)
                    .send(jsonErrorObject);
        };
    }
}
