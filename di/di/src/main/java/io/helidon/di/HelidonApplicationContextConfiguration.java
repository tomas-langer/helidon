package io.helidon.di;

import io.helidon.config.Config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.ApplicationContextConfiguration;

public interface HelidonApplicationContextConfiguration extends ApplicationContextConfiguration {

    @NonNull
    Config.Builder config();
}
