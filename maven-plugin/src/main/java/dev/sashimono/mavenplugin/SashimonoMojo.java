package dev.sashimono.mavenplugin;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import dev.sashimono.mavenplugin.config.ConfigWriter;
import dev.sashimono.mavenplugin.copy.ResourceCopier;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE)
public class SashimonoMojo extends AbstractMojo {

    private static final Set<String> IGNORED_PHASES = Set.of(
            "pre-clean", "clean", "post-clean");

    private static final List<String> MAVEN_PLUGIN_REQUIRED_PHASES = List.of(
            "validate",
            "initialize",
            "generate-sources",
            "process-sources",
            "generate-resources",
            "process-resources",
            "compile",
            "process-classes");
    public static final String MAVEN_PROJECT = "maven-plugin";
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    MojoExecution mojoExecution;

    @Override
    public void execute() throws MojoExecutionException {
        if (Objects.equals(MAVEN_PROJECT, project.getPackaging())) {
            ensurePluginMojoIsGenerated();
            project.setPackaging("jar");
        }
        final boolean resourcesCopied = ResourceCopier.copyResources(project, outputDirectory);
        ConfigWriter.writeConfig(project, resourcesCopied);
    }

    /**
     * Makes sure that required phases have been run to generate the plugin descriptor.
     */
    private void ensurePluginMojoIsGenerated() {

        List<String> goals = session.getGoals();
        // check for default goal(s) if none were specified explicitly,
        // see also org.apache.maven.lifecycle.internal.DefaultLifecycleTaskSegmentCalculator
        if (goals.isEmpty() && !StringUtils.isEmpty(project.getDefaultGoal())) {
            goals = List.of(StringUtils.split(project.getDefaultGoal()));
        }
        final String currentGoal = getCurrentGoal();

        int latestHandledPhaseIndex = -1;
        for (String goal : goals) {
            if (goal.endsWith(currentGoal)) {
                break;
            }
            if (goal.indexOf(':') >= 0 || IGNORED_PHASES.contains(goal)) {
                continue;
            }
            var i = MAVEN_PLUGIN_REQUIRED_PHASES.indexOf(goal);
            if (i < 0 || i == MAVEN_PLUGIN_REQUIRED_PHASES.size() - 1) {
                // all the necessary goals have already been executed
                return;
            }
            if (i > latestHandledPhaseIndex) {
                latestHandledPhaseIndex = i;
            }
        }
        throw new RuntimeException(
                "This project contains a maven-project. Please run 'mvn process-classes sashimono:generate' to ensure all required files are generated");
    }

    private String getCurrentGoal() {
        return mojoExecution.getMojoDescriptor().getPluginDescriptor().getGoalPrefix() + ":"
                + mojoExecution.getGoal();
    }

}
