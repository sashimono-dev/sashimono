package dev.sashimono.builder.jar;

import java.util.Optional;

import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.util.ResultMapper;

/**
 * The result of creating a jar file
 *
 * @param result The path to the jar
 */
public record JarResult(ResolvedDependency result) {
    public static ResultMapper<JarResult, FileOutput> FILE_OUTPUT_MAPPER = new ResultMapper<JarResult, FileOutput>() {

        @Override
        public FileOutput map(JarResult val) {
            return new FileOutput(val.result().path());
        }
    };

    public static final ResultMapper<JarResult, ResolvedDependency> RESOLVED_DEPENDENCY_MAPPER = new ResultMapper<JarResult, ResolvedDependency>() {
        @Override
        public ResolvedDependency map(JarResult jar) {
            return new ResolvedDependency(jar.result().dependency(), jar.result().path(), Optional.empty());
        }
    };

}
