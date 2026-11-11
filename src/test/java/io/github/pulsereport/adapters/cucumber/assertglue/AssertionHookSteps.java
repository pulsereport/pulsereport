package io.github.pulsereport.adapters.cucumber.assertglue;

import org.testng.Assert;

import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeStep;
import io.cucumber.java.en.When;

/** Glue for {@code assertion_hooks.feature}: an assertion in every kind of hook and in the step. */
public class AssertionHookSteps {

    @Before
    public void seedData() {
        Assert.assertTrue(true, "data seeded");
    }

    @BeforeStep
    public void beforeEachStep() {
        Assert.assertTrue(true, "before step check");
    }

    @When("I check the total")
    public void checkTotal() {
        Assert.assertEquals(3, 3, "total");
    }

    @AfterStep
    public void afterEachStep() {
        Assert.assertTrue(true, "after step check");
    }

    @After
    public void cleanUp() {
        Assert.assertTrue(true, "data removed");
    }
}
