package dev.sashimono.builder.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Stream;

import dev.sashimono.builder.config.GAV;

public class FileUtil {
    public static void deleteRecursive(final java.nio.file.Path file) {
        try {
            if (Files.isDirectory(file)) {
                try (final Stream<Path> files = Files.list(file)) {
                    files.forEach(FileUtil::deleteRecursive);
                }
            }
            Files.delete(file);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void collectFiles(final Path dir, final List<Path> files) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    files.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getOutputPath(final Path outputDir, final GAV gav) {
        Path parentDir = outputDir;
        final var groupParts = gav.group().split("\\.");
        for (final var i : groupParts) {
            parentDir = parentDir.resolve(i);
        }
        parentDir = parentDir.resolve(gav.artifact());
        parentDir = parentDir.resolve(gav.version());
        return parentDir;
    }
}
