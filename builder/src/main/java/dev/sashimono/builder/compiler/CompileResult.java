package dev.sashimono.builder.compiler;

import java.nio.file.Path;

/**
 * The results of a compilation, basically just a temp directory with class files
 *
 * @param classesDirectory
 */
public record CompileResult(Path classesDirectory) {
}
