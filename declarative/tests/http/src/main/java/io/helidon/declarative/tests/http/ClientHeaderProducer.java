package io.helidon.declarative.tests.http;

import java.util.Optional;

import io.helidon.service.registry.Service;
import io.helidon.webclient.api.RestClient;

@Service.Provider
class ClientHeaderProducer implements RestClient.HeaderProducer {
    @Override
    public Optional<String> produceHeader(String name) {
        return switch (name) {
            case "X-Computed" -> Optional.of("Computed-Value");
            default -> Optional.empty();
        };
    }
}
