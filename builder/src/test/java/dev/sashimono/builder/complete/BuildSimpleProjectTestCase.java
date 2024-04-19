package dev.sashimono.builder.complete;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.sashimono.builder.Sashimono;

public class BuildSimpleProjectTestCase {

    @Test
    public void testBuildingSimpleProject(@TempDir Path output) throws IOException {
        Path project = Paths.get("src/test/resources/simple-project");
        Sashimono.builder().setProjectRoot(project).setOutputDir(output).build().buildProject();
        Path jar = output.resolve("com").resolve("foo").resolve("test").resolve("1.1.0.Final").resolve("test-1.1.0.Final.jar");
        Assertions.assertTrue(Files.exists(jar));

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var main = jarFile.getJarEntry("foo/bar/Main.class");
            Assertions.assertNotNull(main);
            Assertions.assertTrue(main.getSize() > 100);
            Assertions.assertEquals(0, main.getLastModifiedTime().toMillis());
        }
    }

}
