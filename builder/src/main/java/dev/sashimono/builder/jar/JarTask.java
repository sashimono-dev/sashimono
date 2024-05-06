package dev.sashimono.builder.jar;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import dev.sashimono.builder.compiler.CompileResult;
import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.util.StringUtil;
import dev.sashimono.builder.util.TaskMap;

/**
 * Creates a jar file from compiled class files, manifest and optionally prefiltered resource files from Maven.
 */
public class JarTask extends AbstractJarTask implements Function<TaskMap, JarResult> {

    public static final String BUILD_TOOL_JDK_SPEC = "Build-Tool-Jdk-Spec";
    public static final String JAVA_SPEC_VERSION = "java.specification.version";
    public static final String EOL = "\r\n"; // For consistency across manifests
    private final Map<String, String> manifestEntries;

    public JarTask(final Path outputDir, final GAV gav, final Path filteredResourcesDir,
            final Map<String, String> manifestEntries) {
        super(outputDir, gav, filteredResourcesDir);
        this.manifestEntries = manifestEntries;
    }

    @Override
    public JarResult apply(final TaskMap taskMap) {
        final CompileResult deps = taskMap.results(CompileResult.class).get(0);
        try {
            writeManifestFile(deps.classesDirectory());
            collectFiles(deps.classesDirectory());
            final Path target = createJar();
            return new JarResult(new ResolvedDependency(new Dependency(gav, "jar"), target, Optional.empty()));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void writeManifestFile(final Path classesDir) throws IOException {
        manifestEntries.put(BUILD_TOOL_JDK_SPEC, System.getProperty(JAVA_SPEC_VERSION));
        final Path metaInfDir = classesDir.resolve("META-INF");
        final Path manifestPath = metaInfDir.resolve("MANIFEST.MF");
        Files.createDirectories(metaInfDir);
        try (final BufferedWriter writer = Files.newBufferedWriter(manifestPath)) {
            final String delimiter = ": ";
            final int maxLineBytes = 72;
            final int maxLineBytesLessEol = maxLineBytes - 2;
            for (final Map.Entry<String, String> entry : manifestEntries.entrySet()) {
                String line = StringUtil.camelToCapitalisedKebabCase(entry.getKey()) + delimiter + entry.getValue();
                final StringBuilder toWrite = new StringBuilder();
                while (line.getBytes(StandardCharsets.UTF_8).length > maxLineBytes) {
                    int index = maxLineBytesLessEol;
                    String newLine = line.substring(0, index);
                    while (newLine.getBytes(StandardCharsets.UTF_8).length > maxLineBytes && index > 0) {
                        newLine = line.substring(0, --index);
                    }
                    toWrite.append(newLine).append(EOL);
                    line = " " + line.substring(index);
                }
                toWrite.append(line).append(EOL);
                writer.write(toWrite.toString());
            }
        }
    }
}
