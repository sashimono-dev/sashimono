package dev.sashimono.mavenplugin.config;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes a project config to a .sashimono directory
 */
public class ConfigWriter {

    public static final String SASHIMONO_DIR = ".sashimono";
    public static final String DEPENDENCIES_LIST = "dependencies.list";
    public static final String REQUIRE_ = "require ";
    public static final String SCOPE = "compile";
    public static final char DELIMITER = ':';
    public static final String ARTIFACT_ = "artifact ";
    public static final String PACKAGING_ = "packaging ";
    public static final String MODULE_ = "module ";

    public static void writeConfig(final MavenProject project) {
        final Path dirPath = project.getBasedir().toPath().resolve(SASHIMONO_DIR);
        final Path filePath = dirPath.resolve(DEPENDENCIES_LIST);
        try {
            // Make sure directories already exist
            Files.createDirectories(dirPath);
            try (final BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                // Write artifact details
                writer.write(ARTIFACT_ + project.getGroupId() + DELIMITER + project.getArtifactId() + DELIMITER
                        + project.getVersion() + System.lineSeparator());
                // Write package details
                writer.write(PACKAGING_ + project.getPackaging() + System.lineSeparator());
                for (final String module : project.getModules()) {
                    writer.write(MODULE_ + module + System.lineSeparator());
                }
                for (final Dependency dependency : project.getDependencies()) {
                    // We only care about compile dependencies
                    if (dependency.getScope().equals(SCOPE)) {
                        // Write dependency details
                        writer.write(REQUIRE_ + dependency.getGroupId() + DELIMITER + dependency.getArtifactId() + DELIMITER
                                + dependency.getVersion() + System.lineSeparator());
                    }
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
