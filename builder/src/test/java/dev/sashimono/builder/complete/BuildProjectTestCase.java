package dev.sashimono.builder.complete;

import static dev.sashimono.builder.jar.JarTask.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
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
    public void testBuildingSimpleProject(BuildResult result)
            throws IOException, ClassNotFoundException, NoSuchMethodException {
        Path dir = result.output().resolve("com").resolve("foo").resolve("test").resolve("1.1.0.Final");
        Path jar = dir.resolve("test-1.1.0.Final.jar");
        Path pom = dir.resolve("test-1.1.0.Final.pom");
        Path sourcesJar = dir.resolve("test-1.1.0.Final-sources.jar");
        Path javadocJar = dir.resolve("test-1.1.0.Final-javadoc.jar");
        Assertions.assertTrue(Files.exists(jar));
        Assertions.assertTrue(Files.exists(pom));
        Assertions.assertTrue(Files.exists(sourcesJar));
        Assertions.assertTrue(Files.exists(javadocJar));
        String expectedAppPropsContents = """
                greeting.message = hello
                greeting.name = quarkus""".replaceAll(NEW_LINE, System.lineSeparator());

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var main = jarFile.getJarEntry("foo/bar/Main.class");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
            var applicationProperties = jarFile.getJarEntry("config/application.properties");
            Assertions.assertNotNull(applicationProperties);
            String appPropsContents = new String(jarFile.getInputStream(applicationProperties).readAllBytes(),
                    StandardCharsets.UTF_8);
            Assertions.assertEquals(expectedAppPropsContents, appPropsContents);
            Assertions.assertEquals(0, applicationProperties.getLastModifiedTime().toMillis());
            var manifest = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            Assertions.assertNotNull(manifest);
            String expectedManifestContents = """
                    Build-Tool-Jdk-Spec: %s
                    Main-Class: foo.bar.Main
                    One-Thousand: d9vv1dQJkclSxtvwo21AT4pLr4Ijl8R1p6KPzpfvsFiwn0ZoBkFpJ71C
                     zxHEiMUZQfU4BvGpEyNs3IrmsyZx41P1618zm68dwZULWp4UefXRKe3RvFTLctQPqX11E
                     HKdQNYai0p1HJAd9OtwR8U9WOb9cOhpsZoC6oDgJG15N0ptiFzCniLBP2Rh8FT27ZC24H
                     TbATknxt3mI6q3DXy43BjzxoIpY8zi5ZFbBdgiMkErR481KADeApzhyjSmcefkdNs8r8e
                     VL8WJrpgxJT9JOr18SCqKMq3Lti55WE96JI9hpUCm2OCNjghTKllfeKZg2d1kOCKh5d9M
                     laZEptga9QR9DCYM6BXLWFBJg3E46thEMMxYj4rhoZ4JaY2EAygUcHEJs2Ynb1tDLiK2G
                     fbETwWUDW9GfR6um3YtE0LCIKbpC0mI2NOOdBDiSm8kbD3mRQk4bxpr4bDHViTGaPS2Fm
                     oYYXOTISl0qFyyiFm1QEyvKtNdMUn0CcypVjPlJWpKOgu3qL3Q1RkEHAbY9INF4N3wl7q
                     ssM90bA21iMkxluyjfz1B6htp9BC4rhlC989iAbP6X880mmcCwNQxQxnSoVHLcOq4eKbY
                     0VxNWcPKGzqQsLsZBRF9OcU80XqwVVyHvQ4yawi5oXnQniH6aqgWeDFSfgfIMY6SFoLxV
                     zmTgvOi5AqbTftPCBdlZhHWmTVjPKKKXhkp0hjryt0OCnr8N723lzIthW1XBdE9CQEXH6
                     KRsXBwHkzVRgwEFLz2L25Y1oErENlH80W6pdVbUVn5I2DRTQ4xSNJswHHRWSUnbEF9Byx
                     tCdw4gieFkb3yS8Y1RoOmzmdUPauqFauTwNVRJrycRqqsaN5ZHqqIMSvYiXJeN8vFrgsx
                     SxBzN0u1Pyojk1q3jckKIwTt4dvGxnFby1TcYh8T3u8b98UPOWv7vjwg1cKZY6U8z1iYn
                     NxRjpSPM8ItUEuI4fgkUcyaWykVVPPny9
                    Seventy: ompQpCz2x8oMrzRBgJdS2s5AllQGUIEMi5a5C1i3FUVdbqRHNZwUk0GEwyFkJ
                    Seventy-Three: 0v5plZ0ZULBPWq3O8Xc9LVvoAfar45VFUi8Zf5bdQisT3lAGgq36qQc
                     SzT
                    Seventy-Two: DEOF6Z8F2YP96Pp5moOuDPDhzRprwY9JNYn9mPlbDQogriQwJ1gbDqH8HqV
                    """.formatted(System.getProperty(JAVA_SPEC_VERSION)).replaceAll(NEW_LINE,
                    EOL);
            String manifestContents = new String(jarFile.getInputStream(manifest).readAllBytes(),
                    StandardCharsets.UTF_8);
            Assertions.assertEquals(expectedManifestContents, manifestContents);
            Assertions.assertEquals(0, manifest.getLastModifiedTime().toMillis());
            URLClassLoader classLoader = new URLClassLoader(new URL[] { jar.toFile().toURI().toURL() });
            Class clazz = Class.forName("foo.bar.Main", true, classLoader);
            Method method = clazz.getMethod("main", String[].class);
            Parameter[] parameters = method.getParameters();
            Assertions.assertEquals(1, parameters.length);
            Assertions.assertEquals("args", parameters[0].getName());
        }

        String pomContents = Files.readString(pom);
        String expectedPomContents = """
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
        Assertions.assertEquals(expectedPomContents, pomContents);

        try (JarFile sourcesJarFile = new JarFile(sourcesJar.toFile())) {
            var main = sourcesJarFile.getJarEntry("foo/bar/Main.java");
            Assertions.assertNotNull(main);
            String expectedMainContents = """
                    package foo.bar;

                    public class Main {

                        public static void main(String ... args) {
                            System.out.println("Hello World");
                        }

                    }
                    """.replaceAll(NEW_LINE,
                    System.lineSeparator());
            String mainContents = new String(sourcesJarFile.getInputStream(main).readAllBytes(),
                    StandardCharsets.UTF_8);
            Assertions.assertEquals(expectedMainContents, mainContents);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
            var applicationProperties = sourcesJarFile.getJarEntry("config/application.properties");
            Assertions.assertNotNull(applicationProperties);
            String appPropsContents = new String(sourcesJarFile.getInputStream(applicationProperties).readAllBytes(),
                    StandardCharsets.UTF_8);
            Assertions.assertEquals(expectedAppPropsContents, appPropsContents);
            Assertions.assertEquals(0, applicationProperties.getLastModifiedTime().toMillis());
        }

        try (JarFile javadocJarFile = new JarFile(javadocJar.toFile())) {
            var main = javadocJarFile.getJarEntry("foo/bar/Main.html");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
        }
    }

    @BuildTest("src/test/resources/multi-module-project")
    public void testBuildingMultiModuleProject(BuildResult result)
            throws IOException, ClassNotFoundException, NoSuchMethodException {
        Path dir = result.output().resolve("com").resolve("acme").resolve("foo").resolve("1.0");
        Path jar = dir.resolve("foo-1.0.jar");
        Path pom = dir.resolve("foo-1.0.pom");
        Path sourcesJar = dir.resolve("foo-1.0-sources.jar");
        Path javadocJar = dir.resolve("foo-1.0-javadoc.jar");
        Assertions.assertTrue(Files.exists(jar));
        Assertions.assertTrue(Files.exists(pom));
        Assertions.assertTrue(Files.exists(sourcesJar));
        Assertions.assertTrue(Files.exists(javadocJar));

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var main = jarFile.getJarEntry("acme/foo/Greeter.class");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
            var manifest = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            Assertions.assertNotNull(manifest);
            String expectedManifestContents = """
                    Build-Tool-Jdk-Spec: %s
                    """.formatted(System.getProperty(JAVA_SPEC_VERSION)).replaceAll(NEW_LINE,
                    EOL);
            String manifestContents = new String(jarFile.getInputStream(manifest).readAllBytes(),
                    StandardCharsets.UTF_8);
            Assertions.assertEquals(expectedManifestContents, manifestContents);
            Assertions.assertEquals(0, manifest.getLastModifiedTime().toMillis());
            URLClassLoader classLoader = new URLClassLoader(new URL[] { jar.toFile().toURI().toURL() });
            Class clazz = Class.forName("acme.foo.Greeter", true, classLoader);
            Method method = clazz.getMethod("greet", String.class);
            Parameter[] parameters = method.getParameters();
            Assertions.assertEquals(1, parameters.length);
            Assertions.assertEquals("name", parameters[0].getName());
        }

        String pomContents = Files.readString(pom);
        String expectedPomContents = """
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
        Assertions.assertEquals(expectedPomContents, pomContents);

        try (JarFile sourcesJarFile = new JarFile(sourcesJar.toFile())) {
            var greeter = sourcesJarFile.getJarEntry("acme/foo/Greeter.java");
            Assertions.assertNotNull(greeter);
            String expectedMainContents = """
                    package acme.foo;

                    public class Greeter {

                        public String greet(String name) {
                            return "Hello " + name;
                        }

                    }
                    """.replaceAll(NEW_LINE,
                    System.lineSeparator());
            String mainContents = new String(sourcesJarFile.getInputStream(greeter).readAllBytes(),
                    StandardCharsets.UTF_8);
            Assertions.assertEquals(expectedMainContents, mainContents);
            Assertions.assertEquals(0, greeter.getLastModifiedTime().toMillis());
        }

        try (JarFile javadocJarFile = new JarFile(javadocJar.toFile())) {
            var greeter = javadocJarFile.getJarEntry("acme/foo/Greeter.html");
            Assertions.assertNotNull(greeter);
            Assertions.assertTrue(greeter.getSize() > 100);
            Assertions.assertEquals(0, greeter.getLastModifiedTime().toMillis());
        }

        dir = result.output().resolve("com").resolve("acme").resolve("bar").resolve("1.0");
        jar = dir.resolve("bar-1.0.jar");
        pom = dir.resolve("bar-1.0.pom");
        sourcesJar = dir.resolve("bar-1.0-sources.jar");
        javadocJar = dir.resolve("bar-1.0-javadoc.jar");
        Assertions.assertTrue(Files.exists(jar));
        Assertions.assertTrue(Files.exists(pom));
        Assertions.assertTrue(Files.exists(sourcesJar));
        Assertions.assertTrue(Files.exists(javadocJar));

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var main = jarFile.getJarEntry("acme/bar/Main.class");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
            var manifest = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            Assertions.assertNotNull(manifest);
            String expectedManifestContents = """
                    Build-Tool-Jdk-Spec: %s
                    Main-Class: acme.bar.Main
                    """.formatted(System.getProperty(JAVA_SPEC_VERSION)).replaceAll(NEW_LINE,
                    EOL);
            String manifestContents = new String(jarFile.getInputStream(manifest).readAllBytes(),
                    StandardCharsets.UTF_8);
            Assertions.assertEquals(expectedManifestContents, manifestContents);
            Assertions.assertEquals(0, manifest.getLastModifiedTime().toMillis());
            URLClassLoader classLoader = new URLClassLoader(new URL[] { jar.toFile().toURI().toURL() });
            Class clazz = Class.forName("acme.bar.Main", true, classLoader);
            Method method = clazz.getMethod("main", String[].class);
            Parameter[] parameters = method.getParameters();
            Assertions.assertEquals(1, parameters.length);
            Assertions.assertEquals("args", parameters[0].getName());
        }

        pomContents = Files.readString(pom);
        expectedPomContents = """
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
        Assertions.assertEquals(expectedPomContents, pomContents);

        try (JarFile sourcesJarFile = new JarFile(sourcesJar.toFile())) {
            var main = sourcesJarFile.getJarEntry("acme/bar/Main.java");
            Assertions.assertNotNull(main);
            String expectedMainContents = """
                    package acme.bar;

                    import acme.foo.Greeter;
                    public class Main {
                        public static void main(String ... args) {
                            System.out.println(new Greeter().greet("Sashimono"));
                        }

                    }
                    """.replaceAll(NEW_LINE,
                    System.lineSeparator());
            String mainContents = new String(sourcesJarFile.getInputStream(main).readAllBytes(),
                    StandardCharsets.UTF_8);
            Assertions.assertEquals(expectedMainContents, mainContents);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
        }

        try (JarFile javadocJarFile = new JarFile(javadocJar.toFile())) {
            var main = javadocJarFile.getJarEntry("acme/bar/Main.html");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
        }
    }
}
