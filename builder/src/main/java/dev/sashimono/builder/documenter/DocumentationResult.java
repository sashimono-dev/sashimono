package dev.sashimono.builder.documenter;

import java.nio.file.Path;

/**
 * The results of documenting, basically just a temp directory with javadoc files
 *
 * @param documentationDirectory
 */
public record DocumentationResult(Path documentationDirectory) {
}
