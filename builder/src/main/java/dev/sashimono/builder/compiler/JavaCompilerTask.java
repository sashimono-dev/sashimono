package dev.sashimono.builder.compiler;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.util.TaskMap;

public class JavaCompilerTask implements Function<TaskMap, Path> {

    private final List<Path> sourceDirectories;

    public JavaCompilerTask(List<Path> sourceDirectories) {
        this.sourceDirectories = sourceDirectories;
    }

    @Override
    public Path apply(TaskMap taskMap) {
        var deps = taskMap.results(ResolvedDependency.class).stream().map(ResolvedDependency::path)
                .collect(Collectors.toList());

        var compiler = JavaCompiler.build(deps, sourceDirectories);

        return compiler.compile();
    }
}
