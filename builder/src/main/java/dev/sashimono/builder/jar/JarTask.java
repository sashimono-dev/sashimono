package dev.sashimono.builder.jar;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import dev.sashimono.builder.compiler.CompileResult;
import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.util.FileUtil;
import dev.sashimono.builder.util.Log;
import dev.sashimono.builder.util.StringUtil;
import dev.sashimono.builder.util.TaskMap;

/**
 * Creates a jar file from compiled class files, manifest and optionally prefiltered resource files from Maven.
 */
public class JarTask implements Function<TaskMap, JarResult> {

    private static final Log log = Log.of(JarTask.class);
    public static final String MANIFEST_MF = "MANIFEST.MF";
    public static final String BUILD_TOOL_JDK_SPEC = "Build-Tool-Jdk-Spec";
    public static final String JAVA_SPEC_VERSION = "java.specification.version";
    public static final String EOL = "\r\n"; // For consistency across manifests
    private final Path outputDir;
    private final GAV gav;
    private final Path filteredResourcesDir;
    private final Map<String, String> manifestEntries;

    public JarTask(Path outputDir, GAV gav, Path filteredResourcesDir, Map<String, String> manifestEntries) {
        this.outputDir = outputDir;
        this.gav = gav;
        this.filteredResourcesDir = filteredResourcesDir;
        this.manifestEntries = manifestEntries;
    }

    @Override
    public JarResult apply(TaskMap taskMap) {
        var deps = taskMap.results(CompileResult.class).get(0);
        Path parentDir = FileUtil.getOutputPath(outputDir, gav);
        List<Path> toJar = new ArrayList<>();
        try {
            writeManifestFile(deps.classesDirectory());
            FileUtil.collectFiles(deps.classesDirectory(), toJar);
            if (filteredResourcesDir != null) {
                FileUtil.collectFiles(filteredResourcesDir, toJar);
            }
            toJar.sort(Comparator.comparing(Object::toString));
            Files.createDirectories(parentDir);
            Path target = parentDir.resolve(gav.artifact() + "-" + gav.version() + ".jar");
            try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(target))) {
                toJar.forEach(new Consumer<Path>() {
                    @Override
                    public void accept(Path file) {
                        try {
                            String entryName;
                            String fileName = file.getFileName().toString();
                            if (fileName.endsWith(".class") || fileName.equals(MANIFEST_MF)) {
                                entryName = deps.classesDirectory().relativize(file).toString();
                            } else {
                                entryName = filteredResourcesDir.relativize(file).toString();
                            }
                            ZipEntry entry = new ZipEntry(entryName);
                            entry.setCreationTime(FileTime.fromMillis(0));
                            entry.setSize(Files.size(file));
                            entry.setLastAccessTime(FileTime.fromMillis(0));
                            entry.setLastModifiedTime(FileTime.fromMillis(0));
                            out.putNextEntry(entry);
                            out.write(Files.readAllBytes(file));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            log.infof("created jar %s", target);

            return new JarResult(new ResolvedDependency(new Dependency(gav, "jar"), target, Optional.empty()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void writeManifestFile(final Path classesDir) throws IOException {
        manifestEntries.put(BUILD_TOOL_JDK_SPEC, System.getProperty(JAVA_SPEC_VERSION));
        final Path metaInfDir = classesDir.resolve("META-INF");
        final Path manifestPath = metaInfDir.resolve(MANIFEST_MF);
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
