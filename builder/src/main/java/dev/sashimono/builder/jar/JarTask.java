package dev.sashimono.builder.jar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import dev.sashimono.builder.util.TaskMap;

/**
 * Creates a jar file from compiled class files and optionally prefiltered resource files from Maven.
 */
public class JarTask implements Function<TaskMap, JarResult> {

    private static final Log log = Log.of(JarTask.class);
    private final Path outputDir;
    private final GAV gav;
    private final Path filteredResourcesDir;

    public JarTask(Path outputDir, GAV gav, Path filteredResourcesDir) {
        this.outputDir = outputDir;
        this.gav = gav;
        this.filteredResourcesDir = filteredResourcesDir;
    }

    @Override
    public JarResult apply(TaskMap taskMap) {
        var deps = taskMap.results(CompileResult.class).get(0);
        Path parentDir = outputDir;
        var groupParts = gav.group().split("\\.");
        for (var i : groupParts) {
            parentDir = parentDir.resolve(i);
        }
        parentDir = parentDir.resolve(gav.artifact());
        parentDir = parentDir.resolve(gav.version());
        List<Path> toJar = new ArrayList<>();
        try {
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
                            if (file.getFileName().toString().endsWith(".class")) {
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
}
