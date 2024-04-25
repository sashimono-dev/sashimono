package dev.sashimono.builder.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Stream;

public class FileUtil {
    public static void deleteRecursive(final java.nio.file.Path file) {
        try {
            if (Files.isDirectory(file)) {
                try (Stream<Path> files = Files.list(file)) {
                    files.forEach(FileUtil::deleteRecursive);
                }
            }
            Files.delete(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void collectFiles(Path dir, List<Path> files) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    files.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
