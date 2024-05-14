package dev.sashimono.builder.jar;

import static java.lang.Boolean.getBoolean;
import static java.net.http.HttpRequest.BodyPublishers.ofFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import dev.sashimono.builder.util.FileUtil;
import dev.sashimono.builder.util.Log;
import dev.sashimono.builder.util.TaskMap;

public class DeployTask implements Function<TaskMap, Void> {

    private static final Log log = Log.of(DeployTask.class);
    public static final String DEPLOY_ARTIFACTS = "deploy_artifacts";
    private final Path outputDir;
    private final HttpClient client;

    public DeployTask(final Path outputDir, final HttpClient client) {
        this.outputDir = outputDir;
        this.client = client;
    }

    @Override
    public Void apply(final TaskMap taskMap) {
        // TODO Support staging repository deployment and generation of maven-metadata.xml
        if (getBoolean(DEPLOY_ARTIFACTS)) {
            final String url = System.getenv("REPOSITORY_URL");
            final String username = System.getenv("REPOSITORY_USERNAME");
            final String password = System.getenv("REPOSITORY_PASSWORD");
            final List<Path> files = FileUtil.collectFiles(outputDir);
            for (final Path file : files) {
                try {
                    final String fullUri = url + "/" + outputDir.relativize(file);
                    final HttpResponse<String> response = client.send(
                            HttpRequest.newBuilder().PUT(ofFile(file)).uri(new URI(fullUri)).header("Authorization",
                                    "Basic " + Base64.getEncoder()
                                            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)))
                                    .build(),
                            BodyHandlers.ofString());
                    log.info(response.toString());
                } catch (final IOException | URISyntaxException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }
}
