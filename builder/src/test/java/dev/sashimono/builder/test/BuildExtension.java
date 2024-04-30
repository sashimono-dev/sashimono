package dev.sashimono.builder.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.io.TempDirFactory;

import dev.sashimono.builder.Sashimono;
import dev.sashimono.builder.util.HashUtil;

class BuildExtension implements BeforeEachCallback, ParameterResolver {

    Path tempDir;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        tempDir = TempDirFactory.Standard.INSTANCE.createTempDirectory(null, context);
        var ann = context.getRequiredTestMethod().getAnnotation(BuildTest.class);
        Path project = Paths.get(ann.value());
        Sashimono.builder().setProjectRoot(project).setOutputDir(tempDir).build().buildProject();

        //Test all files of a certain type are signed
        //TODO: should this be in some kind of extension system that lets you apply tests to all builds?

        try (Stream<Path> pathStream = Files.walk(tempDir)) {
            pathStream.filter(f -> f.getFileName().toString().endsWith(".jar") || f.getFileName().toString().endsWith(".xml"))
                    .forEach(f -> {
                        try (var in = Files.newInputStream(f)) {
                            Path md5 = f.getParent().resolve(f.getFileName() + ".md5");
                            Assertions.assertEquals(Files.readString(md5), HashUtil.md5(in));
                        } catch (Exception e) {
                            throw new RuntimeException("MD5 match failed for " + f, e);
                        }
                        try (var in = Files.newInputStream(f)) {
                            Path sha1 = f.getParent().resolve(f.getFileName() + ".sha1");
                            Assertions.assertEquals(Files.readString(sha1), HashUtil.sha1(in));
                        } catch (IOException e) {
                            throw new RuntimeException("SHA1 match failed for " + f, e);
                        }
                    });
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type == BuildResult.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return new BuildResult(tempDir);
    }
}
