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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

/**
 * Steps for testing updating Engine Config story.
 *
 * @author gazbert
 */
public class UpdateEngineConfigSteps extends AbstractSteps {

  private String token;

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

  @When("administrator has a valid token")
  public void whenAdministratorHasValidToken() throws Exception {
    token = getAdminToken();
  }

  @When("administrator does not have valid token")
  public void whenAdministratorDoesNotHaveValidToken() {
    token = null;
  }

  /**
   * Parameter injection method 1.
   * The arguments extracted from the .story file step candidate are simply matched following
   * natural order to the parameters in the annotated Java method.
   */
  @When(
      "administrator calls API with botname $botname and tradeCycleInterval of $tradeCycleInterval")
  public void whenAdminCallsApiToUpdateEngineConfig(String botname, int tradeCycleInterval)
      throws Exception {

    final EngineConfig updateEngineConfig = new EngineConfig();
    updateEngineConfig.setBotId("my-bitstamp-bot_1");
    updateEngineConfig.setBotName(botname);
    updateEngineConfig.setTradeCycleInterval(tradeCycleInterval);

    final ObjectMapper mapper = new ObjectMapper();
    final String jsonPayload = mapper.writeValueAsString(updateEngineConfig);

    setHttpResponse(makeUpdateApiCallWithToken(getApiPath(), token, jsonPayload));
  }

  /**
   * Tests updating engine config.
   *
   * @param botname name of bot
   * @param tradeCycleInterval trade cycle interval in secs.
   * @throws Exception @throws Exception is anything unexpected happens.
   */
  @When(
      "user calls API to update Engine Config with botname $botname and tradeCycleInterval of"
          + " $tradeCycleInterval")
  public void whenUserCallsApiToUpdateEngineConfig(String botname, int tradeCycleInterval)
      throws Exception {

    final EngineConfig updateEngineConfig = new EngineConfig();
    updateEngineConfig.setBotId("my-bitstamp-bot_1");
    updateEngineConfig.setBotName(botname);
    updateEngineConfig.setTradeCycleInterval(tradeCycleInterval);

    final ObjectMapper mapper = new ObjectMapper();
    final String jsonPayload = mapper.writeValueAsString(updateEngineConfig);

    setHttpResponse(makeUpdateApiCallWithToken(getApiPath(), getUserToken(), jsonPayload));
  }

  // --------------------------------------------------------------------------
  // Then step conditions
  // --------------------------------------------------------------------------

  /**
   * Parameter injection method 2.
   * Here, The arguments extracted from the .story file step candidate are mapped explicitly to
   * the parameters in the annotated Java method.
   */
  @Then(
      "the bot will respond with updated Engine config with botname $name and trade cycle interval "
          + "$interval")
  public void thenBotRespondsWithUpdatedEngineConfig(
      @Named("name") String botname, @Named("interval") int tradeCycleInterval) throws Exception {

    final EngineConfig expectedEngineConfig = new EngineConfig();
    expectedEngineConfig.setBotId("my-bitstamp-bot_1");
    expectedEngineConfig.setBotName(botname);
    expectedEngineConfig.setTradeCycleInterval(tradeCycleInterval);

    final ObjectMapper mapper = new ObjectMapper();
    final HttpEntity responseEntity = getHttpResponse().getEntity();
    final Header encodingHeader = responseEntity.getContentEncoding();

    final Charset encoding =
        encodingHeader == null
            ? StandardCharsets.UTF_8
            : Charsets.toCharset(encodingHeader.getValue());

    final String responseJson = EntityUtils.toString(responseEntity, encoding);
    final EngineConfig engineConfig = mapper.readValue(responseJson, EngineConfig.class);

    assertEquals(expectedEngineConfig, engineConfig); // equality based on id only
    assertEquals(botname, engineConfig.getBotName());
    assertEquals(tradeCycleInterval, engineConfig.getTradeCycleInterval());
  }
}
