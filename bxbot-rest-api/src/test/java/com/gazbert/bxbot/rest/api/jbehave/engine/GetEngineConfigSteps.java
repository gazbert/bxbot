/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 gazbert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.rest.api.jbehave.engine;

import static junit.framework.TestCase.assertEquals;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

/**
 * Steps for testing fetching Engine Config story.
 *
 * @author gazbert
 */
public class GetEngineConfigSteps {

  private String api;
  private int httpResponseCode;
  private String mediaType;

  @Given("a valid Engine Config API path")
  public void givenValidEngineConfigApiPath() {
    api = "http://localhost:8080/api/v1/config/engine";
  }

  @When("I call the API without credentials")
  public void whenICallTheApi() throws IOException {
    httpResponseCode = makeApiCall(api).getStatusLine().getStatusCode();
  }

  @Then("the bot will respond with: 401 Unauthorized")
  public void thenBotRespondsWithUnauthorized() {
    assertEquals(SC_UNAUTHORIZED, httpResponseCode);
  }

  @When("I call the API with valid credentials")
  public void whenICallTheApiWithValidCredentials() throws IOException {
    // TODO: Make call to /auth endpoint to get token and pass into makeApiCall + push up to base class
    mediaType = ContentType.getOrDefault(makeApiCall(api).getEntity()).getMimeType();
  }

  @Then("the bot responds with data of type JSON")
  public void thenBotRespondsWithDataOfTypeJson() {
    assertEquals("application/json", mediaType);
  }

  private HttpResponse makeApiCall(String api) throws IOException {
    HttpUriRequest request = new HttpGet(api);
    return HttpClientBuilder.create().build().execute(request);
  }
}
