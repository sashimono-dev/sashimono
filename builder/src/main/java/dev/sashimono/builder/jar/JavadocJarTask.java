package dev.sashimono.builder.jar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.documenter.DocumentationResult;
import dev.sashimono.builder.util.Log;
import dev.sashimono.builder.util.TaskMap;

/**
 * Creates a jar file from javadoc.
 */
public class JavadocJarTask extends AbstractJarTask implements Function<TaskMap, FileOutput> {

    private static final Log log = Log.of(JavadocJarTask.class);

    public JavadocJarTask(final Path outputDir, final GAV gav, final Path filteredResourcesDir) {
        super(outputDir, gav, "javadoc", filteredResourcesDir);
    }

    @Override
    protected Log getLogger() {
        return log;
    }

    @Override
    public FileOutput apply(final TaskMap taskMap) {
        final DocumentationResult deps = taskMap.results(DocumentationResult.class).get(0);
        try {
            // TODO Write manifest file like JarTask
            collectFiles(deps.documentationDirectory());
            return new FileOutput(createJar());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
