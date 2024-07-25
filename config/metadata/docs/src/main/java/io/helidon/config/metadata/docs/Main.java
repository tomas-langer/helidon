package io.helidon.config.metadata.docs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.helidon.logging.common.LogConfig;

/**
 * Main class to start generating data.
 */
public class Main {
    static {
        LogConfig.initClass();
    }

    public static void main(String[] args) throws Exception {
        LogConfig.configureRuntime();

        Path targetPath;

        if (args.length == 0) {
            targetPath = findReasonablePath();
        } else if (args.length == 1) {
            targetPath = Paths.get(args[0]);
        } else {
            throw new IllegalArgumentException("This tool can have zero or one parameters "
                                                       + "(path to generated code). Got " + args.length + " parameters");
        }

        Path path = targetPath.toAbsolutePath().normalize();
        if (Files.exists(path) && Files.isDirectory(path)) {
            ConfigDocumentation docs = ConfigDocumentation.create(path);
            docs.process();
        } else {
            throw new IllegalArgumentException("Target path must be a directory and must exist: "
                                                       + path.toAbsolutePath().normalize());
        }
    }

    private static Path findReasonablePath() {
        Path p = Paths.get("docs/src/main/asciidoc/config");
        if (Files.exists(p) && Files.isDirectory(p)) {
            return p;
        }
        p = Paths.get(".").toAbsolutePath().normalize();
        if (p.toString().replace('\\', '/').endsWith("config/metadata/docs")) {
            // we are probably in Helidon repository in config/metadata/docs
            return p.getParent().getParent().getParent().resolve("docs/src/main/asciidoc/config");
        }
        throw new IllegalArgumentException("Cannot discover config asciidoc path, please provide it as a parameter");
    }
}
