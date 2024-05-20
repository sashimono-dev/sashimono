package dev.sashimono.gradle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskAction;

import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.config.ModuleConfig;
import dev.sashimono.config.ConfigWriter;

public class GenerateTask extends DefaultTask {

    @TaskAction
    public void generate() {
        List<Dependency> deps = new ArrayList<>();
        for (var i : getProject().getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
                .getResolvedConfiguration().getResolvedArtifacts()) {
            deps.add(new Dependency(
                    new GAV(i.getModuleVersion().getId().getGroup(), i.getName(), i.getModuleVersion().getId().getVersion()),
                    i.getType()));
        }
        ModuleConfig moduleConfig = new ModuleConfig(
                new GAV(getProject().getGroup().toString(), getProject().getName(), getProject().getVersion().toString()),
                "jar",
                deps,
                List.of(),
                null,
                getProject().getRootDir().toPath(), //TODO
                Map.of(), List.of());
        ConfigWriter.writeConfig(getProject().getRootDir().toPath(), moduleConfig, List.of());
    }
}
