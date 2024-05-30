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
        Assertions.assertEquals(1, config.moduleConfigs().size());
        var module = config.moduleConfigs().get(0);
        Assertions.assertEquals(2, module.dependencies().size());
        var dep1 = module.dependencies().get(0);
        Assertions.assertEquals("org.apache.httpcomponents", dep1.GAV().group());
        Assertions.assertEquals("httpclient", dep1.GAV().artifact());
        Assertions.assertEquals("4.5.14", dep1.GAV().version());
        var dep2 = module.dependencies().get(1);
        Assertions.assertEquals("io.netty", dep2.GAV().group());
        Assertions.assertEquals("netty-transport-native-epoll", dep2.GAV().artifact());
        Assertions.assertEquals("4.1.110.Final", dep2.GAV().version());
        Assertions.assertEquals("linux-aarch_64", dep2.classifier());
        Assertions.assertEquals(new GAV("com.foo", "test", "1.1.0.Final"), module.gav());
        Assertions.assertEquals(project.resolve(SASHIMONO_DIR).resolve(ConfigReader.RESOURCES_DIR),
                module.filteredResourcesDir());
        Assertions.assertEquals(List.of(project.resolve("src/main/java")), config.moduleConfigs().get(0).sourceDirectories());
        Assertions.assertEquals(project.resolve("pom.xml"),
                module.pomPath());
    }

}
