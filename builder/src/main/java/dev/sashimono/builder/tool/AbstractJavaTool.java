package dev.sashimono.builder.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import dev.sashimono.builder.util.Log;

public abstract class AbstractJavaTool {

    protected abstract Log getLogger();

    public abstract Path process();

    protected final javax.tools.Tool tool;
    protected final List<String> flags;
    protected final List<Path> dependencies;
    protected final List<Path> sourceDirectories;

    protected AbstractJavaTool(final javax.tools.Tool tool, final List<String> flags, final List<Path> dependencies,
            final List<Path> sourceDirectories) {
        this.tool = tool;
        this.flags = flags;
        this.dependencies = dependencies;
        this.sourceDirectories = sourceDirectories;
    }

    protected List<File> collectSourceFiles() {
        final List<File> sourceFiles = new ArrayList<>();
        sourceDirectories.forEach(s -> {
            try {
                Files.walkFileTree(s, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                        if (file.getFileName().toString().endsWith(".java")) {
                            sourceFiles.add(file.toFile());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                });
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
        //we sort the source files to help with reproducibility
        sourceFiles.sort(Comparator.comparing(Object::toString));
        return sourceFiles;
    }

    protected Path configureFileManager(final StandardJavaFileManager fileManager, final JavaFileManager.Location to)
            throws IOException {
        final Path output = Files.createTempDirectory("output");
        fileManager.setLocation(StandardLocation.CLASS_PATH,
                dependencies.stream().map(Path::toFile).collect(Collectors.toSet()));
        fileManager.setLocation(to, List.of(output.toFile()));
        return output;
    }

    protected void processDiagnostics(final DiagnosticCollector<JavaFileObject> diagnosticsCollector) {
        for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnosticsCollector.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                getLogger().error(diagnostic.getMessage(Locale.getDefault()));
            } else {
                getLogger().info(diagnostic.getMessage(Locale.getDefault()));
            }
        }
    }
}
