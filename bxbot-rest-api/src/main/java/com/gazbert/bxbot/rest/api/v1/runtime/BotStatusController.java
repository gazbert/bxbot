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

package com.gazbert.bxbot.rest.api.v1.runtime;

import static com.gazbert.bxbot.rest.api.v1.EndpointLocations.RUNTIME_ENDPOINT_BASE_URI;

import com.gazbert.bxbot.domain.bot.BotStatus;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.rest.api.v1.RestController;
import com.gazbert.bxbot.services.config.EngineConfigService;
import com.gazbert.bxbot.services.runtime.BotStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.Date;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for directing Bot Status requests.
 *
 * @author gazbert
 * @since 1.0
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping(RUNTIME_ENDPOINT_BASE_URI)
@Tag(name = "Bot Status")
@Log4j2
public class BotStatusController extends RestController {

  private static final String STATUS_RESOURCE_PATH = "/status";

  private final BotStatusService botStatusService;
  private final EngineConfigService engineConfigService;

  /**
   * Creates the BotStatusController.
   *
   * @param botStatusService the bot status service.
   * @param engineConfigService the engine config service.
   */
  @Autowired
  public BotStatusController(
      BotStatusService botStatusService, EngineConfigService engineConfigService) {
    this.botStatusService = botStatusService;
    this.engineConfigService = engineConfigService;
  }

  /**
   * Returns the process status for the bot.
   *
   * @param principal the authenticated user making the request.
   * @return the process status.
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = STATUS_RESOURCE_PATH)
  @Operation(summary = "Fetches the bot status")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = BotStatus.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public BotStatus getStatus(@Parameter(hidden = true) Principal principal) {

    log.info("GET " + STATUS_RESOURCE_PATH + " - getStatus() - caller: {}", principal.getName());

    final EngineConfig engineConfig = engineConfigService.getEngineConfig();
    final String status = botStatusService.getStatus();

    final BotStatus botStatus = new BotStatus();
    botStatus.setBotId(engineConfig.getBotId());
    botStatus.setDisplayName(engineConfig.getBotName());
    botStatus.setStatus(status);
    botStatus.setDatetime(new Date());

    log.info("Response: {}", botStatus);
    return botStatus;
  }
}
