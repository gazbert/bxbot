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

import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.services.config.MarketConfigService;
import io.swagger.annotations.Api;
import java.security.Principal;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for directing Market config requests.
 *
 * @author gazbert
 * @since 1.0
 */
@Api(tags = {"Market Configuration"})
@RestController
@RequestMapping(CONFIG_ENDPOINT_BASE_URI)
public class MarketConfigController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String MARKETS_RESOURCE_PATH = "/markets";
  private final MarketConfigService marketConfigService;

  @Autowired
  public MarketConfigController(MarketConfigService marketConfigService) {
    this.marketConfigService = marketConfigService;
  }

  /**
   * Returns all of the Market configuration for the bot.
   *
   * @param principal the authenticated user.
   * @return all the Market configurations.
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = MARKETS_RESOURCE_PATH)
  public List<MarketConfig> getAllMarkets(@ApiIgnore Principal principal) {

    LOG.info(
        () ->
            "GET " + MARKETS_RESOURCE_PATH + " - getAllMarkets() - caller: " + principal.getName());

    final List<MarketConfig> marketConfigs = marketConfigService.getAllMarketConfig();
    LOG.info(() -> "Response: " + marketConfigs);

    return marketConfigs;
  }

  /**
   * Returns the Market configuration for a given id.
   *
   * @param principal the authenticated user.
   * @param marketId the id of the Market to fetch.
   * @return the Market configuration.
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = MARKETS_RESOURCE_PATH + "/{marketId}")
  public ResponseEntity<MarketConfig> getMarket(
      @ApiIgnore Principal principal, @PathVariable String marketId) {

    LOG.info(
        () ->
            "GET "
                + MARKETS_RESOURCE_PATH
                + "/"
                + marketId
                + " - getMarket() - caller: "
                + principal.getName());

    final MarketConfig marketConfig = marketConfigService.getMarketConfig(marketId);
    return marketConfig == null
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : buildResponseEntity(marketConfig, HttpStatus.OK);
  }

  /**
   * Updates a given Market configuration.
   *
   * @param principal the authenticated user.
   * @param marketId id of the Market config to update.
   * @param config the updated Market config.
   * @return 204 'No Content' HTTP status code if update successful, 404 'Not Found' HTTP status
   *     code if Market config not found.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping(value = MARKETS_RESOURCE_PATH + "/{marketId}")
  public ResponseEntity<MarketConfig> updateMarket(
      @ApiIgnore Principal principal,
      @PathVariable String marketId,
      @RequestBody MarketConfig config) {

    LOG.info(
        () ->
            "PUT "
                + MARKETS_RESOURCE_PATH
                + "/"
                + marketId
                + " - updateMarket() - caller: "
                + principal.getName());

    LOG.info(() -> "Request: " + config);

    if (config.getId() == null || !marketId.equals(config.getId())) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    final MarketConfig updatedConfig = marketConfigService.updateMarketConfig(config);
    return updatedConfig == null
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : buildResponseEntity(updatedConfig, HttpStatus.OK);
  }

  /**
   * Creates a new Market configuration.
   *
   * @param principal the authenticated user.
   * @param config the new Market config.
   * @return 201 'Created' HTTP status code and created Market config in response body if create
   *     successful, some other HTTP status code otherwise.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(value = MARKETS_RESOURCE_PATH)
  public ResponseEntity<MarketConfig> createMarket(
      @ApiIgnore Principal principal, @RequestBody MarketConfig config) {

    LOG.info(
        () ->
            "POST " + MARKETS_RESOURCE_PATH + " - createMarket() - caller: " + principal.getName());

    LOG.info(() -> "Request: " + config);

    final MarketConfig createdConfig = marketConfigService.createMarketConfig(config);
    return createdConfig == null
        ? new ResponseEntity<>(HttpStatus.BAD_REQUEST)
        : buildResponseEntity(createdConfig, HttpStatus.CREATED);
  }

  /**
   * Deletes a Market configuration for a given id.
   *
   * @param principal the authenticated user.
   * @param marketId the id of the Market configuration to delete.
   * @return 204 'No Content' HTTP status code if delete successful, 404 'Not Found' HTTP status
   *     code if Market config not found.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping(value = MARKETS_RESOURCE_PATH + "/{marketId}")
  public ResponseEntity<MarketConfig> deleteMarket(
      @ApiIgnore Principal principal, @PathVariable String marketId) {

    LOG.info(
        () ->
            "DELETE "
                + MARKETS_RESOURCE_PATH
                + "/"
                + marketId
                + " - deleteMarket() - caller: "
                + principal.getName());

    final MarketConfig deletedConfig = marketConfigService.deleteMarketConfig(marketId);
    return deletedConfig == null
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  // ------------------------------------------------------------------------
  // Private utils
  // ------------------------------------------------------------------------

  private ResponseEntity<MarketConfig> buildResponseEntity(MarketConfig entity, HttpStatus status) {
    LOG.info(() -> "Response: " + entity);
    return new ResponseEntity<>(entity, null, status);
  }
}
