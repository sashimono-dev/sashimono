package dev.sashimono.builder;

import java.nio.file.Path;

import dev.sashimono.builder.config.ConfigReader;
import dev.sashimono.builder.config.ProjectConfig;
import dev.sashimono.builder.util.Log;

/**
 * The entry point for the build tool.
 *
 * By design this tool only accepts the path of a project to build,
 * in the interests of reproducibility all config needs to be specified
 * in the project itself rather than on the command line.
 *
 * Note that some debug options may be exposed through system properties,
 * however this should not affect the actual results of the execution in any way.
 */
public class Main {

    public static void main(String... args) {
        if (args.length != 1) {
            Log.of(Main.class).error("Usage: java -jar sashimono.jar <project-path>");
            System.exit(1);
        }

        ProjectConfig config = ConfigReader.readConfig(Path.of(args[0]));

    }

}
