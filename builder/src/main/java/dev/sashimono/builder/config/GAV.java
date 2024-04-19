package dev.sashimono.builder.config;

/**
 * A maven artifact identifier
 *
 * @param group The maven artifact group id
 * @param artifact The maven artifact identifier
 * @param version The artifact version
 */
public record GAV(String group, String artifact, String version) {
}
