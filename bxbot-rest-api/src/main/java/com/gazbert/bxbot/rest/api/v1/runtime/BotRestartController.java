/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
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

package com.gazbert.bxbot.rest.api.v1.runtime;

import static com.gazbert.bxbot.rest.api.v1.EndpointLocations.RUNTIME_ENDPOINT_BASE_URI;

import com.gazbert.bxbot.rest.api.v1.RestController;
import com.gazbert.bxbot.services.runtime.BotRestartService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for directing Bot restart requests.
 * 用于指导 Bot 重启请求的操作。
 *
 * @author gazbert
 * @since 1.0
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping(RUNTIME_ENDPOINT_BASE_URI)
@Tag(name = "Bot Restart")
public class BotRestartController implements RestController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String RESTART_RESOURCE_PATH = "/restart";
  private final BotRestartService botRestartService;

  @Autowired
  public BotRestartController(BotRestartService botRestartService) {
    this.botRestartService = botRestartService;
  }

  /**
   * Restarts the bot.
   * 重新启动机器人。
   *
   * @param principal the authenticated user making the request.
   *                  * @param principal 发出请求的经过身份验证的用户。
   *
   * @return 200 OK on success with 'Restarting' response on success, some other HTTP status code  otherwise.
   * @return 200 OK 成功，成功时“重新启动”响应，否则返回一些其他 HTTP 状态代码。
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(value = RESTART_RESOURCE_PATH)
  @Operation(summary = "Restarts the bot")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<String> restart(@Parameter(hidden = true) Principal principal) {

    LOG.info(
        () -> "POST " + RESTART_RESOURCE_PATH + " - restart() - caller: restart() - 调用者：" + principal.getName());

    final Object status = botRestartService.restart();

    LOG.info(() -> "Response: 响应：" + status.toString());
    return new ResponseEntity<>(status.toString(), null, HttpStatus.OK);
  }
}
