package dev.sashimono.mavenplugin;

import dev.sashimono.mavenplugin.config.ConfigWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "sashimono", defaultPhase = LifecyclePhase.COMPILE)
public class SashimonoMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        ConfigWriter.writeConfig(project);
    }

}
