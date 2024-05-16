package config;

import static dev.sashimono.builder.config.ConfigReader.DEPENDENCIES_LIST;
import static dev.sashimono.builder.config.ConfigReader.SASHIMONO_DIR;
import static dev.sashimono.mavenplugin.config.MavenConfigWriter.MAVEN_COMPILER_PLUGIN;
import static dev.sashimono.mavenplugin.config.MavenConfigWriter.MAVEN_JAR_PLUGIN;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.sashimono.mavenplugin.config.MavenConfigWriter;

public class MavenConfigWriterTestCase {

    public static final String NEW_LINE = "\n";

    @Test
    public void testMultiModuleConfigWriter(@TempDir final File tempDir) throws IOException, XmlPullParserException {
        final Model model = createModel(List.of("foo", "bar"));
        final List<Dependency> dependencies = createDependencies();
        final MavenProject project = createModule(model, dependencies, tempDir);
        project.setBuild(createBuild("-parameters", "-proc:none"));
        MavenConfigWriter.writeConfig(project, true, () -> List.of(
                new org.eclipse.aether.graph.Dependency(new DefaultArtifact("org.apache.httpcomponents:httpclient:4.5.14"),
                        "compile"),
                new org.eclipse.aether.graph.Dependency(new DefaultArtifact("org.hibernate.orm:hibernate-core:6.4.4.Final"),
                        "provided")));
        final String fileContents = Files
                .readString(tempDir.toPath().resolve(SASHIMONO_DIR).resolve(DEPENDENCIES_LIST));
        final String expected = """
                artifact com.acme:parent:1.0
                packaging pom
                module foo
                module bar
                require org.apache.httpcomponents:httpclient:4.5.14
                require org.hibernate.orm:hibernate-core:6.4.4.Final
                filtered_resources true
                source src/main/java
                pom pom.xml
                compiler_argument -parameters
                compiler_argument -proc:none
                """.replaceAll(NEW_LINE, System.lineSeparator());
        Assertions.assertEquals(expected, fileContents);
    }

    @Test
    public void testSingleModuleConfigWriter(@TempDir final File tempDir) throws IOException, XmlPullParserException {
        final Model model = createModel("foo", "jar");
        final List<Dependency> dependencies = createDependencies();
        final MavenProject project = createModule(model, dependencies, tempDir);
        final Build build = createBuild("foo.bar.Main", "-parameters", "-proc:none");
        project.setBuild(build);
        MavenConfigWriter.writeConfig(project, false, () -> List.of(
                new org.eclipse.aether.graph.Dependency(new DefaultArtifact("org.apache.httpcomponents:httpclient:4.5.14"),
                        "compile"),
                new org.eclipse.aether.graph.Dependency(new DefaultArtifact("org.hibernate.orm:hibernate-core:6.4.4.Final"),
                        "provided")));

        final String fileContents = Files
                .readString(tempDir.toPath().resolve(SASHIMONO_DIR).resolve(DEPENDENCIES_LIST));
        final String expected = """
                artifact com.acme:foo:1.0
                packaging jar
                require org.apache.httpcomponents:httpclient:4.5.14
                require org.hibernate.orm:hibernate-core:6.4.4.Final
                filtered_resources false
                source src/main/java
                pom pom.xml
                manifest_entry mainClass:foo.bar.Main
                compiler_argument -parameters
                compiler_argument -proc:none
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

    private MavenProject createModule(final Model model, final List<Dependency> dependencies, final File tempDir) {
        final MavenProject project = new MavenProject(model);
        project.setFile(tempDir.toPath().resolve("pom.xml").toFile());
        project.setDependencies(dependencies);
        project.addCompileSourceRoot(tempDir.toPath().resolve("src/main/java").toString());
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

    private Build createBuild(final String compilerArg1, final String compilerArg2)
            throws XmlPullParserException, IOException {
        return createBuild(null, compilerArg1, compilerArg2);
    }

    private Build createBuild(final String mainClassName, final String compilerArg1, final String compilerArg2)
            throws XmlPullParserException, IOException {
        final Build build = new Build();
        final List<Plugin> plugins = new ArrayList<>();
        if (mainClassName != null) {
            final Plugin jarPlugin = createJarPlugin(mainClassName);
            plugins.add(jarPlugin);
        }
        if (compilerArg1 != null && compilerArg2 != null) {
            final Plugin compilerPlugin = createCompilerPlugin(compilerArg1, compilerArg2);
            plugins.add(compilerPlugin);
        }
        build.setPlugins(plugins);
        return build;
    }

    private Plugin createJarPlugin(final String mainClassName) throws XmlPullParserException, IOException {
        final Plugin plugin = new Plugin();
        plugin.setArtifactId(MAVEN_JAR_PLUGIN);
        final String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>%s</mainClass>
                        </manifest>
                    </archive>
                </configuration>
                """.formatted(mainClassName);
        plugin.setConfiguration(processXml(xml));
        return plugin;
    }

    private Plugin createCompilerPlugin(final String compilerArg1, final String compilerArg2)
            throws XmlPullParserException, IOException {
        final Plugin plugin = new Plugin();
        plugin.setArtifactId(MAVEN_COMPILER_PLUGIN);
        final String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <configuration>
                    <compilerArgs>
                        <arg>%s</arg>
                    </compilerArgs>
                    <compilerArgument>%s</compilerArgument>
                </configuration>
                """.formatted(compilerArg1, compilerArg2);
        plugin.setConfiguration(processXml(xml));
        return plugin;
    }

    private Xpp3Dom processXml(final String xml) throws XmlPullParserException, IOException {
        final InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        return Xpp3DomBuilder.build(is, String.valueOf(StandardCharsets.UTF_8));
    }
}
