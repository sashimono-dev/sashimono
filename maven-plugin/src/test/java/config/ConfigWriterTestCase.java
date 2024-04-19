package config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.sashimono.mavenplugin.config.ConfigWriter;

public class ConfigWriterTestCase {

    @Test
    public void testMultiModuleConfigWriter(@TempDir final File tempDir) throws IOException {
        final Model model = new Model();
        model.setGroupId("com.acme");
        model.setArtifactId("parent");
        model.setVersion("1.0");
        model.setPackaging("pom");
        model.setModules(List.of("foo", "bar"));
        final MavenProject project = new MavenProject(model);
        project.setFile(tempDir.toPath().resolve("pom.xml").toFile());
        final Dependency dependency = new Dependency();
        dependency.setGroupId("org.apache.httpcomponents");
        dependency.setArtifactId("httpclient");
        dependency.setVersion("4.5.14");
        dependency.setScope("compile");
        project.setDependencies(List.of(dependency));
        ConfigWriter.writeConfig(project);
        final String fileContents = Files
                .readString(tempDir.toPath().resolve(ConfigWriter.SASHIMONO_DIR).resolve(ConfigWriter.DEPENDENCIES_LIST));
        Assertions.assertEquals(
                "artifact com.acme:parent:1.0\n" +
                        "packaging pom\n" +
                        "module foo\n" +
                        "module bar\n" +
                        "require org.apache.httpcomponents:httpclient:4.5.14\n",
                fileContents);
    }

    @Test
    public void testSingleModuleConfigWriter(@TempDir final File tempDir) throws IOException {
        final Model model = new Model();
        model.setGroupId("com.acme");
        model.setArtifactId("foo");
        model.setVersion("1.0");
        model.setPackaging("jar");
        final MavenProject project = new MavenProject(model);
        project.setFile(tempDir.toPath().resolve("pom.xml").toFile());
        final Dependency dependency = new Dependency();
        dependency.setGroupId("org.apache.httpcomponents");
        dependency.setArtifactId("httpclient");
        dependency.setVersion("4.5.14");
        dependency.setScope("compile");
        project.setDependencies(List.of(dependency));
        ConfigWriter.writeConfig(project);
        final String fileContents = Files
                .readString(tempDir.toPath().resolve(ConfigWriter.SASHIMONO_DIR).resolve(ConfigWriter.DEPENDENCIES_LIST));
        Assertions.assertEquals(
                "artifact com.acme:foo:1.0\n" +
                        "packaging jar\n" +
                        "require org.apache.httpcomponents:httpclient:4.5.14\n",
                fileContents);
    }

}
