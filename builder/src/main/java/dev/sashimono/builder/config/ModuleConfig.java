package dev.sashimono.builder.config;

import java.nio.file.Path;
import java.util.List;

/**
 * The configuration for a module in a project
 *
 * @param gav The project GAV
 * @param packaging The type of project, currently only jar is supported
 * @param dependencies The project dependencies
 * @param sourceDirectories The source directories to compile
 */
public record ModuleConfig(GAV gav, String packaging, List<Dependency> dependencies, List<Path> sourceDirectories) {
}
