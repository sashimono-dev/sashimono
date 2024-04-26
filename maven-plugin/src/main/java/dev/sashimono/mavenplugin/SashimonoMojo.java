package dev.sashimono.mavenplugin;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import dev.sashimono.mavenplugin.config.ConfigWriter;
import dev.sashimono.mavenplugin.copy.ResourceCopier;

@Mojo(name = "sashimono", defaultPhase = LifecyclePhase.COMPILE)
public class SashimonoMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        final boolean resourcesCopied = ResourceCopier.copyResources(project, outputDirectory);
        ConfigWriter.writeConfig(project, resourcesCopied);
    }

}
