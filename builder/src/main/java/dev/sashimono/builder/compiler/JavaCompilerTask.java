package dev.sashimono.builder.compiler;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.util.TaskMap;

public class JavaCompilerTask implements Function<TaskMap, CompileResult> {

    private final List<Path> sourceDirectories;

    public JavaCompilerTask(List<Path> sourceDirectories) {
        this.sourceDirectories = sourceDirectories;
    }

    @Override
    public CompileResult apply(TaskMap taskMap) {
        //grab both the downloaded and compiled dependencies
        var deps = taskMap.results(ResolvedDependency.class).stream().map(ResolvedDependency::path).toList();
        var compiler = JavaCompiler.build(deps, sourceDirectories);

        return new CompileResult(compiler.compile());
    }
}
