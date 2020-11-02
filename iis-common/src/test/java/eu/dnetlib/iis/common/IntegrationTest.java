package eu.dnetlib.iis.common;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Integration test markup.
 * <p>
 * Integration tests are tests to be run using cluster environment.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Tag("IntegrationTest")
public @interface IntegrationTest {
}
