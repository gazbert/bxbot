/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
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

package com.gazbert.bxbot.rest.api.v1.config;

import static com.gazbert.bxbot.rest.api.v1.EndpointLocations.CONFIG_ENDPOINT_BASE_URI;

import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.services.config.ExchangeConfigService;
import io.swagger.annotations.Api;
import java.security.Principal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for directing Exchange config requests.
 *
 * <p>Exchange config can only be fetched and updated - it cannot be deleted or created.
 *
 * <p>There is only 1 Exchange Adapter per bot.
 *
 * @author gazbert
 * @since 1.0
 */
@Api(tags = {"Exchange Configuration"})
@RestController
@RequestMapping(CONFIG_ENDPOINT_BASE_URI)
public class ExchangeConfigController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String EXCHANGE_RESOURCE_PATH = "/exchange";
  private final ExchangeConfigService exchangeConfigService;

  public ExchangeConfigController(ExchangeConfigService exchangeConfigService) {
    this.exchangeConfigService = exchangeConfigService;
  }

  /**
   * Returns the Exchange configuration for the bot.
   *
   * <p>The AuthenticationConfig is stripped out and not exposed for remote consumption. The API
   * keys/credentials should not leave the bot's local machine via the REST API.
   *
   * @param principal the authenticated user making the request.
   * @return the Exchange configuration.
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = EXCHANGE_RESOURCE_PATH)
  public ExchangeConfig getExchange(@ApiIgnore Principal principal) {

    LOG.info(
        () ->
            "GET " + EXCHANGE_RESOURCE_PATH + " - getExchange() - caller: " + principal.getName());

    final ExchangeConfig exchangeConfig = exchangeConfigService.getExchangeConfig();
    exchangeConfig.setAuthenticationConfig(null);
    LOG.info(() -> "Response: " + exchangeConfig);
    return exchangeConfig;
  }

  /**
   * Updates the Exchange configuration for the bot.
   *
   * <p>Any AuthenticationConfig is stripped out and not updated. The API keys/credentials should
   * not enter the bot's local machine via the REST API.
   *
   * @param principal the authenticated user making the request.
   * @param config the Exchange config to update.
   * @return 200 'OK' HTTP status code with updated Exchange config in the body if update
   *     successful, some other HTTP status code otherwise.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping(value = EXCHANGE_RESOURCE_PATH)
  public ResponseEntity<ExchangeConfig> updateExchange(
      @ApiIgnore Principal principal, @RequestBody ExchangeConfig config) {

    LOG.info(
        () ->
            "PUT "
                + EXCHANGE_RESOURCE_PATH
                + " - updateExchange() - caller: "
                + principal.getName());

    LOG.info(() -> "Request: " + config);

    final ExchangeConfig updatedConfig =
        exchangeConfigService.updateExchangeConfig(mergeWithLocalAuthenticationConfig(config));
    return buildResponseEntity(updatedConfig);
  }

  // ------------------------------------------------------------------------
  // Private utils
  // ------------------------------------------------------------------------

  private ExchangeConfig mergeWithLocalAuthenticationConfig(ExchangeConfig remoteConfig) {
    final ExchangeConfig localConfig = exchangeConfigService.getExchangeConfig();
    remoteConfig.setAuthenticationConfig(localConfig.getAuthenticationConfig());
    return remoteConfig;
  }

  private ResponseEntity<ExchangeConfig> buildResponseEntity(ExchangeConfig entity) {
    LOG.info(() -> "Response: " + entity);
    return new ResponseEntity<>(entity, null, HttpStatus.OK);
  }
}
