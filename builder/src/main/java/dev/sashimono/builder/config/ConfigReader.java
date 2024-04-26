package dev.sashimono.builder.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Reads a project config from a .sashimono directory
 *
 * This format is very much subject to change.
 */
public class ConfigReader {

    public static final String SASHIMONO_DIR = ".sashimono";
    public static final String DEPENDENCIES_LIST = "dependencies.list";
    public static final String REQUIRE = "require ";
    public static final String ARTIFACT = "artifact ";
    public static final String MODULE = "module ";
    public static final String PACKAGING = "packaging ";
    public static final String JAR = "jar";
    public static final String FILTERED_RESOURCES = "filtered_resources ";
    public static final String RESOURCES_DIR = "resources";
    public static final String SOURCE = "source ";

    public static ProjectConfig readConfig(Path root) {

        Set<Path> processed = new HashSet<>();
        Set<Path> toProcess = new HashSet<>();
        toProcess.add(root);
        List<ModuleConfig> moduleConfigs = new ArrayList<>();
        Path filteredResourcesDir = null;
        try {
            while (!toProcess.isEmpty()) {
                Iterator<Path> iterator = toProcess.iterator();
                var project = iterator.next();
                iterator.remove();
                if (processed.contains(project)) {
                    continue;
                }
                processed.add(project);
                var sashimonoDir = project.resolve(SASHIMONO_DIR);
                var result = sashimonoDir.resolve(DEPENDENCIES_LIST);
                var lines = Files.readAllLines(result);
                //TODO: this is all a bit hacky
                List<Dependency> deps = new ArrayList<>();
                List<Path> sourceDirs = new ArrayList<>();
                GAV gav = null;
                String packaging = null;
                var lineNo = 0;
                for (var i : lines) {
                    try {
                        lineNo++;
                        if (i.contains("#")) {
                            i = i.substring(0, i.indexOf("#")).trim();
                        }
                        if (i.trim().isBlank()) {
                            continue;
                        }
                        if (i.startsWith(REQUIRE)) {
                            var dep = i.substring(REQUIRE.length());
                            var parts = dep.split(":");
                            // TODO: error handling and reporting
                            deps.add(new Dependency(new GAV(parts[0], parts[1], parts[2]), JAR));
                        } else if (i.startsWith(ARTIFACT)) {
                            if (gav != null) {
                                throw new RuntimeException("artifact directive specified twice");
                            }
                            var dep = i.substring(ARTIFACT.length());
                            var parts = dep.split(":");
                            gav = new GAV(parts[0], parts[1], parts[2]);
                        } else if (i.startsWith(MODULE)) {
                            var module = i.substring(MODULE.length());
                            Path subModule = project.resolve(module);
                            toProcess.add(subModule);
                        } else if (i.startsWith(PACKAGING)) {
                            if (packaging != null) {
                                throw new RuntimeException("packaging directive specified twice");
                            }
                            packaging = i.substring(PACKAGING.length());
                        } else if (i.startsWith(FILTERED_RESOURCES)) {
                            var val = i.substring(FILTERED_RESOURCES.length());
                            filteredResourcesDir = Boolean.parseBoolean(val) ? sashimonoDir.resolve(RESOURCES_DIR) : null;
                        } else if (i.startsWith(SOURCE)) {
                            var val = i.substring(SOURCE.length());
                            sourceDirs.add(project.resolve(val));
                        } else {
                            throw new RuntimeException("error parsing dependencies file: " + i);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse " + project + " on line " + lineNo);
                    }
                }
                if (gav == null) {
                    throw new RuntimeException("artifact directive not specified");
                }
                // Root will also get added as a module here
                moduleConfigs.add(new ModuleConfig(gav, packaging, deps, sourceDirs));
            }
            return new ProjectConfig(root, moduleConfigs, filteredResourcesDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
