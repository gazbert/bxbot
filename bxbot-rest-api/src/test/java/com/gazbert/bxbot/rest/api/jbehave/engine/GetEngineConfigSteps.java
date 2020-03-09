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
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.rest.api.security.authentication.JwtAuthenticationRequest;
import com.gazbert.bxbot.rest.api.security.authentication.JwtAuthenticationResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

/**
 * Steps for testing fetching Engine Config story.
 *
 * @author gazbert
 */
public class GetEngineConfigSteps {

  // TODO: Make config prop
  private static final String authUri = "http://localhost:8080/api/token";

  private String api;
  private HttpResponse httpResponse;

  @Given("a valid Engine Config API path")
  public void givenValidEngineConfigApiPath() {
    api = "http://localhost:8080/api/v1/config/engine";
  }

  @When("I call the API with valid credentials")
  public void whenICallTheApiWithValidCredentials() throws IOException {
    final String token = getToken();
    this.httpResponse = makeApiCall(api, token);
  }

  @Then("the bot will respond with expected Engine config")
  public void thenBotRespondsWithEngineConfig() throws Exception {

    final EngineConfig expectedEngineConfig = new EngineConfig();
    expectedEngineConfig.setBotId("my-bitstamp-bot_1");
    expectedEngineConfig.setBotName("Bitstamp Bot");
    expectedEngineConfig.setEmergencyStopCurrency("BTC");
    expectedEngineConfig.setEmergencyStopBalance(new BigDecimal("1.0"));
    expectedEngineConfig.setTradeCycleInterval(20);

    final ObjectMapper mapper = new ObjectMapper();
    final HttpEntity responseEntity = httpResponse.getEntity();
    final Header encodingHeader = responseEntity.getContentEncoding();

    final Charset encoding =
        encodingHeader == null
            ? StandardCharsets.UTF_8
            : Charsets.toCharset(encodingHeader.getValue());

    final String responseJson = EntityUtils.toString(responseEntity, encoding);
    final EngineConfig engineConfig = mapper.readValue(responseJson, EngineConfig.class);

    assertEquals(expectedEngineConfig, engineConfig);
  }

  @When("I call the API without credentials")
  public void whenICallTheApiWithoutCredentials() throws IOException {
    httpResponse = makeApiCall(api);
  }

  @Then("the bot will respond with 401 Unauthorized")
  public void thenBotRespondsWith401Unauthorized() {
    assertEquals(SC_UNAUTHORIZED, httpResponse.getStatusLine().getStatusCode());
  }

  @Given("an invalid Engine Config API path")
  public void givenInvalidEngineConfigApiPath() {
    api = "http://localhost:8080/api/v1/config/engine-not-here";
  }

  @Then("the bot will respond with 404 Not Found")
  public void thenBotRespondsWith404NotFound() {
    assertEquals(SC_NOT_FOUND, httpResponse.getStatusLine().getStatusCode());
  }

  // --------------------------------------------------------------------------
  // Private utils
  // TODO: Push up into a base class + clean up!
  // --------------------------------------------------------------------------

  private HttpResponse makeApiCall(String api) throws IOException {
    final HttpUriRequest request = new HttpGet(api);
    return HttpClientBuilder.create().build().execute(request);
  }

  private HttpResponse makeApiCall(String api, String token) throws IOException {
    final HttpUriRequest request = new HttpGet(api);
    request.setHeader("Authorization", "Bearer " + token);
    return HttpClientBuilder.create().build().execute(request);
  }

  private String getToken() throws IOException {
    // Creds must match real user entries in the ./java/resources/import.sql file
    final JwtAuthenticationRequest jwtAuthenticationRequest = new JwtAuthenticationRequest();
    jwtAuthenticationRequest.setUsername("user");
    jwtAuthenticationRequest.setPassword("user");

    final ObjectMapper mapper = new ObjectMapper();
    final String credentials = mapper.writeValueAsString(jwtAuthenticationRequest);
    final StringEntity requestEntity = new StringEntity(credentials, ContentType.APPLICATION_JSON);
    final HttpPost postMethod = new HttpPost(authUri);
    postMethod.setEntity(requestEntity);

    final CloseableHttpClient httpclient = HttpClients.createDefault();
    final HttpResponse response = httpclient.execute(postMethod);

    final HttpEntity responseEntity = response.getEntity();
    final Header encodingHeader = responseEntity.getContentEncoding();

    final Charset encoding =
        encodingHeader == null
            ? StandardCharsets.UTF_8
            : Charsets.toCharset(encodingHeader.getValue());

    final String responseJson = EntityUtils.toString(responseEntity, encoding);
    final JwtAuthenticationResponse authenticationResponse =
        mapper.readValue(responseJson, JwtAuthenticationResponse.class);

    httpclient.close();

    return authenticationResponse.getToken();
  }
}
