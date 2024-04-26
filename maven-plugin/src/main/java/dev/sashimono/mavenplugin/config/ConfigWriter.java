package dev.sashimono.mavenplugin.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

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

    public static void writeConfig(final MavenProject project, final boolean resourcesCopied) {
        final Path baseDirPath = project.getBasedir().toPath();
        final Path dirPath = baseDirPath.resolve(SASHIMONO_DIR);
        final Path filePath = dirPath.resolve(DEPENDENCIES_LIST);
        try {
            // Make sure directories already exist
            Files.createDirectories(dirPath);
            try (final BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                // Write artifact details
                writer.write(ARTIFACT + project.getGroupId() + DELIMITER + project.getArtifactId() + DELIMITER
                        + project.getVersion() + System.lineSeparator());
                // Write package details
                writer.write(PACKAGING + project.getPackaging() + System.lineSeparator());
                for (final String module : project.getModules()) {
                    writer.write(MODULE + module + System.lineSeparator());
                }
                for (final Dependency dependency : project.getDependencies()) {
                    // We only care about compile and provided dependencies
                    if (SCOPES.contains(dependency.getScope())) {
                        // Write dependency details
                        writer.write(REQUIRE + dependency.getGroupId() + DELIMITER + dependency.getArtifactId() + DELIMITER
                                + dependency.getVersion() + System.lineSeparator());
                    }
                }
                writer.write(FILTERED_RESOURCES + resourcesCopied + System.lineSeparator());
                for (final String srcPath : project.getCompileSourceRoots()) {
                    writer.write(SOURCE + baseDirPath.relativize(Path.of(srcPath)) + System.lineSeparator());
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
