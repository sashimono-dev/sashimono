package dev.sashimono.builder.config;

/**
 * A remote repository. Supports http://, https:// and file:// urls.
 *
 * @param name The repository name
 * @param url The repository url
 */
public record Repository(String name, String url) {
}
