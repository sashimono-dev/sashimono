package config;

import static dev.sashimono.config.ConfigWriter.DEPENDENCIES_LIST;
import static dev.sashimono.config.ConfigWriter.SASHIMONO_DIR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.config.ModuleConfig;
import dev.sashimono.config.ConfigWriter;

public class ConfigWriterTestCase {

    public static final String NEW_LINE = "\n";

    @Test
    public void testMultiModuleConfigWriter(@TempDir final File tempDir) throws IOException {
        final List<Dependency> dependencies = createDependencies();
        final var project = createProject(new GAV("com.acme", "parent", "1.0"), dependencies, tempDir, Map.of());
        ConfigWriter.writeConfig(tempDir.toPath(), project, List.of("foo", "bar"));
        final String fileContents = Files
                .readString(tempDir.toPath().resolve(SASHIMONO_DIR).resolve(DEPENDENCIES_LIST));
        final String expected = """
                artifact com.acme:parent:1.0
                packaging jar
                module foo
                module bar
                require org.apache.httpcomponents:httpclient:4.5.14
                require org.hibernate.orm:hibernate-core:6.4.4.Final
                require com.h2database:h2:h2.version
                filtered_resources false
                source src/main/java
                pom pom.xml
                """.replaceAll(NEW_LINE, System.lineSeparator());
        Assertions.assertEquals(expected, fileContents);
    }

    @Test
    public void testSingleModuleConfigWriter(@TempDir final File tempDir) throws Exception {
        final List<Dependency> dependencies = createDependencies();
        final var project = createProject(new GAV("com.acme", "foo", "1.0"), dependencies, tempDir,
                Map.of("mainClass", "foo.bar.Main"));
        //        final Build build = createBuild("foo.bar.Main");
        //        project.setBuild(build);
        ConfigWriter.writeConfig(tempDir.toPath(), project, List.of());

        final String fileContents = Files
                .readString(tempDir.toPath().resolve(SASHIMONO_DIR).resolve(DEPENDENCIES_LIST));
        final String expected = """
                artifact com.acme:foo:1.0
                packaging jar
                require org.apache.httpcomponents:httpclient:4.5.14
                require org.hibernate.orm:hibernate-core:6.4.4.Final
                require com.h2database:h2:h2.version
                filtered_resources false
                source src/main/java
                pom pom.xml
                manifest_entry mainClass:foo.bar.Main
                """.replaceAll(NEW_LINE, System.lineSeparator());
        Assertions.assertEquals(expected, fileContents);
    }

    private ModuleConfig createProject(final GAV project, final List<Dependency> dependencies, final File tempDir,
            Map<String, String> manifest) {
        return new ModuleConfig(project,
                "jar",
                dependencies,
                List.of(tempDir.toPath().resolve("src/main/java")),
                null, tempDir.toPath().resolve("pom.xml"), manifest);
    }

    private List<Dependency> createDependencies() {
        final Dependency dependency1 = new Dependency(
                new GAV("org.apache.httpcomponents", "httpclient", "4.5.14"), "jar");
        final Dependency dependency2 = new Dependency(new GAV("org.hibernate.orm", "hibernate-core", "6.4.4.Final"), "jar");
        final Dependency dependency3 = new Dependency(new GAV("com.h2database", "h2", "h2.version"), "jar");
        return List.of(dependency1, dependency2, dependency3);
    }

}
