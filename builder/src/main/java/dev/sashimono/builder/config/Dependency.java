package dev.sashimono.builder.config;

/**
 * A project dependency, basically an artifact that needs to be downloaded.
 *
 * @param GAV
 * @param type
 */
public record Dependency(GAV GAV, String type) {
}
