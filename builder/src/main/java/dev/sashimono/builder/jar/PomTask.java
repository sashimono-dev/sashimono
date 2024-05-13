package dev.sashimono.builder.jar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.util.FileUtil;
import dev.sashimono.builder.util.Log;
import dev.sashimono.builder.util.TaskMap;

/**
 * Copies POM file.
 */
public class PomTask implements Function<TaskMap, FileOutput> {

    private static final Log log = Log.of(PomTask.class);
    private final Path outputDir;
    private final GAV gav;
    private final Path sourcePomPath;

    public PomTask(final Path outputDir, final GAV gav, final Path pomPath) {
        this.outputDir = outputDir;
        this.gav = gav;
        this.sourcePomPath = pomPath;
    }

    @Override
    public FileOutput apply(final TaskMap taskMap) {
        final Path parentDir = FileUtil.getOutputPath(outputDir, gav);
        try {
            Files.createDirectories(parentDir);
            final String pomFileName = gav.artifact() + "-" + gav.version() + ".pom";
            final Path targetPomPath = parentDir.resolve(pomFileName);
            Files.copy(sourcePomPath, targetPomPath, StandardCopyOption.REPLACE_EXISTING);
            log.infof("copied pom to %s", targetPomPath);
            return new FileOutput(targetPomPath);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
