package io.helidon.inject.maven.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.codegen.CodegenOptions;

class MavenOptions implements CodegenOptions {
    private final Map<String, String> options;

    private MavenOptions(Map<String, String> options) {
        this.options = options;
    }

    public static CodegenOptions create(Set<String> compilerArgs) {
        Map<String, String> options = new HashMap<>();

        compilerArgs.forEach(it -> addInjectOption(options, it));

        return new MavenOptions(Map.copyOf(options));
    }

    @Override
    public Optional<String> option(String option) {
        return Optional.ofNullable(options.get(option));
    }

    private static void addInjectOption(Map<String, String> options, String option) {
        String toProcess = option;
        if (toProcess.startsWith("-A")) {
            toProcess = toProcess.substring(2);
        }
        int eq = toProcess.indexOf('=');
        if (eq < 0) {
            options.put(toProcess, "true");
            return;
        }
        options.put(toProcess.substring(0, eq), toProcess.substring(eq + 1));
    }
}
