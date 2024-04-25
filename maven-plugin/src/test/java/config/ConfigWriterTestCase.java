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

    public static final String NEW_LINE = "\n";

    @Test
    public void testMultiModuleConfigWriter(@TempDir final File tempDir) throws IOException {
        final Model model = createModel(List.of("foo", "bar"));
        final List<Dependency> dependencies = createDependencies();
        final MavenProject project = createProject(model, dependencies, tempDir);
        ConfigWriter.writeConfig(project, true);
        final String fileContents = Files
                .readString(tempDir.toPath().resolve(ConfigWriter.SASHIMONO_DIR).resolve(ConfigWriter.DEPENDENCIES_LIST));
        final String expected = """
                artifact com.acme:parent:1.0
                packaging pom
                module foo
                module bar
                require org.apache.httpcomponents:httpclient:4.5.14
                require org.hibernate.orm:hibernate-core:6.4.4.Final
                filtered_resources true
                """.replaceAll(NEW_LINE, System.lineSeparator());
        Assertions.assertEquals(expected, fileContents);
    }

    @Test
    public void testSingleModuleConfigWriter(@TempDir final File tempDir) throws IOException {
        final Model model = createModel("foo", "jar");
        final List<Dependency> dependencies = createDependencies();
        final MavenProject project = createProject(model, dependencies, tempDir);
        ConfigWriter.writeConfig(project, false);
        final String fileContents = Files
                .readString(tempDir.toPath().resolve(ConfigWriter.SASHIMONO_DIR).resolve(ConfigWriter.DEPENDENCIES_LIST));
        final String expected = """
                artifact com.acme:foo:1.0
                packaging jar
                require org.apache.httpcomponents:httpclient:4.5.14
                require org.hibernate.orm:hibernate-core:6.4.4.Final
                filtered_resources false
                """.replaceAll(NEW_LINE, System.lineSeparator());
        Assertions.assertEquals(expected, fileContents);
    }

    private Model createModel(final String artifactId, final String packaging) {
        final Model model = new Model();
        model.setGroupId("com.acme");
        model.setArtifactId(artifactId);
        model.setVersion("1.0");
        model.setPackaging(packaging);
        return model;
    }

    private Model createModel(final List<String> modules) {
        final Model model = createModel("parent", "pom");
        model.setModules(modules);
        return model;
    }

    private MavenProject createProject(final Model model, final List<Dependency> dependencies, final File tempDir) {
        final MavenProject project = new MavenProject(model);
        project.setFile(tempDir.toPath().resolve("pom.xml").toFile());
        project.setDependencies(dependencies);
        return project;
    }

    private List<Dependency> createDependencies() {
        final Dependency dependency1 = new Dependency();
        dependency1.setGroupId("org.apache.httpcomponents");
        dependency1.setArtifactId("httpclient");
        dependency1.setVersion("4.5.14");
        dependency1.setScope("compile");
        final Dependency dependency2 = new Dependency();
        dependency2.setGroupId("org.hibernate.orm");
        dependency2.setArtifactId("hibernate-core");
        dependency2.setVersion("6.4.4.Final");
        dependency2.setScope("provided");
        final Dependency dependency3 = new Dependency();
        dependency3.setGroupId("com.h2database");
        dependency3.setArtifactId("h2");
        dependency3.setVersion("h2.version");
        dependency3.setScope("test");
        return List.of(dependency1, dependency2, dependency3);
    }

}
