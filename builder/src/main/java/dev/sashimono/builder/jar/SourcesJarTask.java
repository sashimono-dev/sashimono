package dev.sashimono.builder.jar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.util.TaskMap;

/**
 * Creates a jar file from sources and optionally prefiltered resource files from Maven.
 */
public class SourcesJarTask extends AbstractJarTask implements Function<TaskMap, FileOutput> {

    private final List<Path> sourceDirs;

    public SourcesJarTask(final Path outputDir, final GAV gav, final Path filteredResourcesDir, final List<Path> sourceDirs) {
        super(outputDir, gav, filteredResourcesDir);
        this.sourceDirs = sourceDirs;
    }

    @Override
    public FileOutput apply(final TaskMap taskMap) {
        try {
            // TODO Write manifest file like JarTask
            collectFiles(sourceDirs.toArray(new Path[0]));
            return new FileOutput(createJar("sources"));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
