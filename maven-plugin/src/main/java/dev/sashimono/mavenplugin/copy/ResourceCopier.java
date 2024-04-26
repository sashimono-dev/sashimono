package dev.sashimono.mavenplugin.copy;

import static dev.sashimono.mavenplugin.config.ConfigWriter.SASHIMONO_DIR;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.project.MavenProject;

/**
 * Copies Maven prefiltered resources to a .sashimono directory
 * Excludes .class files
 */
public class ResourceCopier {

    public static final String RESOURCES_DIR = "resources";
    public static final String CLASS_EXT = ".class";

    public static boolean copyResources(final MavenProject project, final File buildOutputDirectory) {
        final AtomicBoolean resourcesCopied = new AtomicBoolean(false);
        final Path destDirPath = project.getBasedir().toPath().resolve(SASHIMONO_DIR + File.separator + RESOURCES_DIR);
        final Path resourcePath = buildOutputDirectory.toPath();
        try {
            final String resourcePathStr = resourcePath.toString();
            Files.walkFileTree(resourcePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path filePath, final BasicFileAttributes attrs)
                        throws IOException {
                    if (!filePath.getFileName().toString().endsWith(CLASS_EXT)) {
                        final String subPath = filePath.toString().substring(resourcePathStr.length() + 1);
                        final Path destFilePath = destDirPath.resolve(subPath);
                        // Make sure directories already exist
                        Files.createDirectories(destFilePath.getParent());
                        Files.copy(filePath, destFilePath, StandardCopyOption.REPLACE_EXISTING);
                        resourcesCopied.set(true);
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return resourcesCopied.get();
    }
}
