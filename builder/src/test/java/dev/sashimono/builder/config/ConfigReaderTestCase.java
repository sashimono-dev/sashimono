package dev.sashimono.builder.config;

import static dev.sashimono.builder.config.ConfigReader.SASHIMONO_DIR;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigReaderTestCase {

    @Test
    public void testConfigReader() {
        Path project = Paths.get("src/test/resources/simple-project");
        ProjectConfig config = ConfigReader.readConfig(project);
        Assertions.assertEquals(project.resolve(SASHIMONO_DIR).resolve(ConfigReader.RESOURCES_DIR),
                config.filteredResourcesDir());
        Assertions.assertEquals(1, config.moduleConfigs().size());
        var module = config.moduleConfigs().get(0);
        Assertions.assertEquals(1, module.dependencies().size());
        var dep = module.dependencies().get(0);
        Assertions.assertEquals("org.apache.httpcomponents", dep.GAV().group());
        Assertions.assertEquals("httpclient", dep.GAV().artifact());
        Assertions.assertEquals("4.5.14", dep.GAV().version());
        Assertions.assertEquals(new GAV("com.foo", "test", "1.1.0.Final"), module.gav());
        Assertions.assertEquals(List.of(project.resolve("src/main/java")), config.moduleConfigs().get(0).sourceDirectories());
    }

}
