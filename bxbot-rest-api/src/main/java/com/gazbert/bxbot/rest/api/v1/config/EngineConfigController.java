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

import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.rest.api.v1.RestController;
import com.gazbert.bxbot.services.config.EngineConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import javax.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for directing Engine config requests.
 * 用于指导引擎配置请求的控制器。
 *
 * <p>Engine config can only be fetched and updated - it cannot be deleted or created.
 * * <p>引擎配置只能被获取和更新 - 它不能被删除或创建。
 *
 * <p>There is only 1 Trading Engine per bot.
 * <p>每个机器人只有 1 个交易引擎。
 *
 * @author gazbert
 * @since 1.0
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping(CONFIG_ENDPOINT_BASE_URI)
@Tag(name = "Engine Configuration")
public class EngineConfigController implements RestController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String ENGINE_RESOURCE_PATH = "/engine";
  private final EngineConfigService engineConfigService;

  @Autowired
  public EngineConfigController(EngineConfigService engineConfigService) {
    this.engineConfigService = engineConfigService;
  }

  /**
   * Returns the Engine configuration for the bot.
   * 返回机器人的引擎配置。
   *
   * @param principal the authenticated user making the request.
   *                  发出请求的经过身份验证的用户。
   *
   * @return the Engine configuration. 引擎配置。
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = ENGINE_RESOURCE_PATH)
  @Operation(summary = "Fetches Engine config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EngineConfig.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public EngineConfig getEngine(@Parameter(hidden = true) Principal principal) {

    LOG.info(
        () -> "GET " + ENGINE_RESOURCE_PATH + " - getEngine() - caller: getEngine() - 调用者：" + principal.getName());

    final EngineConfig engineConfig = engineConfigService.getEngineConfig();
    LOG.info(() -> "Response: 响应：" + engineConfig);
    return engineConfig;
  }

  /**
   * Updates the Engine configuration for the bot.
   * 更新机器人的引擎配置。
   *
   * @param principal the authenticated user making the request.
   *                  发出请求的经过身份验证的用户。
   *
   * @param config the Engine config to update.
   *               要更新的引擎配置。
   * @return 200 'OK' HTTP status code and updated Engine config in the response body if update  successful, some other HTTP status code otherwise.
   * @return 200 'OK' HTTP 状态代码，如果更新成功，则在响应正文中更新引擎配置，否则返回一些其他 HTTP 状态代码。
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping(value = ENGINE_RESOURCE_PATH)
  @Operation(summary = "Updates Engine config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EngineConfig.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<EngineConfig> updateEngine(
      @Parameter(hidden = true) Principal principal, @Valid @RequestBody EngineConfig config) {

    LOG.info(
        () -> "PUT " + ENGINE_RESOURCE_PATH + " - updateEngine() - caller: updateEngine() - 调用者：" + principal.getName());

    LOG.info(() -> "Request: 请求： " + config);

    final EngineConfig updatedConfig = engineConfigService.updateEngineConfig(config);
    return buildResponseEntity(updatedConfig);
  }

  private ResponseEntity<EngineConfig> buildResponseEntity(EngineConfig entity) {
    LOG.info(() -> "Response: 响应：" + entity);
    return new ResponseEntity<>(entity, null, HttpStatus.OK);
  }
}
