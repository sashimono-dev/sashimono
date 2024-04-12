package dev.sashimono.builder.config;

import java.nio.file.Path;
import java.util.List;

public record ProjectConfig(Path projectRoot, List<ModuleConfig> moduleConfigs) {

}
