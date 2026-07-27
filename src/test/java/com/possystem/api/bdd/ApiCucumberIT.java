package com.possystem.api.bdd;

import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * JUnit 5 entry point for the Cucumber BDD scenarios covering the REST API
 * (src/test/resources/features/api/*.feature).
 *
 * Named {@code *IT} (not {@code *Test}) on purpose: Cucumber's JUnit Platform engine does not
 * translate an {@code org.junit.Assume}/Assumptions-based skip thrown from a {@code @Before}
 * hook into a SKIPPED result the way plain JUnit 5 tests do — it surfaces as a test ERROR
 * instead. Rather than let that fail the default build, this class is picked up by
 * maven-failsafe-plugin's {@code integration-test}/{@code verify} goals instead of Surefire's
 * {@code test} goal, so it's excluded from the always-green {@code mvn test} path and only runs
 * as part of {@code mvn verify} — which should only be invoked once the API server is actually
 * running (see README: BDD & E2E Testing).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/api")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.possystem.api.bdd")
@Tag("bdd")
@Tag("integration")
public class ApiCucumberIT {
}
