package dev.sashimono.builder.test;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.io.TempDirFactory;

import dev.sashimono.builder.Sashimono;

class BuildExtension implements BeforeEachCallback, ParameterResolver {

    Path tempDir;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        tempDir = TempDirFactory.Standard.INSTANCE.createTempDirectory(null, context);
        var ann = context.getRequiredTestMethod().getAnnotation(BuildTest.class);
        Path project = Paths.get(ann.value());
        Sashimono.builder().setProjectRoot(project).setOutputDir(tempDir).build().buildProject();
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
