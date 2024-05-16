package dev.sashimono.builder.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The configuration for a module in a project
 *
 * @param gav The project GAV
 * @param packaging The type of project, currently only jar is supported
 * @param dependencies The project dependencies
 * @param sourceDirectories The source directories to compile
 * @param filteredResourcesDir Optional directory containing non .class resources prefiltered by Maven
 * @param pomPath The path of the module POM file
 * @param manifestEntries The manifest entries associated with the jar
 * @param compilerArguments The arguments used when compiling the jar
 */
public record ModuleConfig(GAV gav, String packaging, List<Dependency> dependencies, List<Path> sourceDirectories,
        Path filteredResourcesDir, Path pomPath, Map<String, String> manifestEntries, List<String> compilerArguments) {
}
