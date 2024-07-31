package io.helidon.service.maven.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.FilerResource;

class MavenFilerResource implements FilerResource {
    private final Path resourcePath;

    private byte[] currentBytes;
    private boolean modified;

    MavenFilerResource(Path resourcePath) {
        this(resourcePath, new byte[0]);
    }

    MavenFilerResource(Path resourcePath, byte[] bytes) {
        this.resourcePath = resourcePath;
        this.currentBytes = bytes;
    }

    @Override
    public byte[] bytes() {
        return Arrays.copyOf(currentBytes, currentBytes.length);
    }

    @Override
    public void bytes(byte[] newBytes) {
        currentBytes = Arrays.copyOf(newBytes, newBytes.length);
        modified = true;
    }

    @Override
    public void write() {
        if (modified) {
            try {
                Files.write(resourcePath, currentBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new CodegenException("Failed to write resource " + resourcePath.toAbsolutePath(), e);
            }
        }
    }
}
