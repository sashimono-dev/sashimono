package dev.sashimono.builder.dependencies;

import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.config.Repository;
import dev.sashimono.builder.config.RepositoryConfig;
import dev.sashimono.builder.util.HashUtil;
import dev.sashimono.builder.util.TaskMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.util.List;

public class DownloadDependencyTaskTestCase {

    private static final Dependency GIZMO = new Dependency(new GAV("io.quarkus.gizmo", "gizmo", "1.8.0"), "jar");
    private static final RepositoryConfig CENTRAL = new RepositoryConfig(List.of(new Repository("central", "https://repo1.maven.org/maven2")));

    private final String MD5 = "d1d3f94435694e58b91ecd1a84f5793b";

    @Test
    public void testDownloadDependency() throws Exception {
       var task =  new DownloadDependencyTask(GIZMO, CENTRAL, HttpClient.newBuilder().build());
       var result = task.apply(new TaskMap(List.of()));
        Assertions.assertTrue(Files.exists(result.path()));
        try (InputStream inputStream = Files.newInputStream(result.path())) {
            Assertions.assertEquals(HashUtil.md5(inputStream), MD5);
        }
    }
    @Test
    public void testMissingDep() throws Exception {
        var task =  new DownloadDependencyTask(new Dependency(new GAV("io.quarkus.fake", "fake", "1.8.0"), "jar"), CENTRAL, HttpClient.newBuilder().build());
        Assertions.assertThrows(RuntimeException.class, () -> {
            task.apply(new TaskMap(List.of()));
        });
    }

}
