package dev.sashimono.builder.config;

import java.util.List;

/**
 * A configured list of repositories
 *
 * @param repositories The repositories
 */
public record RepositoryConfig(List<Repository> repositories) {

}
