package dev.sashimono.builder.jar;

import static java.lang.Boolean.getBoolean;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import dev.sashimono.builder.util.TaskMap;

public class SignatureTask implements Function<TaskMap, Void> {

    @Override
    public Void apply(final TaskMap taskMap) {
        if (getBoolean("sign_artifacts")) {
            final Path executablePath = Path.of(System.getenv("GPG_EXECUTABLE_PATH"));
            final String keyName = System.getenv("GPG_KEYNAME");
            final String passPhrase = System.getenv("GPG_PASSPHRASE");

            final List<FileOutput> outputs = taskMap.results(FileOutput.class);
            for (final FileOutput i : outputs) {
                final Path inputPath = i.file().toAbsolutePath();
                final String inputFileName = inputPath.getFileName().toString();
                if (inputFileName.endsWith(".jar")) {
                    final Path outputPath = inputPath
                            .resolveSibling(inputFileName + ".asc");
                    final ProcessBuilder processBuilder = new ProcessBuilder();
                    final List<String> args = List.of(executablePath.toString(), "--local-user", keyName, "--passphrase",
                            passPhrase, "--batch", "--no-tty", "--armor", "--detach-sign", "--pinentry-mode", "loopback",
                            "--output", outputPath.toString(), "--sign", inputPath.toString());
                    processBuilder.command(args);
                    try {
                        processBuilder.start();
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return null;
    }

}
