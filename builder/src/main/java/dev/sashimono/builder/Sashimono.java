package dev.sashimono.builder;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import dev.sashimono.builder.compiler.CompileResult;
import dev.sashimono.builder.compiler.JavaCompilerTask;
import dev.sashimono.builder.config.ConfigReader;
import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.GAV;
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
    public static final String JAR = "jar";
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

        Map<GAV, Task<JarResult>> jarTasks = new HashMap<>();
        //first we need to figure out what we are building locally, so we don't try and download it
        for (var m : config.moduleConfigs()) {
            if (m.packaging().equals(JAR)) {
                Task<JarResult> jarTask = runner.newTask(JarResult.class, new JarTask(outputDir, m.gav()));

                //this task allows us to treat compiled jar files the same as downloaded dependencies
                //it just maps between the two types
                Task<ResolvedDependency> jarAsDependency = runner.newTask(ResolvedDependency.class, t -> {
                    var jar = t.results(JarResult.class).get(0);
                    return new ResolvedDependency(jar.result().dependency(), jar.result().path(), Optional.empty());
                });
                jarAsDependency.addDependency(jarTask);
                jarTasks.put(m.gav(), jarTask);
                depTasks.put(new Dependency(m.gav(), JAR), jarAsDependency);
            }
        }
        for (var m : config.moduleConfigs()) {
            List<Task<?>> moduleDependencies = new ArrayList<>();
            //download dependencies
            for (var i : m.dependencies()) {

                Task<ResolvedDependency> downloadTask;
                if (depTasks.containsKey(i)) {
                    //already queued for download, or locally built. We don't want to download things twice
                    downloadTask = depTasks.get(i);
                } else {
                    //queue the dependency for download
                    //dependency downloads are 'background' tasks, as we always want to do compilation
                    //and actual built tasks if they are ready in preference
                    //we don't want to download a full projects dependencies before we start
                    //building anything
                    downloadTask = runner.newBackgroundTask(ResolvedDependency.class,
                            new DownloadDependencyTask(i, CENTRAL, httpClient));
                    depTasks.put(i, downloadTask);
                }
                moduleDependencies.add(downloadTask);

            }
            if (m.packaging().equals(JAR)) {
                Task<CompileResult> compileTask = runner.newTask(CompileResult.class,
                        new JavaCompilerTask(m.sourceDirectories()));
                for (var i : moduleDependencies) {
                    compileTask.addDependency(i);
                }
                Task<JarResult> jarTask = jarTasks.get(m.gav());
                jarTask.addDependency(compileTask);
            }
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
