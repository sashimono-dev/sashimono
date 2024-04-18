package dev.sashimono.builder.compiler;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import dev.sashimono.builder.util.Log;

public class JavaCompiler {

    static final Log log = Log.of(JavaCompiler.class);

    private final javax.tools.JavaCompiler compiler;
    private final List<String> compilerFlags;
    private final List<Path> dependencies;
    private final List<Path> sourceDirectories;

    public static JavaCompiler build(List<Path> dependencies, List<Path> sourceDirectories) {
        return new JavaCompiler(ToolProvider.getSystemJavaCompiler(), List.of(), dependencies, sourceDirectories);
    }

    JavaCompiler(javax.tools.JavaCompiler compiler, List<String> compilerFlags, List<Path> dependencies,
            List<Path> sourceDirectories) {
        this.compiler = compiler;
        this.compilerFlags = compilerFlags;
        this.dependencies = dependencies;
        this.sourceDirectories = sourceDirectories;
        if (compiler == null) {
            throw new RuntimeException("No system java compiler provided");
        }
    }

    public Path compile() {

        StandardJavaFileManager fileManager = compiler.getStandardFileManager((DiagnosticListener) null, (Locale) null,
                StandardCharsets.UTF_8);

        Set<File> sourceFiles = new HashSet<>();
        sourceDirectories.forEach(s -> {
            try {
                Files.walkFileTree(s, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.getFileName().toString().endsWith(".java")) {
                            sourceFiles.add(file.toFile());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            var output = Files.createTempDirectory("output");
            fileManager.setLocation(StandardLocation.CLASS_PATH,
                    dependencies.stream().map(Path::toFile).collect(Collectors.toSet()));
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(output.toFile()));

            DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector();
            var sources = fileManager
                    .getJavaFileObjectsFromFiles(sourceFiles);
            javax.tools.JavaCompiler.CompilationTask task = this.compiler.getTask((Writer) null, fileManager,
                    diagnosticsCollector,
                    this.compilerFlags, (Iterable) null, sources);
            boolean compilationTaskSucceed = task.call();

            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticsCollector.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    log.error(diagnostic.getMessage(Locale.getDefault()));
                } else {
                    log.info(diagnostic.getMessage(Locale.getDefault()));
                }
            }

            if (!compilationTaskSucceed) {
                throw new RuntimeException("compilation failed");
            }
            log.info("Compiled classes to %s", output);
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize file manager", e);
        }
    }

}
