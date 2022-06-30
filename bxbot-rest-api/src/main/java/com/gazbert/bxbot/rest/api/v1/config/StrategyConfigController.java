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

import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.rest.api.v1.RestController;
import com.gazbert.bxbot.services.config.StrategyConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Controller for directing Strategy config requests.
 * 用于指导策略配置请求的控制器。
 *
 * @author gazbert
 * @since 1.0
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping(CONFIG_ENDPOINT_BASE_URI)
@Tag(name = "Strategy Configuration")
public class StrategyConfigController implements RestController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String STRATEGIES_RESOURCE_PATH = "/strategies";
  private final StrategyConfigService strategyConfigService;

  @Autowired
  public StrategyConfigController(StrategyConfigService strategyConfigService) {
    this.strategyConfigService = strategyConfigService;
  }

  /**
   * Returns all of the Strategy configuration for the bot.
   * 返回机器人的所有策略配置。
   *
   * @param principal the authenticated user.  经过身份验证的用户。
   * @return all the Strategy configurations.  所有策略配置。
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = STRATEGIES_RESOURCE_PATH)
  @Operation(summary = "Fetches all Strategy config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = StrategyConfig.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public List<StrategyConfig> getAllStrategies(@Parameter(hidden = true) Principal principal) {

    LOG.info(
        () ->
            "GET "
                + STRATEGIES_RESOURCE_PATH
                + " - getAllStrategies() - caller: - getAllStrategies() - 调用者："
                + principal.getName());

    final List<StrategyConfig> strategyConfigs = strategyConfigService.getAllStrategyConfig();

    LOG.info(() -> "Response: 响应:" + strategyConfigs);
    return strategyConfigs;
  }

  /**
   * Returns the Strategy configuration for a given id.
   * 返回给定 id 的策略配置。
   *
   * @param principal the authenticated user.  经过身份验证的用户。
   * @param strategyId the id of the Strategy to fetch. 要获取的策略的 id。
   * @return the Strategy configuration.  策略配置。
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = STRATEGIES_RESOURCE_PATH + "/{strategyId}")
  @Operation(summary = "Fetches a Strategy config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = StrategyConfig.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<StrategyConfig> getStrategy(
      @Parameter(hidden = true) Principal principal, @PathVariable String strategyId) {

    LOG.info(
        () ->
            "GET "
                + STRATEGIES_RESOURCE_PATH
                + "/"
                + strategyId
                + " - getStrategy() - caller: - getStrategy() - 调用者："
                + principal.getName());

    final StrategyConfig strategyConfig = strategyConfigService.getStrategyConfig(strategyId);
    return strategyConfig == null
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : buildResponseEntity(strategyConfig, HttpStatus.OK);
  }

  /**
   * Updates a given Strategy configuration.
   * 更新给定的策略配置。
   *
   * @param principal the authenticated user.
   *                    经过身份验证的用户。
   *
   * @param strategyId id of the Strategy config to update.
   *                   要更新的策略配置的 ID。
   *
   * @param config the updated Strategy config.
   *               更新的策略配置。
   *
   * @return 200 'OK' HTTP status code and updated Strategy config in the body if update successful, 404 'Not Found' HTTP status code if Strategy config not found.
   * @return 200 'OK' HTTP 状态码和更新的策略配置如果更新成功，404 'Not Found' HTTP 状态码如果没有找到策略配置。
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping(value = STRATEGIES_RESOURCE_PATH + "/{strategyId}")
  @Operation(summary = "Updates a Strategy config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = StrategyConfig.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<StrategyConfig> updateStrategy(
      @Parameter(hidden = true) Principal principal,
      @PathVariable String strategyId,
      @RequestBody StrategyConfig config) {

    LOG.info(
        () ->
            "PUT "
                + STRATEGIES_RESOURCE_PATH
                + "/"
                + strategyId
                + " - updateStrategy() - caller: - updateStrategy() - 调用者："
                + principal.getName());

    LOG.info(() -> "Request: 请求：" + config);

    if (config.getId() == null || !strategyId.equals(config.getId())) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    final StrategyConfig updatedConfig = strategyConfigService.updateStrategyConfig(config);
    return updatedConfig == null
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : buildResponseEntity(updatedConfig, HttpStatus.OK);
  }

  /**
   * Creates a new Strategy configuration.
   * 创建一个新的策略配置。
   *
   * @param principal the authenticated user.
   *                  经过身份验证的用户。
   *
   * @param config the new Strategy config.
   *               新的策略配置。
   *
   * @return 201 'Created' HTTP status code and created Strategy config in response body if create  successful, some other status code otherwise.
   * @return 201 'Created' HTTP 状态码，如果创建成功，则在响应正文中创建策略配置，否则为其他状态码。
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(value = STRATEGIES_RESOURCE_PATH)
  @Operation(summary = "Creates a Strategy config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(schema = @Schema(implementation = StrategyConfig.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<StrategyConfig> createStrategy(
      @Parameter(hidden = true) Principal principal, @RequestBody StrategyConfig config) {

    LOG.info(
        () ->
            "POST "
                + STRATEGIES_RESOURCE_PATH
                + " - createStrategy() - caller: - createStrategy() - 调用者："
                + principal.getName());

    LOG.info(() -> "Request: 请求：" + config);

    final StrategyConfig createdConfig = strategyConfigService.createStrategyConfig(config);
    return createdConfig == null
        ? new ResponseEntity<>(HttpStatus.BAD_REQUEST)
        : buildResponseEntity(createdConfig, HttpStatus.CREATED);
  }

  /**
   * Deletes a Strategy configuration for a given id.
   * 删除给定 id 的策略配置。
   *
   * @param principal the authenticated user.
   *                  经过身份验证的用户。
   *
   * @param strategyId the id of the Strategy configuration to delete.
   *                   要删除的策略配置的 id。
   *
   * @return 204 'No Content' HTTP status code if delete successful, 404 'Not Found' HTTP status code if Strategy config not found.
   * @return 204 'No Content' HTTP 状态码，如果删除成功，404 'Not Found' HTTP 状态码，如果没有找到策略配置。
   */
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping(value = STRATEGIES_RESOURCE_PATH + "/{strategyId}")
  @Operation(summary = "Deletes a Strategy config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "No Content",
            content = @Content(schema = @Schema(implementation = StrategyConfig.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<StrategyConfig> deleteStrategy(
      @Parameter(hidden = true) Principal principal, @PathVariable String strategyId) {

    LOG.info(
        () ->
            "DELETE "
                + STRATEGIES_RESOURCE_PATH
                + "/"
                + strategyId
                + " - deleteStrategy() - caller: - deleteStrategy() - 调用者："
                + principal.getName());

    final StrategyConfig deletedConfig = strategyConfigService.deleteStrategyConfig(strategyId);
    return deletedConfig == null
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  // ------------------------------------------------------------------------
  // Private utils  // 私有工具
  // ------------------------------------------------------------------------

  private ResponseEntity<StrategyConfig> buildResponseEntity(
      StrategyConfig entity, HttpStatus status) {
    LOG.info(() -> "Response: 响应：" + entity);
    return new ResponseEntity<>(entity, null, status);
  }
}
