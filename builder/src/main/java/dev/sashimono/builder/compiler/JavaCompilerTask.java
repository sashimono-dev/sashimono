package dev.sashimono.builder.compiler;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.util.TaskMap;

public class JavaCompilerTask implements Function<TaskMap, CompileResult> {

    private final List<Path> sourceDirectories;
    private final List<String> compilerArguments;

    public JavaCompilerTask(final List<Path> sourceDirectories, final List<String> compilerArguments) {
        this.sourceDirectories = sourceDirectories;
        this.compilerArguments = compilerArguments;
    }

    @Override
    public CompileResult apply(final TaskMap taskMap) {
        //grab both the downloaded and compiled dependencies
        final List<Path> deps = taskMap.results(ResolvedDependency.class).stream().map(ResolvedDependency::path).toList();
        final JavaCompiler compiler = JavaCompiler.build(deps, sourceDirectories, compilerArguments);

        return new CompileResult(compiler.process());
    }
}
