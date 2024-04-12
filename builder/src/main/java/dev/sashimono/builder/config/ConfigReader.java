package dev.sashimono.builder.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a project config from a .sashimono directory
 */
public class ConfigReader {

    public static final String SASHIMONO_DIR = ".sashimono";
    public static final String DEPENDENCIES_LIST = "dependencies.list";
    public static final String REQUIRE_ = "require ";
    public static final String JAR = "jar";

    public static ProjectConfig readConfig(Path projectRoot) {
        var result = projectRoot.resolve(SASHIMONO_DIR).resolve(DEPENDENCIES_LIST);
        try {
            var lines = Files.readAllLines(result);
            List<Dependency> deps = new ArrayList<>();
            for (var i : lines) {
                if (i.contains("#")) {
                    i = i.substring(0, i.indexOf("#")).trim();
                }
                if (i.trim().isBlank()) {
                    continue;
                }
                if (i.startsWith(REQUIRE_)) {
                    var dep = i.substring(REQUIRE_.length());
                    var parts = dep.split(":");
                    // TODO: error handling and reporting
                    deps.add(new Dependency(new GAV(parts[0], parts[1], parts[2]), JAR));
                } else {
                    throw new RuntimeException("error parsing dependencies file"); // TODO: report line number etc
                }
            }
            return new ProjectConfig(projectRoot, List.of(new ModuleConfig(deps)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
