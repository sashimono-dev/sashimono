package dev.sashimono.config;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.ModuleConfig;

/**
 * Writes a project config to a .sashimono directory
 */
public class ConfigWriter {

    public static final String SASHIMONO_DIR = ".sashimono";
    public static final String DEPENDENCIES_LIST = "dependencies.list";
    public static final String REQUIRE = "require ";
    public static final List<String> SCOPES = List.of("compile", "provided");
    public static final char DELIMITER = ':';
    public static final String ARTIFACT = "artifact ";
    public static final String PACKAGING = "packaging ";
    public static final String MODULE = "module ";
    public static final String FILTERED_RESOURCES = "filtered_resources ";
    public static final String SOURCE = "source ";
    public static final String POM = "pom ";
    public static final String MANIFEST_ENTRY = "manifest_entry ";

    public static void writeConfig(final Path baseDirPath, final ModuleConfig moduleConfig, List<String> submodules) {
        final Path dirPath = baseDirPath.resolve(SASHIMONO_DIR);
        final Path filePath = dirPath.resolve(DEPENDENCIES_LIST);
        try {
            // Make sure directories already exist
            Files.createDirectories(dirPath);
            try (final BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                // Write artifact details
                writer.write(ARTIFACT + moduleConfig.gav().group() + DELIMITER + moduleConfig.gav().artifact() + DELIMITER
                        + moduleConfig.gav().version() + System.lineSeparator());
                // Write package details
                writer.write(PACKAGING + moduleConfig.packaging() + System.lineSeparator());
                for (final String module : submodules) {
                    writer.write(MODULE + module + System.lineSeparator());
                }

                final List<Dependency> dependencies = moduleConfig.dependencies();
                for (final var dependency : dependencies) {
                    // We only care about compile and provided dependencies
                    // Write dependency details
                    writer.write(REQUIRE + dependency.GAV().group() + DELIMITER
                            + dependency.GAV().artifact() + DELIMITER
                            + dependency.GAV().version() + System.lineSeparator());
                }
                writer.write(FILTERED_RESOURCES + (moduleConfig.filteredResourcesDir() != null) + System.lineSeparator());
                for (final Path srcPath : moduleConfig.sourceDirectories()) {
                    writer.write(SOURCE + baseDirPath.relativize(srcPath) + System.lineSeparator());
                }
                writer.write(POM + baseDirPath.relativize(moduleConfig.pomPath()) + System.lineSeparator());
                for (final Map.Entry<String, String> entry : moduleConfig.manifestEntries().entrySet()) {
                    writer.write(MANIFEST_ENTRY + entry.getKey() + DELIMITER + entry.getValue() + System.lineSeparator());
                }
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
