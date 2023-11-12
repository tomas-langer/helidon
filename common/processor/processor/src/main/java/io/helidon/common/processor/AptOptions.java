package io.helidon.common.processor;

import java.util.List;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;

public final class AptOptions {
    private final ProcessingEnvironment aptEnv;

    private AptOptions(ProcessingEnvironment aptEnv) {
        this.aptEnv = aptEnv;
    }

    public static AptOptions create(ProcessingEnvironment aptEnv) {
        return new AptOptions(aptEnv);
    }

    public Optional<String> option(String option) {
        return Optional.ofNullable(aptEnv.getOptions().get(option));
    }

    public <T extends Enum<T>> T option(String option, T defaultValue, Class<T> enumType) {
        return option(option)
                .map(it -> Enum.valueOf(enumType, it))
                .orElse(defaultValue);
    }

    public String option(String option, String defaultValue) {
        return option(option).orElse(defaultValue);
    }

    public boolean option(String option, boolean defaultValue) {
        return option(option).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    public List<String> listOption(String option) {
        return option(option).map(it -> it.split(","))
                .map(List::of) // list from array
                .orElseGet(List::of); // empty list
    }
}
