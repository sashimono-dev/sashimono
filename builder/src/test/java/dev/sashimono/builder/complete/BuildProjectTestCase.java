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

    @BuildTest("src/test/resources/simple-project")
    public void testBuildingSimpleProject(BuildResult result) throws IOException {
        Path jar = result.output().resolve("com").resolve("foo").resolve("test").resolve("1.1.0.Final")
                .resolve("test-1.1.0.Final.jar");
        Assertions.assertTrue(Files.exists(jar));

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
    }

    @BuildTest("src/test/resources/multi-module-project")
    public void testBuildingMultiModuleProject(BuildResult result) throws IOException {
        Path jar = result.output().resolve("com").resolve("acme").resolve("foo").resolve("1.0").resolve("foo-1.0.jar");
        Assertions.assertTrue(Files.exists(jar));

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var main = jarFile.getJarEntry("acme/foo/Greeter.class");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
        }

        jar = result.output().resolve("com").resolve("acme").resolve("bar").resolve("1.0").resolve("bar-1.0.jar");
        Assertions.assertTrue(Files.exists(jar));

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var main = jarFile.getJarEntry("acme/bar/Main.class");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
        }
    }
}
