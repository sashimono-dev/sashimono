package dev.sashimono.builder.jar;

import dev.sashimono.builder.dependencies.ResolvedDependency;

/**
 * The result of creating a jar file
 *
 * @param result The path to the jar
 */
public record JarResult(ResolvedDependency result) {
}
