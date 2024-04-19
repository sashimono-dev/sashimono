package dev.sashimono.builder.config;

import java.nio.file.Path;
import java.util.List;

public record ModuleConfig(GAV gav, List<Dependency> dependencies, List<Path> sourceDirectories) {
}
