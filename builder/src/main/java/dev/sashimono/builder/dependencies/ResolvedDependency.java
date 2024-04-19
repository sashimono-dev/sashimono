package dev.sashimono.builder.dependencies;

import java.nio.file.Path;
import java.util.Optional;

import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.Repository;

public record ResolvedDependency(Dependency dependency, Path path, Optional<Repository> repository) {

}
