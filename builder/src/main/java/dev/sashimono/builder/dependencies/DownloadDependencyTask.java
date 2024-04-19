package dev.sashimono.builder.dependencies;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;

import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.RepositoryConfig;
import dev.sashimono.builder.util.TaskMap;

/**
 * A task that can download a dependency from a remote repository, this also supports file urls
 * where it just accesses the file directly from the local repo.
 */
public class DownloadDependencyTask implements Function<TaskMap, ResolvedDependency> {

    final Dependency dependency;
    final RepositoryConfig repositoryConfig;

    final HttpClient client;

    public DownloadDependencyTask(Dependency dependency, RepositoryConfig repositoryConfig, HttpClient client) {
        this.dependency = dependency;
        this.repositoryConfig = repositoryConfig;
        this.client = client;
    }

    @Override
    public ResolvedDependency apply(TaskMap taskMap) {
        try {
            Path target = Files.createTempFile("sashimono", "dep" + "." + dependency.type());
            String localPart = dependency.GAV().group().replace(".", "/") + "/" + dependency.GAV().artifact() + "/"
                    + dependency.GAV().version() + "/" + dependency.GAV().artifact() + "-" + dependency.GAV().version() + "."
                    + dependency.type();
            for (var repo : repositoryConfig.repositories()) {
                Files.deleteIfExists(target);
                if (repo.url().startsWith("file://")) {
                    var path = Paths.get(repo.url().substring("file://".length()) + "/" + localPart);
                    if (Files.exists(path)) {
                        return new ResolvedDependency(dependency, path, Optional.of(repo));
                    }

                } else if (repo.url().startsWith("http")) {
                    //TODO: local repo support
                    String fullUri = repo.url() + "/" + localPart;
                    var result = client.send(HttpRequest.newBuilder().GET().uri(new URI(fullUri)).build(),
                            HttpResponse.BodyHandlers.ofFile(target));
                    if (result.statusCode() == 200) {
                        return new ResolvedDependency(dependency, target, Optional.of(repo));
                    }
                }
            }
            throw new RuntimeException("Unable to resolve " + dependency.GAV());
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
