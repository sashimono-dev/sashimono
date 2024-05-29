package dev.sashimono.mavenplugin.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.config.ModuleConfig;
import dev.sashimono.config.ConfigWriter;

/**
 * Writes a project config to a .sashimono directory
 */
public class MavenConfigWriter {

    public static final List<String> SCOPES = List.of("compile", "provided");
    public static final String MAVEN_JAR_PLUGIN = "maven-jar-plugin";
    public static final String MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin";

    public static void writeConfig(final MavenProject project, final boolean resourcesCopied,
            final Supplier<List<Dependency>> dependencySupplier) {
        final Path baseDirPath = project.getBasedir().toPath();
        try {
            final GAV gav = new GAV(project.getGroupId(), project.getArtifactId(), project.getVersion());

            final List<dev.sashimono.builder.config.Dependency> sashimonoDeps = new ArrayList<>();
            final List<Dependency> dependencies = dependencySupplier.get();
            for (final var dependency : dependencies) {
                // We only care about compile and provided dependencies
                if (SCOPES.contains(dependency.getScope())) {
                    final Artifact artifact = dependency.getArtifact();
                    // Write dependency details
                    sashimonoDeps.add(new dev.sashimono.builder.config.Dependency(new GAV(artifact.getGroupId(),
                            artifact.getArtifactId(), artifact.getVersion()),
                            artifact.getExtension(), artifact.getClassifier()));
                }
            }
            final List<Path> sourceDirectories = new ArrayList<>();

            for (final String srcPath : project.getCompileSourceRoots()) {
                sourceDirectories.add(Path.of(srcPath));
            }
            findCompilerArguments(project);
            final ModuleConfig moduleConfig = new ModuleConfig(gav,
                    project.getPackaging(), project.getArtifact().getClassifier(), sashimonoDeps, sourceDirectories,
                    resourcesCopied ? Path.of("tmp") : null, project.getFile().toPath(), findManifestEntries(project),
                    findCompilerArguments(project));
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
                Optional.ofNullable(dom).map(d -> d.getChild("archive")).map(a -> a.getChild("manifest"))
                        .ifPresent(c -> Arrays.stream(c.getChildren())
                                .forEach(e -> manifestEntries.put(e.getName(), e.getValue())));
            }
        }
        return manifestEntries;
    }

    public static List<String> findCompilerArguments(final MavenProject project) {
        final List<String> args = new ArrayList<>();
        for (final Plugin plugin : project.getModel().getBuild().getPlugins()) {
            if (plugin.getArtifactId().equals(MAVEN_COMPILER_PLUGIN)) {
                final Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
                Optional.ofNullable(dom).map(d -> d.getChild("source")).ifPresent(a -> {
                    args.add("-source");
                    args.add(a.getValue());
                });
                Optional.ofNullable(dom).map(d -> d.getChild("target")).ifPresent(a -> {
                    args.add("-target");
                    args.add(a.getValue());
                });
                Optional.ofNullable(dom).map(d -> d.getChild("compilerArgs"))
                        .ifPresent(c -> Arrays.stream(c.getChildren()).forEach(a -> args.add(a.getValue())));
                Optional.ofNullable(dom).map(d -> d.getChild("compilerArgument")).ifPresent(a -> args.add(a.getValue()));
            }
        }
        return args;
    }

}
