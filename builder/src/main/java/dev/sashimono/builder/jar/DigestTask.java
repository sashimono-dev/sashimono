package dev.sashimono.builder.jar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Function;

import dev.sashimono.builder.util.HashUtil;
import dev.sashimono.builder.util.TaskMap;

public class DigestTask implements Function<TaskMap, Void> {
    @Override
    public Void apply(TaskMap taskMap) {
        var out = taskMap.results(FileOutput.class);
        for (var i : out) {
            try (var data = Files.newInputStream(i.file())) {
                Files.writeString(i.file().getParent().resolve(i.file().getFileName().toString() + ".md5"),
                        HashUtil.hashStream(data, "MD5"), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (var data = Files.newInputStream(i.file())) {
                Files.writeString(i.file().getParent().resolve(i.file().getFileName().toString() + ".sha1"),
                        HashUtil.hashStream(data, "SHA1"), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
