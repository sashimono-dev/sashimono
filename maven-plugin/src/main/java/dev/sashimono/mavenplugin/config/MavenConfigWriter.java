package dev.sashimono.mavenplugin.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.graph.Dependency;

import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.config.ModuleConfig;
import dev.sashimono.config.ConfigWriter;

/**
 * Writes a project config to a .sashimono directory
 */
public class MavenConfigWriter {

    public static final String SASHIMONO_DIR = ".sashimono";
    public static final String DEPENDENCIES_LIST = "dependencies.list";
    public static final List<String> SCOPES = List.of("compile", "provided");
    public static final String MAVEN_JAR_PLUGIN = "maven-jar-plugin";

    public static void writeConfig(final MavenProject project, final boolean resourcesCopied,
            final Supplier<List<Dependency>> dependencySupplier) {
        final Path baseDirPath = project.getBasedir().toPath();
        try {
            GAV gav = new GAV(project.getGroupId(), project.getArtifactId(), project.getVersion());

            final List<dev.sashimono.builder.config.Dependency> sashimonoDeps = new ArrayList<>();
            final List<Dependency> dependencies = dependencySupplier.get();
            for (final var dependency : dependencies) {
                // We only care about compile and provided dependencies
                if (SCOPES.contains(dependency.getScope())) {
                    // Write dependency details
                    sashimonoDeps.add(new dev.sashimono.builder.config.Dependency(new GAV(dependency.getArtifact().getGroupId(),
                            dependency.getArtifact().getArtifactId(), dependency.getArtifact().getVersion()),
                            dependency.getArtifact().getExtension()));
                }
            }
            List<Path> sourceDirectories = new ArrayList<>();

            for (final String srcPath : project.getCompileSourceRoots()) {
                sourceDirectories.add(Path.of(srcPath));
            }
            ModuleConfig moduleConfig = new ModuleConfig(gav, project.getPackaging(), sashimonoDeps, sourceDirectories,
                    resourcesCopied ? Path.of("tmp") : null, project.getFile().toPath(), findManifestEntries(project));
            ConfigWriter.writeConfig(baseDirPath, moduleConfig, project.getModules());

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
