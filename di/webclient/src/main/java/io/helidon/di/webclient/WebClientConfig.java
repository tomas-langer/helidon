package io.helidon.di.webclient;

import io.helidon.config.Config;
import io.helidon.webclient.WebClient;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Internal;

@EachProperty(value = "client", primary = "default")
@Internal
class WebClientConfig {
    private WebClient.Builder builder;

    protected WebClientConfig(@Parameter String name, Config config) {
        builder = WebClient.builder()
                .config(config.get("client." + name));
    }

    WebClient.Builder clientBuilder() {
        return builder;
    }
}
