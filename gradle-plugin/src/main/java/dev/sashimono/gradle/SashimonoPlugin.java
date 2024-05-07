package dev.sashimono.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

public class SashimonoPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.getTasks().register("generateBuildConfig", GenerateTask.class, (task) -> {
            task.dependsOn(target.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME));
        });

    }
}
