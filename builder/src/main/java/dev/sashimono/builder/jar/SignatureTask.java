package dev.sashimono.builder.jar;

import static java.lang.Boolean.getBoolean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import dev.sashimono.builder.util.Log;
import dev.sashimono.builder.util.TaskMap;

public class SignatureTask implements Function<TaskMap, Void> {

    private static final Log log = Log.of(SignatureTask.class);

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
                if (inputFileName.endsWith(".jar") || inputFileName.endsWith(".pom")) {
                    final Path outputPath = inputPath
                            .resolveSibling(inputFileName + ".asc");
                    final ProcessBuilder processBuilder = new ProcessBuilder();
                    final List<String> args = List.of(executablePath.toString(), "--local-user", keyName, "--passphrase",
                            passPhrase, "--batch", "--no-tty", "--armor", "--detach-sign", "--pinentry-mode", "loopback",
                            "--output", outputPath.toString(), "--sign", inputPath.toString());
                    processBuilder.command(args);
                    final ExecutorService executorService = Executors.newFixedThreadPool(2);
                    try {
                        final Process process = processBuilder.start();
                        executorService.submit(() -> new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                                .forEach(log::info));
                        executorService.submit(() -> new BufferedReader(new InputStreamReader(process.getErrorStream())).lines()
                                .forEach(log::error));
                        process.waitFor();
                    } catch (final IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        executorService.shutdown();
                    }
                }
            }
        }
        return null;
    }

}
