package dev.sashimono.builder.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
    public static final String POM = "pom ";
    public static final String MANIFEST_ENTRY = "manifest_entry ";
    public static final String DELIMITER = ":";
    public static final String COMPILER_ARGUMENT = "compiler_argument ";

    public static ProjectConfig readConfig(Path root) {

        Set<Path> processed = new HashSet<>();
        Set<Path> toProcess = new HashSet<>();
        toProcess.add(root);
        List<ModuleConfig> moduleConfigs = new ArrayList<>();
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
                String classifier = null;
                String packaging = null;
                Path filteredResourcesDir = null;
                Path pomPath = null;
                Map<String, String> manifestEntries = new TreeMap<>();
                List<String> compilerArguments = new ArrayList<>();
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
                            var parts = dep.split(DELIMITER);
                            var depClassifier = parts.length > 3 ? parts[3] : null;
                            // TODO: error handling and reporting
                            deps.add(new Dependency(new GAV(parts[0], parts[1], parts[2]), JAR, depClassifier));
                        } else if (i.startsWith(ARTIFACT)) {
                            if (gav != null) {
                                throw new RuntimeException("artifact directive specified twice");
                            }
                            var dep = i.substring(ARTIFACT.length());
                            var parts = dep.split(DELIMITER);
                            gav = new GAV(parts[0], parts[1], parts[2]);
                            classifier = parts.length > 3 ? parts[3] : null;
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
                        } else if (i.startsWith(POM)) {
                            var val = i.substring(POM.length());
                            pomPath = project.resolve(val);
                        } else if (i.startsWith(MANIFEST_ENTRY)) {
                            var manifestEntry = i.substring(MANIFEST_ENTRY.length());
                            var parts = manifestEntry.split(DELIMITER);
                            manifestEntries.put(parts[0], parts[1]);
                        } else if (i.startsWith(COMPILER_ARGUMENT)) {
                            var argument = i.substring(COMPILER_ARGUMENT.length());
                            compilerArguments.add(argument);
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
                moduleConfigs.add(
                        new ModuleConfig(gav, packaging, classifier, deps, sourceDirs, filteredResourcesDir, pomPath,
                                manifestEntries,
                                compilerArguments));
            }
            return new ProjectConfig(root, moduleConfigs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
