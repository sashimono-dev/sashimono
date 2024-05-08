package dev.sashimono.builder.tool;

import java.nio.file.Path;
import java.util.List;

public abstract class AbstractJavaToolTask {

    protected final List<Path> sourceDirectories;

    protected AbstractJavaToolTask(final List<Path> sourceDirectories) {
        this.sourceDirectories = sourceDirectories;
    }
}
