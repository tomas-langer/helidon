package io.helidon.service.inject;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.config.Config;
import io.helidon.service.registry.Service;

@Service.Provider
@Weight(0.1) // a very low weight, as this just provides an empty config
class ConfigProvider implements Supplier<Config> {
    private final Config config = Config.empty();

    @Override
    public Config get() {
        return config;
    }
}
