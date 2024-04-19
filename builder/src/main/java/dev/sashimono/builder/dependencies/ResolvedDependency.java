package dev.sashimono.builder.dependencies;

import java.nio.file.Path;
import java.util.Optional;

import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.Repository;

/**
 * A project dependency. Note that currently this is not used for jars built as part of the project.
 *
 * @see dev.sashimono.builder.jar.JarResult
 * @param dependency The dependency
 * @param path The path to the jar file
 * @param repository The repository it was downloaded from
 */
public record ResolvedDependency(Dependency dependency, Path path, Optional<Repository> repository) {

}
