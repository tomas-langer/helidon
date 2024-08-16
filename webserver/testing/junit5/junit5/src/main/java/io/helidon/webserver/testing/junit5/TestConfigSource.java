package io.helidon.webserver.testing.junit5;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.common.Weight;
import io.helidon.config.spi.ConfigNode;
import io.helidon.config.spi.ConfigSource;
import io.helidon.config.spi.LazyConfigSource;
import io.helidon.service.registry.Service;

@Service.Provider
@Weight(1000) // must be a very high weights, so we can override whatever we want
class TestConfigSource implements LazyConfigSource, ConfigSource {
    private static final Map<String, String> OPTIONS = new HashMap<>();
    private static final ReentrantLock LOCK = new ReentrantLock();

    TestConfigSource() {
    }

    static void set(String key, String value) {
        LOCK.lock();
        try {
            OPTIONS.put(key, value);
        } finally {
            LOCK.unlock();
        }
    }

    static void clear() {
        LOCK.lock();
        try {
            OPTIONS.clear();
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public Optional<ConfigNode> node(String key) {
        LOCK.lock();
        try {
            String value = OPTIONS.get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(ConfigNode.ValueNode.create(value));
        } finally {
            LOCK.unlock();
        }
    }
}
