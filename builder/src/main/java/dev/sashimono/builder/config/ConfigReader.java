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
    public static final String ARTIFACT_ = "artifact ";
    public static final String JAR = "jar";

    public static ProjectConfig readConfig(Path projectRoot) {
        var result = projectRoot.resolve(SASHIMONO_DIR).resolve(DEPENDENCIES_LIST);
        try {
            var lines = Files.readAllLines(result);
            List<Dependency> deps = new ArrayList<>();
            GAV gav = null;
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
                } else if (i.startsWith(ARTIFACT_)) {
                    if (gav != null) {
                        throw new RuntimeException("artifact directive specified twice"); // TODO: report line number etc
                    }
                    var dep = i.substring(ARTIFACT_.length());
                    var parts = dep.split(":");
                    gav = new GAV(parts[0], parts[1], parts[2]);
                } else {
                    throw new RuntimeException("error parsing dependencies file: " + i); // TODO: report line number etc
                }
            }
            if (gav == null) {
                throw new RuntimeException("artifact directive not specified");
            }
            return new ProjectConfig(projectRoot,
                    List.of(new ModuleConfig(gav, deps, List.of(projectRoot.resolve("src/main/java")))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
