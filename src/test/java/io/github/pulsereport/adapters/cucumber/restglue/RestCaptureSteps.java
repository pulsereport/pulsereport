package io.github.pulsereport.adapters.cucumber.restglue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.When;
import io.github.pulsereport.adapters.restassured.RestAssuredAdapter;
import io.github.pulsereport.adapters.selenium.SeleniumTracker;
import io.restassured.filter.FilterContext;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/** Glue for {@code rest_capture.feature}: API calls in a hook and a step, a recording uploaded afterwards. */
public class RestCaptureSteps {

    private final RestAssuredAdapter rest = new RestAssuredAdapter();

    @Before
    public void createUser() {
        call("POST", "https://api.example.test/users", 201);
    }

    @When("I fetch the user")
    public void fetchUser() {
        call("GET", "https://api.example.test/users/1", 200);
        rest.recordResponseTime("ignored", 42);
    }

    @After
    public void uploadRecording() {
        SeleniumTracker.attachVideo("recording", "https://minio.example.test/videos/run-1/user.mp4",
                Instant.now().minusSeconds(5));
    }

    private void call(String method, String url, int status) {
        FilterableRequestSpecification request = mock(FilterableRequestSpecification.class);
        FilterableResponseSpecification responseSpec = mock(FilterableResponseSpecification.class);
        FilterContext context = mock(FilterContext.class);
        Response response = mock(Response.class);
        ResponseBody<?> body = mock(ResponseBody.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getURI()).thenReturn(url);
        when(request.getHeaders()).thenReturn(new Headers());
        when(context.next(request, responseSpec)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(status);
        when(response.getHeaders()).thenReturn(new Headers());
        when(response.getBody()).thenAnswer(i -> body);
        when(body.asString()).thenReturn("{\"id\":1}");
        rest.filter(request, responseSpec, context);
    }
}
