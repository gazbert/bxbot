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
import com.gazbert.bxbot.rest.api.v1.RestController;
import com.gazbert.bxbot.services.config.MarketConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import lombok.extern.log4j.Log4j2;
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

/**
 * Controller for directing Market config requests.
 *
 * @author gazbert
 * @since 1.0
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping(CONFIG_ENDPOINT_BASE_URI)
@Tag(name = "Market Configuration")
@Log4j2
public class MarketConfigController extends RestController {

  private static final String MARKETS_RESOURCE_PATH = "/markets";
  private final MarketConfigService marketConfigService;

  /**
   * Creates the MarketConfigController.
   *
   * @param marketConfigService the market config service.
   */
  @Autowired
  public MarketConfigController(MarketConfigService marketConfigService) {
    this.marketConfigService = marketConfigService;
  }

  /**
   * Returns all the Market configuration for the bot.
   *
   * @param principal the authenticated user.
   * @return all the Market configurations.
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = MARKETS_RESOURCE_PATH)
  @Operation(summary = "Fetches all Market config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = MarketConfig.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public List<MarketConfig> getAllMarkets(@Parameter(hidden = true) Principal principal) {

    log.info(
        "GET " + MARKETS_RESOURCE_PATH + " - getAllMarkets() - caller: {}", principal.getName());

    final List<MarketConfig> marketConfigs = marketConfigService.getAllMarketConfig();
    log.info("Response: {}", marketConfigs);

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
  @Operation(summary = "Fetches a Market config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = MarketConfig.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<MarketConfig> getMarket(
      @Parameter(hidden = true) Principal principal, @PathVariable String marketId) {

    log.info(
        "GET " + MARKETS_RESOURCE_PATH + "/{} - getMarket() - caller: {}",
        marketId,
        principal.getName());

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
   * @return 200 'OK' HTTP status code if update successful, 404 'Not Found' HTTP status code if
   *     Market config not found.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping(value = MARKETS_RESOURCE_PATH + "/{marketId}")
  @Operation(summary = "Updates a Market config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = MarketConfig.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<MarketConfig> updateMarket(
      @Parameter(hidden = true) Principal principal,
      @PathVariable String marketId,
      @RequestBody MarketConfig config) {

    log.info(
        "PUT " + MARKETS_RESOURCE_PATH + "/{} - updateMarket() - caller: {}",
        marketId,
        principal.getName());

    log.info("Request: {}", config);

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
  @Operation(summary = "Creates a Market config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(schema = @Schema(implementation = MarketConfig.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<MarketConfig> createMarket(
      @Parameter(hidden = true) Principal principal, @RequestBody MarketConfig config) {

    log.info(
        "POST " + MARKETS_RESOURCE_PATH + " - createMarket() - caller: {}", principal.getName());

    log.info("Request: {}", config);

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
  @Operation(summary = "Deletes a Market config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "No Content",
            content = @Content(schema = @Schema(implementation = MarketConfig.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<MarketConfig> deleteMarket(
      @Parameter(hidden = true) Principal principal, @PathVariable String marketId) {

    log.info(
        "DELETE " + MARKETS_RESOURCE_PATH + "/{} - deleteMarket() - caller: {}",
        marketId,
        principal.getName());

    final MarketConfig deletedConfig = marketConfigService.deleteMarketConfig(marketId);
    return deletedConfig == null
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  // ------------------------------------------------------------------------
  // Private utils
  // ------------------------------------------------------------------------

  private ResponseEntity<MarketConfig> buildResponseEntity(MarketConfig entity, HttpStatus status) {
    log.info("Response: {}", entity);
    return new ResponseEntity<>(entity, null, status);
  }
}
