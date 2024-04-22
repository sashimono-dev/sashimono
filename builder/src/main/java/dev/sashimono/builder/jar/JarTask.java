package dev.sashimono.builder.jar;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
import dev.sashimono.builder.util.Log;
import dev.sashimono.builder.util.TaskMap;

/**
 * Creates a jar file from compiled class files.
 */
public class JarTask implements Function<TaskMap, JarResult> {

    private static final Log log = Log.of(JarTask.class);
    private final Path outputDir;
    private final GAV gav;

    public JarTask(Path outputDir, GAV gav) {
        this.outputDir = outputDir;
        this.gav = gav;
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
            Files.walkFileTree(deps.classesDirectory(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    toJar.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            toJar.sort(Comparator.comparing(Object::toString));
            Files.createDirectories(parentDir);
            Path target = parentDir.resolve(gav.artifact() + "-" + gav.version() + ".jar");
            try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(target))) {
                toJar.forEach(new Consumer<Path>() {
                    @Override
                    public void accept(Path file) {
                        try {
                            String entryName = deps.classesDirectory().relativize(file).toString();
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
