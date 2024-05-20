package dev.sashimono.builder.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import dev.sashimono.builder.tool.AbstractJavaTool;
import dev.sashimono.builder.util.Log;

public class JavaCompiler extends AbstractJavaTool {

    private static final Log log = Log.of(JavaCompiler.class);

    public JavaCompiler(final javax.tools.JavaCompiler compiler, final List<String> flags, final List<Path> dependencies,
            final List<Path> sourceDirectories) {
        super(compiler, flags, dependencies, sourceDirectories);
        if (compiler == null) {
            throw new RuntimeException("No system java compiler provided");
        }
    }

    @Override
    protected Log getLogger() {
        return log;
    }

    public static JavaCompiler build(final List<Path> dependencies, final List<Path> sourceDirectories,
            final List<String> compilerArguments) {
        return new JavaCompiler(ToolProvider.getSystemJavaCompiler(), compilerArguments, dependencies, sourceDirectories);
    }

    @Override
    public Path process() {
        final javax.tools.JavaCompiler compiler = (javax.tools.JavaCompiler) tool;
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null,
                StandardCharsets.UTF_8);
        final List<File> sourceFiles = collectSourceFiles();
        try {
            final Path output = configureFileManager(fileManager, StandardLocation.CLASS_OUTPUT);
            final DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
            final var sources = fileManager
                    .getJavaFileObjectsFromFiles(sourceFiles);
            final javax.tools.JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager,
                    diagnosticsCollector,
                    this.flags, null, sources);
            final boolean compilationTaskSucceed = task.call();
            processDiagnostics(diagnosticsCollector);
            if (!compilationTaskSucceed) {
                throw new RuntimeException("compilation failed");
            }
            log.infof("Compiled classes to %s", output);
            return output;
        } catch (final IOException e) {
            throw new RuntimeException("Cannot initialize file manager", e);
        }
    }
}
