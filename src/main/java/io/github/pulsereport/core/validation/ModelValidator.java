package io.github.pulsereport.core.validation;

import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStep;
import io.github.pulsereport.core.model.TestSuite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Validates data model objects to ensure they meet required constraints.
 * Provides recursive validation for nested objects.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class ModelValidator {

    /**
     * Validates a model object.
     * 
     * @param object the object to validate
     * @return a list of validation error messages (empty if valid)
     */
    public List<String> validate(Object object) {
        List<String> errors = new ArrayList<>();
        
        if (object == null) {
            errors.add("Object cannot be null");
            return errors;
        }
        
        if (object instanceof TestRun run) {
            validateTestRun(run, errors);
        } else if (object instanceof TestSuite suite) {
            validateTestSuite(suite, errors);
        } else if (object instanceof TestCase case1) {
            validateTestCase(case1, errors);
        } else if (object instanceof TestStep step) {
            validateTestStep(step, errors);
        } else if (object instanceof Artifact artifact) {
            validateArtifact(artifact, errors);
        } else if (object instanceof Metric metric) {
            validateMetric(metric, errors);
        }
        
        return errors;
    }

    /**
     * Validates a TestRun object.
     * 
     * @param testRun the test run to validate
     * @param errors the list to collect errors
     */
    private void validateTestRun(TestRun testRun, List<String> errors) {
        validateRequiredString(testRun.getId(), "TestRun.id", errors);
        validateRequiredString(testRun.getName(), "TestRun.name", errors);
        validateRequiredObject(testRun.getStatus(), "TestRun.status", errors);
        validateRequiredObject(testRun.getStartTime(), "TestRun.startTime", errors);
        validateRequiredCollection(testRun.getSuites(), "TestRun.suites", errors);
        
        if (testRun.getSuites() != null) {
            int index = 0;
            for (TestSuite suite : testRun.getSuites()) {
                if (suite == null) {
                    errors.add("TestRun.suites[" + index + "] cannot be null");
                } else {
                    List<String> suiteErrors = new ArrayList<>();
                    validateTestSuite(suite, suiteErrors);
                    for (String error : suiteErrors) {
                        errors.add("TestRun.suites[" + index + "]." + error);
                    }
                }
                index++;
            }
        }
    }

    /**
     * Validates a TestSuite object.
     * 
     * @param testSuite the test suite to validate
     * @param errors the list to collect errors
     */
    private void validateTestSuite(TestSuite testSuite, List<String> errors) {
        validateRequiredString(testSuite.getId(), "TestSuite.id", errors);
        validateRequiredString(testSuite.getName(), "TestSuite.name", errors);
        validateRequiredObject(testSuite.getStatus(), "TestSuite.status", errors);
        validateRequiredCollection(testSuite.getTestCases(), "TestSuite.testCases", errors);
        
        if (testSuite.getTestCases() != null) {
            int index = 0;
            for (TestCase testCase : testSuite.getTestCases()) {
                if (testCase == null) {
                    errors.add("TestSuite.testCases[" + index + "] cannot be null");
                } else {
                    List<String> testCaseErrors = new ArrayList<>();
                    validateTestCase(testCase, testCaseErrors);
                    for (String error : testCaseErrors) {
                        errors.add("TestSuite.testCases[" + index + "]." + error);
                    }
                }
                index++;
            }
        }
    }

    /**
     * Validates a TestCase object.
     * 
     * @param testCase the test case to validate
     * @param errors the list to collect errors
     */
    private void validateTestCase(TestCase testCase, List<String> errors) {
        validateRequiredString(testCase.getId(), "TestCase.id", errors);
        validateRequiredString(testCase.getName(), "TestCase.name", errors);
        validateRequiredObject(testCase.getStatus(), "TestCase.status", errors);
        
        if (testCase.getSteps() != null) {
            int index = 0;
            for (TestStep step : testCase.getSteps()) {
                if (step == null) {
                    errors.add("TestCase.steps[" + index + "] cannot be null");
                } else {
                    List<String> stepErrors = new ArrayList<>();
                    validateTestStep(step, stepErrors);
                    for (String error : stepErrors) {
                        errors.add("TestCase.steps[" + index + "]." + error);
                    }
                }
                index++;
            }
        }
        
        if (testCase.getArtifacts() != null) {
            int index = 0;
            for (Artifact artifact : testCase.getArtifacts()) {
                if (artifact == null) {
                    errors.add("TestCase.artifacts[" + index + "] cannot be null");
                } else {
                    List<String> artifactErrors = new ArrayList<>();
                    validateArtifact(artifact, artifactErrors);
                    for (String error : artifactErrors) {
                        errors.add("TestCase.artifacts[" + index + "]." + error);
                    }
                }
                index++;
            }
        }
        
        if (testCase.getMetrics() != null) {
            int index = 0;
            for (Metric metric : testCase.getMetrics()) {
                if (metric == null) {
                    errors.add("TestCase.metrics[" + index + "] cannot be null");
                } else {
                    List<String> metricErrors = new ArrayList<>();
                    validateMetric(metric, metricErrors);
                    for (String error : metricErrors) {
                        errors.add("TestCase.metrics[" + index + "]." + error);
                    }
                }
                index++;
            }
        }
    }

    /**
     * Validates a TestStep object.
     * 
     * @param testStep the test step to validate
     * @param errors the list to collect errors
     */
    private void validateTestStep(TestStep testStep, List<String> errors) {
        validateRequiredString(testStep.getName(), "TestStep.name", errors);
        validateRequiredObject(testStep.getStatus(), "TestStep.status", errors);
    }

    /**
     * Validates an Artifact object.
     * 
     * @param artifact the artifact to validate
     * @param errors the list to collect errors
     */
    private void validateArtifact(Artifact artifact, List<String> errors) {
        validateRequiredString(artifact.getName(), "Artifact.name", errors);
        validateRequiredString(artifact.getType(), "Artifact.type", errors);
        validateRequiredString(artifact.getPath(), "Artifact.path", errors);
        validateRequiredObject(artifact.getTimestamp(), "Artifact.timestamp", errors);
    }

    /**
     * Validates a Metric object.
     * 
     * @param metric the metric to validate
     * @param errors the list to collect errors
     */
    private void validateMetric(Metric metric, List<String> errors) {
        validateRequiredString(metric.getName(), "Metric.name", errors);
        validateRequiredString(metric.getUnit(), "Metric.unit", errors);
        validateRequiredObject(metric.getTimestamp(), "Metric.timestamp", errors);
    }

    /**
     * Validates that a required string is not null or empty.
     * 
     * @param value the string value to validate
     * @param fieldName the name of the field
     * @param errors the list to collect errors
     */
    private void validateRequiredString(String value, String fieldName, List<String> errors) {
        if (value == null) {
            errors.add(fieldName + " is required (cannot be null)");
        } else if (value.trim().isEmpty()) {
            errors.add(fieldName + " is required (cannot be empty)");
        }
    }

    /**
     * Validates that a required object is not null.
     * 
     * @param value the object value to validate
     * @param fieldName the name of the field
     * @param errors the list to collect errors
     */
    private void validateRequiredObject(Object value, String fieldName, List<String> errors) {
        if (value == null) {
            errors.add(fieldName + " is required (cannot be null)");
        }
    }

    /**
     * Validates that a required collection is not null and not empty.
     * 
     * @param collection the collection to validate
     * @param fieldName the name of the field
     * @param errors the list to collect errors
     */
    private void validateRequiredCollection(Collection<?> collection, String fieldName, List<String> errors) {
        if (collection == null || collection.isEmpty()) {
            errors.add(fieldName + " is required and cannot be empty");
        }
    }
}
