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
import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.config.ProjectConfig;
import dev.sashimono.builder.config.Repository;
import dev.sashimono.builder.config.RepositoryConfig;
import dev.sashimono.builder.dependencies.DownloadDependencyTask;
import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.jar.DigestTask;
import dev.sashimono.builder.jar.FileOutput;
import dev.sashimono.builder.jar.JarResult;
import dev.sashimono.builder.jar.JarTask;
import dev.sashimono.builder.jar.PomTask;
import dev.sashimono.builder.jar.SourcesJarTask;
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
        Map<Dependency, Task<?>> depTasks = new HashMap<>();

        Map<GAV, Task<JarResult>> jarTasks = new HashMap<>();
        runner.addResultMapper(JarResult.class, JarResult.RESOLVED_DEPENDENCY_MAPPER);
        runner.addResultMapper(JarResult.class, JarResult.FILE_OUTPUT_MAPPER);
        Task<Void> digestTask = runner.newTask(Void.class, new DigestTask());
        Map<GAV, Task<FileOutput>> pomTasks = new HashMap<>();
        Map<GAV, Task<FileOutput>> sourcesJarTasks = new HashMap<>();
        //first we need to figure out what we are building locally, so we don't try and download it
        for (var m : config.moduleConfigs()) {
            if (m.packaging().equals(JAR)) {
                Task<JarResult> jarTask = runner.newTask(JarResult.class,
                        new JarTask(outputDir, m.gav(), m.filteredResourcesDir(), m.manifestEntries()));
                jarTasks.put(m.gav(), jarTask);
                depTasks.put(new Dependency(m.gav(), JAR), jarTask);
                Task<FileOutput> pomTask = runner.newTask(FileOutput.class,
                        new PomTask(outputDir, m.gav(), m.pomPath()));
                pomTasks.put(m.gav(), pomTask);
                Task<FileOutput> sourcesJarTask = runner.newTask(FileOutput.class,
                        new SourcesJarTask(outputDir, m.gav(), m.filteredResourcesDir(), m.sourceDirectories()));
                sourcesJarTasks.put(m.gav(), sourcesJarTask);
            }
        }
        for (var m : config.moduleConfigs()) {
            List<Task<?>> moduleDependencies = new ArrayList<>();
            //download dependencies
            for (var i : m.dependencies()) {

                Task<?> downloadTask;
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
                digestTask.addDependency(jarTask);
                Task<FileOutput> pomTask = pomTasks.get(m.gav());
                digestTask.addDependency(pomTask);
                Task<FileOutput> sourcesJarTask = sourcesJarTasks.get(m.gav());
                digestTask.addDependency(sourcesJarTask);
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
