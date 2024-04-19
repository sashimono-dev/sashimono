package dev.sashimono.builder;

import java.nio.file.Path;

import dev.sashimono.builder.util.Log;

/**
 * The entry point for the build tool.
 * <p>
 * By design this tool accepts a limited number of parameters, anything actually affecting the build process needs
 * to be specified in the build config.
 * <p>
 * Note that some debug options may be exposed through system properties,
 * however this should not affect the actual results of the execution in any way.
 */
public class Main {

    public static void main(String... args) {
        if (args.length != 1) {
            Log.of(Main.class).error("Usage: java -jar sashimono.jar <project-path>");
            System.exit(1);
        }

        Path projectRoot = Path.of(args[0]);
        var sashimono = Sashimono.builder()
                .setProjectRoot(projectRoot)
                .build();

        sashimono.buildProject();

    }

}
