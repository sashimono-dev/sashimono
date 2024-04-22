package dev.sashimono.builder.test;

import java.nio.file.Path;

/**
 * The results of a build, currently just the output directory.
 *
 * @param output
 */
public record BuildResult(Path output) {

}
