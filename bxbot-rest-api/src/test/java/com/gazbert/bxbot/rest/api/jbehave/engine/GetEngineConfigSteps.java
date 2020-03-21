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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.rest.api.jbehave.AbstractSteps;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

/**
 * Steps for testing getting Engine Config story.
 *
 * @author gazbert
 */
public class GetEngineConfigSteps extends AbstractSteps {

  // --------------------------------------------------------------------------
  // Given step conditions
  // --------------------------------------------------------------------------

  @Given("a valid Engine Config API path")
  public void givenValidEngineConfigApiPath() {
    setApiPath("http://localhost:8080/api/v1/config/engine");
  }

  @Given("an invalid Engine Config API path")
  public void givenInvalidEngineConfigApiPath() {
    setApiPath("http://localhost:8080/api/v1/config/engine-not-here");
  }

  // --------------------------------------------------------------------------
  // When step conditions
  // --------------------------------------------------------------------------

  @When("a user calls the API with valid token")
  public void whenUserCallsApiWithValidToken() throws Exception {
    setHttpResponse(makeGetApiCallWithToken(getApiPath(), getUserToken()));
  }

  @When("a user calls the API without valid token")
  public void whenUserCallsApiWithoutValidToken() throws Exception {
    setHttpResponse(makeGetApiCallWithoutToken(getApiPath()));
  }

  // --------------------------------------------------------------------------
  // Then step conditions
  // --------------------------------------------------------------------------


  /**
   * Tests bot will respond with expected Engine config.
   *
   * @throws Exception is anything unexpected happens.
   */
  @Then("the bot will respond with expected Engine config")
  public void thenBotRespondsWithEngineConfig() throws Exception {

    final EngineConfig expectedEngineConfig = new EngineConfig();
    expectedEngineConfig.setBotId("my-bitstamp-bot_1");
    expectedEngineConfig.setBotName("Bitstamp Bot");
    expectedEngineConfig.setEmergencyStopCurrency("BTC");
    expectedEngineConfig.setEmergencyStopBalance(new BigDecimal("1.0"));
    expectedEngineConfig.setTradeCycleInterval(20);

    final ObjectMapper mapper = new ObjectMapper();
    final HttpEntity responseEntity = getHttpResponse().getEntity();
    final Header encodingHeader = responseEntity.getContentEncoding();

    final Charset encoding =
        encodingHeader == null
            ? StandardCharsets.UTF_8
            : Charsets.toCharset(encodingHeader.getValue());

    final String responseJson = EntityUtils.toString(responseEntity, encoding);
    final EngineConfig engineConfig = mapper.readValue(responseJson, EngineConfig.class);

    assertEquals(expectedEngineConfig, engineConfig);
  }
}
