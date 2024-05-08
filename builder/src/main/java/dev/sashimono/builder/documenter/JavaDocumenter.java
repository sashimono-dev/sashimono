package dev.sashimono.builder.documenter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.DocumentationTool;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import dev.sashimono.builder.compiler.JavaCompiler;
import dev.sashimono.builder.tool.AbstractJavaTool;
import dev.sashimono.builder.util.Log;

public class JavaDocumenter extends AbstractJavaTool {

    private static final Log log = Log.of(JavaCompiler.class);

    public JavaDocumenter(final javax.tools.DocumentationTool documenter, final List<String> flags,
            final List<Path> dependencies, final List<Path> sourceDirectories) {
        super(documenter, flags, dependencies, sourceDirectories);
        if (documenter == null) {
            throw new RuntimeException("No system java documenter provided");
        }
    }

    @Override
    protected Log getLogger() {
        return log;
    }

    public static JavaDocumenter build(final List<Path> dependencies, final List<Path> sourceDirectories) {
        // '-notimestamp' prevents timestamp from being written to .html files, ensures that hashes are identical between builds
        return new JavaDocumenter(ToolProvider.getSystemDocumentationTool(), List.of("-notimestamp"), dependencies,
                sourceDirectories);
    }

    @Override
    public Path process() {
        final javax.tools.DocumentationTool documenter = (DocumentationTool) tool;
        final StandardJavaFileManager fileManager = documenter.getStandardFileManager(null, null,
                StandardCharsets.UTF_8);
        final List<File> sourceFiles = collectSourceFiles();
        try {
            final Path output = configureFileManager(fileManager, DocumentationTool.Location.DOCUMENTATION_OUTPUT);
            final DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
            final Iterable<? extends JavaFileObject> sources = fileManager
                    .getJavaFileObjectsFromFiles(sourceFiles);
            final DocumentationTool.DocumentationTask task = documenter.getTask(null, fileManager,
                    diagnosticsCollector,
                    null, this.flags, sources);
            final boolean documentationTaskSucceeded = task.call();
            processDiagnostics(diagnosticsCollector);
            if (!documentationTaskSucceeded) {
                throw new RuntimeException("documenting failed");
            }
            log.infof("Documented classes to %s", output);
            return output;
        } catch (final IOException e) {
            throw new RuntimeException("Cannot initialize file manager", e);
        }
    }

}
