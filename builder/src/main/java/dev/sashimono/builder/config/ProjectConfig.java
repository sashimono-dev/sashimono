package dev.sashimono.builder.config;

import java.nio.file.Path;
import java.util.List;

/**
 * The full project config
 *
 * @param projectRoot The root of the project
 * @param moduleConfigs The individual module configs
 */
public record ProjectConfig(Path projectRoot, List<ModuleConfig> moduleConfigs) {

}
