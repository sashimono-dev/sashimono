package dev.sashimono.builder.test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import dev.sashimono.builder.util.FileUtil;

/**
 * Compares the results of two builds to make sure they match
 */
class ProjectCompareExtension implements AfterEachCallback, InvocationInterceptor, ParameterResolver {

    final BuildExtension first;
    final BuildExtension second;

    ProjectCompareExtension(BuildExtension first, BuildExtension second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {

        Set<String> paths = new TreeSet<>();
        Files.walkFileTree(first.tempDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                paths.add(first.tempDir.relativize(file).toString());
                return super.visitFile(file, attrs);
            }
        });
        Files.walkFileTree(second.tempDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                paths.add(second.tempDir.relativize(file).toString());
                return super.visitFile(file, attrs);
            }
        });
        StringBuilder errors = new StringBuilder();
        for (var path : paths) {
            Path p1 = first.tempDir.resolve(path);
            Path p2 = second.tempDir.resolve(path);
            if (!Files.exists(p1)) {
                errors.append("First build did not produce file ")
                        .append(path)
                        .append(" that was produced by the second build\n");
                continue;
            }
            if (!Files.exists(p2)) {
                errors.append("Second build did not produce file ")
                        .append(path)
                        .append(" that was produced by the first build\n");
                continue;
            }
            var c1 = Files.readAllBytes(p1);
            var c2 = Files.readAllBytes(p2);

            if (!Arrays.equals(c1, c2)) {
                errors.append("File ")
                        .append(path)
                        .append(" differed between first and second builds\n");
            }
        }

        if (!errors.isEmpty()) {
            Assertions.fail(errors.toString());
        }
        invocation.skip();

    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        FileUtil.deleteRecursive(first.tempDir);
        FileUtil.deleteRecursive(second.tempDir);
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
        return new BuildResult(null); //not used
    }
}
