package config;

import static dev.sashimono.builder.config.ConfigReader.SASHIMONO_DIR;
import static dev.sashimono.mavenplugin.copy.ResourceCopier.RESOURCES_DIR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.sashimono.mavenplugin.copy.ResourceCopier;

public class ResourceCopierTestCase {

    @Test
    public void testResourceCopier(@TempDir final File tempDir) throws IOException {
        final Model model = new Model();
        final Build build = new Build();
        final File buildDir = new File("src/test/resources/build");
        build.setDirectory(buildDir.getAbsolutePath());
        final Resource resources = new Resource();
        build.setResources(List.of(resources));
        model.setBuild(build);
        final MavenProject project = new MavenProject(model);
        project.setFile(tempDir.toPath().resolve("pom.xml").toFile());
        ResourceCopier.copyResources(project, buildDir);
        final Path resourcesPath = tempDir.toPath().resolve(SASHIMONO_DIR).resolve(RESOURCES_DIR);
        final String fileContents = Files
                .readString(resourcesPath.resolve("config/application.properties"));
        final String expected = """
                greeting.message = hello
                greeting.name = quarkus"""
                .replaceAll("\n", System.lineSeparator());
        Assertions.assertEquals(expected, fileContents);
        Assertions.assertFalse(resourcesPath.resolve("Foo.class").toFile().exists());
    }

}
