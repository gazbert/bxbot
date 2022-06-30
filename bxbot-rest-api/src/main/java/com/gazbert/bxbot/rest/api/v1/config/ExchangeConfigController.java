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
import com.gazbert.bxbot.rest.api.v1.RestController;
import com.gazbert.bxbot.services.config.ExchangeConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Controller for directing Exchange config requests.
 * 用于指导 Exchange 配置请求的控制器。
 *
 * <p>Exchange config can only be fetched and updated - it cannot be deleted or created.
 * * <p>Exchange 配置只能被获取和更新 - 它不能被删除或创建。
 *
 * <p>There is only 1 Exchange Adapter per bot.
 * <p>每个机器人只有 1 个交换适配器。
 *
 * @author gazbert
 * @since 1.0
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping(CONFIG_ENDPOINT_BASE_URI)
@Tag(name = "Exchange Configuration")
public class ExchangeConfigController implements RestController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String EXCHANGE_RESOURCE_PATH = "/exchange";
  private final ExchangeConfigService exchangeConfigService;

  public ExchangeConfigController(ExchangeConfigService exchangeConfigService) {
    this.exchangeConfigService = exchangeConfigService;
  }

  /**
   * Returns the Exchange configuration for the bot.
   * 返回机器人的 Exchange 配置。
   *
   * <p>The AuthenticationConfig is stripped out and not exposed for remote consumption. The API
    keys/credentials should not leave the bot's local machine via the REST API.
   <p>AuthenticationConfig 已被剥离，不会公开以供远程使用。 API
   密钥/凭据不应通过 REST API 离开机器人的本地计算机。
   *
   * @param principal the authenticated user making the request.
   *                  发出请求的经过身份验证的用户。
   *
   * @return the Exchange configuration.
   *              交换配置。
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = EXCHANGE_RESOURCE_PATH)
  @Operation(summary = "Fetches Exchange config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ExchangeConfig.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ExchangeConfig getExchange(@Parameter(hidden = true) Principal principal) {

    LOG.info(
        () ->
            "GET " + EXCHANGE_RESOURCE_PATH + " - getExchange() - caller: getExchange() - 调用者：" + principal.getName());

    final ExchangeConfig exchangeConfig = exchangeConfigService.getExchangeConfig();
    exchangeConfig.setAuthenticationConfig(null);
    LOG.info(() -> "Response: 响应： " + exchangeConfig);
    return exchangeConfig;
  }

  /**
   * Updates the Exchange configuration for the bot.
   * 更新机器人的 Exchange 配置。
   *
   * <p>Any AuthenticationConfig is stripped out and not updated. The API keys/credentials should not enter the bot's local machine via the REST API.
   *  * <p>任何 AuthenticationConfig 都会被删除并且不会更新。 API 密钥/凭据不应通过 REST API 进入机器人的本地计算机。
   *
   * @param principal the authenticated user making the request.
   *                  发出请求的经过身份验证的用户。
   *
   * @param config the Exchange config to update.
   *               要更新的 Exchange 配置。
   *
   * @return 200 'OK' HTTP status code with updated Exchange config in the body if update  successful, some other HTTP status code otherwise.
   *      @return 200 'OK' HTTP 状态代码，如果更新成功，则在正文中更新 Exchange 配置，否则返回一些其他 HTTP 状态代码。
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping(value = EXCHANGE_RESOURCE_PATH)
  @Operation(summary = "Updates Exchange config")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ExchangeConfig.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<ExchangeConfig> updateExchange(
      @Parameter(hidden = true) Principal principal, @RequestBody ExchangeConfig config) {

    LOG.info(
        () ->
            "PUT "
                + EXCHANGE_RESOURCE_PATH
                + " - updateExchange() - caller: - updateExchange() - 调用者："
                + principal.getName());

    LOG.info(() -> "Request: 请求： " + config);

    final ExchangeConfig updatedConfig =
        exchangeConfigService.updateExchangeConfig(mergeWithLocalAuthenticationConfig(config));
    return buildResponseEntity(updatedConfig);
  }

  // ------------------------------------------------------------------------
  // Private utils  // 私有工具
  // ------------------------------------------------------------------------

  private ExchangeConfig mergeWithLocalAuthenticationConfig(ExchangeConfig remoteConfig) {
    final ExchangeConfig localConfig = exchangeConfigService.getExchangeConfig();
    remoteConfig.setAuthenticationConfig(localConfig.getAuthenticationConfig());
    return remoteConfig;
  }

  private ResponseEntity<ExchangeConfig> buildResponseEntity(ExchangeConfig entity) {
    LOG.info(() -> "Response: 响应：" + entity);
    return new ResponseEntity<>(entity, null, HttpStatus.OK);
  }
}
