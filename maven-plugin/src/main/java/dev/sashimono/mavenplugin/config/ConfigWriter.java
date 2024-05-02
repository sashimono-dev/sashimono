package dev.sashimono.mavenplugin.config;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.graph.Dependency;

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
    public static final String MAVEN_JAR_PLUGIN = "maven-jar-plugin";

    public static void writeConfig(final MavenProject project, final boolean resourcesCopied,
            final Supplier<List<Dependency>> dependencySupplier) {
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

                final List<Dependency> dependencies = dependencySupplier.get();
                for (final var dependency : dependencies) {
                    // We only care about compile and provided dependencies
                    if (SCOPES.contains(dependency.getScope())) {
                        // Write dependency details
                        writer.write(REQUIRE + dependency.getArtifact().getGroupId() + DELIMITER
                                + dependency.getArtifact().getArtifactId() + DELIMITER
                                + dependency.getArtifact().getVersion() + System.lineSeparator());
                    }
                }
                writer.write(FILTERED_RESOURCES + resourcesCopied + System.lineSeparator());
                for (final String srcPath : project.getCompileSourceRoots()) {
                    writer.write(SOURCE + baseDirPath.relativize(Path.of(srcPath)) + System.lineSeparator());
                }
                writer.write(POM + baseDirPath.relativize(project.getFile().toPath()) + System.lineSeparator());
                final Map<String, String> manifestEntries = findManifestEntries(project);
                for (final Map.Entry<String, String> entry : manifestEntries.entrySet()) {
                    writer.write(MANIFEST_ENTRY + entry.getKey() + DELIMITER + entry.getValue() + System.lineSeparator());
                }
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> findManifestEntries(final MavenProject project) {
        final Map<String, String> manifestEntries = new HashMap<>();
        for (final Plugin plugin : project.getModel().getBuild().getPlugins()) {
            if (plugin.getArtifactId().equals(MAVEN_JAR_PLUGIN)) {
                final Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
                for (final Xpp3Dom entry : Optional.ofNullable(dom).map(c -> c.getChild("archive"))
                        .map(c -> c.getChild("manifest")).map(Xpp3Dom::getChildren).orElse(new Xpp3Dom[0])) {
                    manifestEntries.put(entry.getName(), entry.getValue());
                }
            }
        }
        return manifestEntries;
    }

}
