package dev.sashimono.builder.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigReaderTestCase {

    @Test
    public void testConfigReader() {
        Path project = Paths.get("src/test/resources/simple-project");
        ProjectConfig config = ConfigReader.readConfig(project);
        Assertions.assertEquals(project.resolve(ConfigReader.SASHIMONO_DIR).resolve(ConfigReader.RESOURCES),
                config.filteredResourcesDir());
        Assertions.assertEquals(1, config.moduleConfigs().size());
        var module = config.moduleConfigs().get(0);
        Assertions.assertEquals(1, module.dependencies().size());
        var dep = module.dependencies().get(0);
        Assertions.assertEquals("org.apache.httpcomponents", dep.GAV().group());
        Assertions.assertEquals("httpclient", dep.GAV().artifact());
        Assertions.assertEquals("4.5.14", dep.GAV().version());
        Assertions.assertEquals(new GAV("com.foo", "test", "1.1.0.Final"), module.gav());
    }

}
