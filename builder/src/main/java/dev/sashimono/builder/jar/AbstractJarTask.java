package dev.sashimono.builder.jar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.util.FileUtil;
import dev.sashimono.builder.util.Log;

public abstract class AbstractJarTask {

    public static final String DELIMITER = "-";
    protected final Path outputDir;
    protected final GAV gav;
    protected final Path filteredResourcesDir;
    protected final Map<Path, List<Path>> toJarFilesByDir = new TreeMap<>();

    public AbstractJarTask(final Path outputDir, final GAV gav, final Path filteredResourcesDir) {
        this.outputDir = outputDir;
        this.gav = gav;
        this.filteredResourcesDir = filteredResourcesDir;
    }

    protected abstract Log getLogger();

    protected Path createJar() throws IOException {
        return createJar("");
    }

    protected Path createJar(final String suffix) throws IOException {
        if (filteredResourcesDir != null) {
            collectFiles(filteredResourcesDir);
        }
        final Path parentDir = FileUtil.getOutputPath(outputDir, gav);
        Files.createDirectories(parentDir);
        String artifactName = gav.artifact() + DELIMITER + gav.version();
        if (!suffix.isBlank()) {
            artifactName += DELIMITER + suffix;
        }
        artifactName += ".jar";
        final Path target = parentDir.resolve(artifactName);
        try (final JarOutputStream out = new JarOutputStream(Files.newOutputStream(target))) {
            toJarFilesByDir.forEach((dir, files) -> {
                try {
                    files.sort(Comparator.comparing(Object::toString));
                    for (final Path file : files) {
                        final String entryName = dir.relativize(file).toString();
                        final ZipEntry entry = new ZipEntry(entryName);
                        entry.setCreationTime(FileTime.fromMillis(0));
                        entry.setSize(Files.size(file));
                        entry.setLastAccessTime(FileTime.fromMillis(0));
                        entry.setLastModifiedTime(FileTime.fromMillis(0));
                        out.putNextEntry(entry);
                        out.write(Files.readAllBytes(file));
                    }
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        getLogger().infof("created jar %s", target);
        return target;
    }

    protected void collectFiles(final Path... dirs) throws IOException {
        for (final Path dir : dirs) {
            toJarFilesByDir.put(dir, FileUtil.collectFiles(dir));
        }
    }
}
