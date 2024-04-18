package dev.sashimono.builder;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.sashimono.builder.compiler.JavaCompilerTask;
import dev.sashimono.builder.config.ConfigReader;
import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.ProjectConfig;
import dev.sashimono.builder.config.Repository;
import dev.sashimono.builder.config.RepositoryConfig;
import dev.sashimono.builder.dependencies.DownloadDependencyTask;
import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.util.Log;
import dev.sashimono.builder.util.Task;
import dev.sashimono.builder.util.TaskRunner;

/**
 * The entry point for the build tool.
 * <p>
 * By design this tool only accepts the path of a project to build,
 * in the interests of reproducibility all config needs to be specified
 * in the project itself rather than on the command line.
 * <p>
 * Note that some debug options may be exposed through system properties,
 * however this should not affect the actual results of the execution in any way.
 */
public class Main {
    private static final RepositoryConfig CENTRAL = new RepositoryConfig(
            List.of(new Repository("central", "https://repo1.maven.org/maven2")));

    public static void main(String... args) {
        if (args.length != 1) {
            Log.of(Main.class).error("Usage: java -jar sashimono.jar <project-path>");
            System.exit(1);
        }

        ProjectConfig config = ConfigReader.readConfig(Path.of(args[0]));
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
            Task<Path> compileTask = runner.newTask(Path.class, new JavaCompilerTask(m.sourceDirectories()));
            for (var i : moduleDependencies) {
                compileTask.addDependency(i);
            }
        }
        runner.run();

    }

}
