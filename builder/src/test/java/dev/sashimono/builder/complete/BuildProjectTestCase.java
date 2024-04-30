package dev.sashimono.builder.complete;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Assertions;

import dev.sashimono.builder.test.BuildResult;
import dev.sashimono.builder.test.BuildTest;

public class BuildProjectTestCase {

    public static final String NEW_LINE = "\n";

    @BuildTest("src/test/resources/simple-project")
    public void testBuildingSimpleProject(BuildResult result) throws IOException {
        Path dir = result.output().resolve("com").resolve("foo").resolve("test").resolve("1.1.0.Final");
        Path jar = dir.resolve("test-1.1.0.Final.jar");
        Path pom = dir.resolve("pom.xml");
        Assertions.assertTrue(Files.exists(jar));
        Assertions.assertTrue(Files.exists(pom));

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var main = jarFile.getJarEntry("foo/bar/Main.class");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
            var applicationProperties = jarFile.getJarEntry("config/application.properties");
            Assertions.assertNotNull(applicationProperties);
            String expectedAppPropsContents = """
                    greeting.message = hello
                    greeting.name = quarkus""".replaceAll("\n", System.lineSeparator());
            String appPropsContents = new String(jarFile.getInputStream(applicationProperties).readAllBytes(),
                    StandardCharsets.UTF_8);
            Assertions.assertEquals(expectedAppPropsContents, appPropsContents);
            Assertions.assertEquals(0, applicationProperties.getLastModifiedTime().toMillis());
        }

        String pomContents = Files.readString(pom);
        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.foo</groupId>
                    <artifactId>test</artifactId>
                    <version>1.1.0.Final</version>
                    <packaging>jar</packaging>


                    <dependencies>
                        <dependency>
                            <groupId>org.apache.httpcomponents</groupId>
                            <artifactId>httpclient</artifactId>
                            <version>4.5.14</version>
                            <scope>compile</scope>
                        </dependency>
                    </dependencies>


                </project>
                """.replaceAll(NEW_LINE, System.lineSeparator());
        Assertions.assertEquals(expected, pomContents);
    }

    @BuildTest("src/test/resources/multi-module-project")
    public void testBuildingMultiModuleProject(BuildResult result) throws IOException {
        Path dir = result.output().resolve("com").resolve("acme").resolve("foo").resolve("1.0");
        Path jar = dir.resolve("foo-1.0.jar");
        Path pom = dir.resolve("pom.xml");
        Assertions.assertTrue(Files.exists(jar));
        Assertions.assertTrue(Files.exists(pom));

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var main = jarFile.getJarEntry("acme/foo/Greeter.class");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
        }

        String pomContents = Files.readString(pom);
        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>

                    <parent>
                        <groupId>com.acme</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0</version>
                    </parent>

                    <groupId>com.acme</groupId>
                    <artifactId>foo</artifactId>
                    <version>1.0</version>
                    <packaging>jar</packaging>


                </project>
                """.replaceAll(NEW_LINE, System.lineSeparator());
        Assertions.assertEquals(expected, pomContents);

        dir = result.output().resolve("com").resolve("acme").resolve("bar").resolve("1.0");
        jar = dir.resolve("bar-1.0.jar");
        pom = dir.resolve("pom.xml");
        Assertions.assertTrue(Files.exists(jar));
        Assertions.assertTrue(Files.exists(pom));

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var main = jarFile.getJarEntry("acme/bar/Main.class");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
        }

        pomContents = Files.readString(pom);
        expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>

                    <parent>
                        <groupId>com.acme</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0</version>
                    </parent>

                    <groupId>com.acme</groupId>
                    <artifactId>bar</artifactId>
                    <version>1.0</version>
                    <packaging>jar</packaging>


                    <dependencies>
                        <dependency>
                            <groupId>com.acme</groupId>
                            <artifactId>foo</artifactId>
                            <version>1.0</version>
                            <scope>compile</scope>
                        </dependency>
                    </dependencies>


                </project>
                """.replaceAll(NEW_LINE, System.lineSeparator());
        Assertions.assertEquals(expected, pomContents);
    }
}
