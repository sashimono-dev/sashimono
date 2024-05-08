package dev.sashimono.builder.documenter;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.util.TaskMap;

public class JavaDocumenterTask implements Function<TaskMap, DocumentationResult> {

    private final List<Path> sourceDirectories;

    public JavaDocumenterTask(final List<Path> sourceDirectories) {
        this.sourceDirectories = sourceDirectories;
    }

    @Override
    public DocumentationResult apply(final TaskMap taskMap) {
        //grab both the downloaded and compiled dependencies
        final List<Path> deps = taskMap.results(ResolvedDependency.class).stream().map(ResolvedDependency::path).toList();
        final JavaDocumenter documenter = JavaDocumenter.build(deps, sourceDirectories);

        return new DocumentationResult(documenter.process());
    }
}
