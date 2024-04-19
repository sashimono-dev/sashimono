package dev.sashimono.builder;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.sashimono.builder.compiler.CompileResult;
import dev.sashimono.builder.compiler.JavaCompilerTask;
import dev.sashimono.builder.config.ConfigReader;
import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.ProjectConfig;
import dev.sashimono.builder.config.Repository;
import dev.sashimono.builder.config.RepositoryConfig;
import dev.sashimono.builder.dependencies.DownloadDependencyTask;
import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.jar.JarResult;
import dev.sashimono.builder.jar.JarTask;
import dev.sashimono.builder.util.Task;
import dev.sashimono.builder.util.TaskRunner;

public class Sashimono {

    private static final RepositoryConfig CENTRAL = new RepositoryConfig(
            List.of(new Repository("central", "https://repo1.maven.org/maven2")));
    final Path projectRoot;
    final Path outputDir;

    Sashimono(Builder builder) {
        this.projectRoot = Objects.requireNonNull(builder.projectRoot);
        this.outputDir = builder.outputDir != null ? builder.outputDir : projectRoot.resolve("output");
    }

    public void buildProject() {
        ProjectConfig config = ConfigReader.readConfig(projectRoot);
        TaskRunner runner = new TaskRunner();
        HttpClient httpClient = HttpClient.newHttpClient();
        Map<Dependency, Task<ResolvedDependency>> depTasks = new HashMap<>();

        for (var m : config.moduleConfigs()) {
            List<Task<ResolvedDependency>> moduleDependencies = new ArrayList<>();
            for (var i : m.dependencies()) {
                Task<ResolvedDependency> downloadTask;
                if (depTasks.containsKey(i)) {
                    downloadTask = depTasks.get(i);
                } else {
                    downloadTask = runner.newTask(ResolvedDependency.class,
                            new DownloadDependencyTask(i, CENTRAL, httpClient));
                    depTasks.put(i, downloadTask);
                }
                moduleDependencies.add(downloadTask);
            }
            Task<CompileResult> compileTask = runner.newTask(CompileResult.class, new JavaCompilerTask(m.sourceDirectories()));
            for (var i : moduleDependencies) {
                compileTask.addDependency(i);
            }
            Task<JarResult> jarTask = runner.newTask(JarResult.class, new JarTask(outputDir, m.gav()));
            jarTask.addDependency(compileTask);
        }
        runner.run();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        Path projectRoot;
        Path outputDir;

        public Path getProjectRoot() {
            return projectRoot;
        }

        public Builder setProjectRoot(Path projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }

        public Path getOutputDir() {
            return outputDir;
        }

        public Builder setOutputDir(Path outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        public Sashimono build() {
            return new Sashimono(this);
        }
    }
}
