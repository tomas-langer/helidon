package io.helidon.webserver.testing.junit5;

public class TestConfig {
    void set(String key, String value) {
        TestConfigSource.set(key, value);
    }
}
