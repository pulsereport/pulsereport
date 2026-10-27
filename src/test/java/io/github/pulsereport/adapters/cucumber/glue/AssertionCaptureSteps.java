package io.github.pulsereport.adapters.cucumber.glue;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

/** Glue for {@code assertion_capture.feature}; the assertions are plain, unwrapped library calls. */
public class AssertionCaptureSteps {

    private int status;
    private String user;

    @Before
    public void hookAssertionIsNotAStep() {
        Assertions.assertTrue(true, "hook assertion");
    }

    @Given("a login response with status {int} for {string}")
    public void loginResponse(int status, String user) {
        this.status = status;
        this.user = user;
    }

    @Then("the status should be {int}")
    public void statusShouldBe(int expected) {
        Assertions.assertEquals(expected, status, "Login API status code");
    }

    @Then("the user name should be {string}")
    public void userNameShouldBe(String expected) {
        org.testng.Assert.assertEquals(user, expected, "user name");
        MatcherAssert.assertThat("user name length", user.length(), Matchers.greaterThan(0));
    }
}
