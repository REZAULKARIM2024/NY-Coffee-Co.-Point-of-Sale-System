package com.possystem.api.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * JUnit 5 entry point for the Cucumber BDD scenarios covering the REST API
 * (src/test/resources/features/api/*.feature). Named to match Surefire's default
 * {@code *Test.java} inclusion pattern, so no extra Maven/Eclipse configuration is needed —
 * it runs automatically alongside the other test classes whenever {@code mvn test} or
 * Eclipse's JUnit runner executes.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/api")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.possystem.api.bdd")
public class ApiCucumberTest {
}
