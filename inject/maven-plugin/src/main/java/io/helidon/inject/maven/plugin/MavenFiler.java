package io.helidon.inject.maven.plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.CodegenFiler;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.common.types.TypeName;

class MavenFiler implements CodegenFiler {
    private final Path generatedSourceDir;
    private final Path outputDirectory;
    private boolean generatedSources;

    public MavenFiler(Path generatedSourceDir, Path outputDirectory) {
        this.generatedSourceDir = generatedSourceDir;
        this.outputDirectory = outputDirectory;
    }

    public static MavenFiler create(Path generatedSourceDir, Path outputDirectory) {
        return new MavenFiler(generatedSourceDir, outputDirectory);
    }

    @Override
    public Path writeSourceFile(ClassModel classModel, Object... originatingElements) {
        TypeName typeName = classModel.typeName();
        String pathToSourceFile = typeName.packageName().replace('.', '/');
        String fileName = typeName.className() + ".java";
        Path path = generatedSourceDir.resolve(pathToSourceFile)
                .resolve(fileName);
        mkdirs(path.getParent());

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            classModel.write(writer, "    ");
            generatedSources = true;
        } catch (IOException e) {
            throw new CodegenException("Failed to write new source file: " + path.toAbsolutePath(), e, typeName);
        }
        return path;
    }

    @Override
    public Path writeResource(byte[] resource, String location, Object... originatingElements) {
        Path path = outputDirectory.resolve(location);
        mkdirs(path.getParent());
        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            out.write(resource);
        } catch (IOException e) {
            throw new CodegenException("Failed to write new resource file: " + path.toAbsolutePath(), location);
        }
        return path;
    }

    public boolean generatedSources() {
        return generatedSources;
    }

    private void mkdirs(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new CodegenException("Failed to create directories for: " + path.toAbsolutePath());
        }
    }
}
