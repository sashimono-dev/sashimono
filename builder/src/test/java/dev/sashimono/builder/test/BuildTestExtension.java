package dev.sashimono.builder.test;

import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * Extension that builds a project multiple times and checks the result is reproducible.
 */
public class BuildTestExtension implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return isAnnotated(context.getTestMethod(), BuildTest.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        Method testMethod = context.getRequiredTestMethod();
        String displayName = context.getDisplayName();
        BuildExtension first = new BuildExtension();
        BuildExtension second = new BuildExtension();
        List<TestTemplateInvocationContext> tests = new ArrayList<>();
        tests.add(new BuildTestContext(displayName + " [First]", List.of(first)));
        tests.add(new BuildTestContext(displayName + " [Second]", List.of(second)));
        tests.add(new BuildTestContext(displayName + " [Reproducibility Check]",
                List.of(new ProjectCompareExtension(first, second))));
        return tests.stream();
    }

    class BuildTestContext implements TestTemplateInvocationContext {
        final String name;
        final List<Extension> extensions;

        BuildTestContext(String name, List<Extension> extensions) {
            this.name = name;
            this.extensions = extensions;
        }

        @Override
        public String getDisplayName(int invocationIndex) {
            return name;
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return extensions;
        }
    }
}
