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
import com.gazbert.bxbot.services.config.EngineConfigService;
import io.swagger.annotations.Api;
import java.security.Principal;
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
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for directing Engine config requests.
 *
 * <p>Engine config can only be fetched and updated - it cannot be deleted or created.
 *
 * <p>There is only 1 Trading Engine per bot.
 *
 * @author gazbert
 * @since 1.0
 */
@Api(tags = {"Engine Configuration"})
@RestController
@RequestMapping(CONFIG_ENDPOINT_BASE_URI)
public class EngineConfigController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String ENGINE_RESOURCE_PATH = "/engine";
  private final EngineConfigService engineConfigService;

  @Autowired
  public EngineConfigController(EngineConfigService engineConfigService) {
    this.engineConfigService = engineConfigService;
  }

  /**
   * Returns the Engine configuration for the bot.
   *
   * @param principal the authenticated user making the request.
   * @return the Engine configuration.
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = ENGINE_RESOURCE_PATH)
  public EngineConfig getEngine(@ApiIgnore Principal principal) {

    LOG.info(
        () -> "GET " + ENGINE_RESOURCE_PATH + " - getEngine() - caller: " + principal.getName());

    final EngineConfig engineConfig = engineConfigService.getEngineConfig();
    LOG.info(() -> "Response: " + engineConfig);
    return engineConfig;
  }

  /**
   * Updates the Engine configuration for the bot.
   *
   * @param principal the authenticated user making the request.
   * @param config the Engine config to update.
   * @return 200 'OK' HTTP status code and updated Engine config in the response body if update
   *     successful, some other HTTP status code otherwise.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping(value = ENGINE_RESOURCE_PATH)
  public ResponseEntity<EngineConfig> updateEngine(
      @ApiIgnore Principal principal, @RequestBody EngineConfig config) {

    LOG.info(
        () -> "PUT " + ENGINE_RESOURCE_PATH + " - updateEngine() - caller: " + principal.getName());

    LOG.info(() -> "Request: " + config);

    final EngineConfig updatedConfig = engineConfigService.updateEngineConfig(config);
    return buildResponseEntity(updatedConfig);
  }

  private ResponseEntity<EngineConfig> buildResponseEntity(EngineConfig entity) {
    LOG.info(() -> "Response: " + entity);
    return new ResponseEntity<>(entity, null, HttpStatus.OK);
  }
}
