package dev.sashimono.builder.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Runs a Sashimono build against the specified directory.
 *
 * The test is actually run twice and the results are compared to make sure the build is reproducible.
 *
 * The results can be injected via {@link BuildResult}
 */
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(BuildTestExtension.class)
@Target(ElementType.METHOD)
@TestTemplate
public @interface BuildTest {
    String value();
}
