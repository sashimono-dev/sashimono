package config;

import static dev.sashimono.builder.config.ConfigReader.DEPENDENCIES_LIST;
import static dev.sashimono.builder.config.ConfigReader.JAR;
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

import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.sashimono.mavenplugin.config.MavenConfigWriter;

public class MavenConfigWriterTestCase {

    public static final String NEW_LINE = "\n";
    public static final String POM = "pom";

    @Test
    public void testMultiModuleConfigWriter(@TempDir final File tempDir) throws IOException, XmlPullParserException {
        final Model model = createModel(List.of("foo", "bar"));
        final MavenProject project = createModule(model, tempDir);
        project.setBuild(createBuild("-parameters", "-proc:none"));
        project.setArtifact(new org.apache.maven.artifact.DefaultArtifact(project.getGroupId(), project.getArtifactId(),
                project.getVersion(), "compile", POM, null, new DefaultArtifactHandler()));
        MavenConfigWriter.writeConfig(project, true, this::createDependencies);
        final String fileContents = Files
                .readString(tempDir.toPath().resolve(SASHIMONO_DIR).resolve(DEPENDENCIES_LIST));
        final String expected = """
                artifact com.acme:parent:1.0
                packaging pom
                module foo
                module bar
                require org.apache.httpcomponents:httpclient:4.5.14
                require org.hibernate.orm:hibernate-core:6.4.4.Final
                require io.netty:netty-transport-native-epoll:4.1.110.Final:linux-aarch_64
                filtered_resources true
                source src/main/java
                pom pom.xml
                compiler_argument -source
                compiler_argument 1.8
                compiler_argument -target
                compiler_argument 1.8
                compiler_argument -parameters
                compiler_argument -proc:none
                """.replaceAll(NEW_LINE, System.lineSeparator());
        Assertions.assertEquals(expected, fileContents);
    }

    @Test
    public void testSingleModuleConfigWriter(@TempDir final File tempDir) throws IOException, XmlPullParserException {
        final Model model = createModel("foo", JAR);
        final MavenProject project = createModule(model, tempDir);
        final Build build = createBuild("foo.bar.Main", "-parameters", "-proc:none");
        project.setBuild(build);
        project.setArtifact(new org.apache.maven.artifact.DefaultArtifact(project.getGroupId(), project.getArtifactId(),
                project.getVersion(), "compile", JAR, "dummy", null));
        MavenConfigWriter.writeConfig(project, false, this::createDependencies);

        final String fileContents = Files
                .readString(tempDir.toPath().resolve(SASHIMONO_DIR).resolve(DEPENDENCIES_LIST));
        final String expected = """
                artifact com.acme:foo:1.0:dummy
                packaging jar
                require org.apache.httpcomponents:httpclient:4.5.14
                require org.hibernate.orm:hibernate-core:6.4.4.Final
                require io.netty:netty-transport-native-epoll:4.1.110.Final:linux-aarch_64
                filtered_resources false
                source src/main/java
                pom pom.xml
                manifest_entry mainClass:foo.bar.Main
                compiler_argument -source
                compiler_argument 1.8
                compiler_argument -target
                compiler_argument 1.8
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
        final Model model = createModel("parent", POM);
        model.setModules(modules);
        return model;
    }

    private MavenProject createModule(final Model model, final File tempDir) {
        final MavenProject project = new MavenProject(model);
        project.setFile(tempDir.toPath().resolve("pom.xml").toFile());
        project.addCompileSourceRoot(tempDir.toPath().resolve("src/main/java").toString());
        return project;
    }

    private List<Dependency> createDependencies() {
        return List.of(
                new Dependency(
                        new DefaultArtifact("org.apache.httpcomponents", "httpclient", null, "4.5.14"),
                        "compile"),
                new Dependency(
                        new DefaultArtifact("org.hibernate.orm", "hibernate-core", null, "6.4.4.Final"),
                        "provided"),
                new Dependency(new DefaultArtifact("com.h2database", "h2", null, "2.2.224"),
                        "test"),
                new Dependency(
                        new DefaultArtifact("io.netty", "netty-transport-native-epoll", "linux-aarch_64", null,
                                "4.1.110.Final"),
                        "compile"));
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
                    <source>1.8</source>
                    <target>1.8</target>
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
