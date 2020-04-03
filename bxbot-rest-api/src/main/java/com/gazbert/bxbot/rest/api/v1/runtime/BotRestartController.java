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

import com.gazbert.bxbot.services.runtime.BotRestartService;
import io.swagger.annotations.Api;
import java.security.Principal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for directing Bot restart requests.
 *
 * @author gazbert
 * @since 1.0
 */
@Api(tags = {"Bot Restart"})
@RestController
@RequestMapping(RUNTIME_ENDPOINT_BASE_URI)
public class BotRestartController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String RESTART_RESOURCE_PATH = "/restart";
  private final BotRestartService botRestartService;

  @Autowired
  public BotRestartController(BotRestartService botRestartService) {
    this.botRestartService = botRestartService;
  }

  /**
   * Restarts the bot.
   *
   * @param principal the authenticated user making the request.
   * @return 200 OK on success with 'Restarting' response on success, some other HTTP status code
   *     otherwise.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(value = RESTART_RESOURCE_PATH)
  public ResponseEntity<String> restart(@ApiIgnore Principal principal) {

    LOG.info(
        () -> "POST " + RESTART_RESOURCE_PATH + " - restart() - caller: " + principal.getName());

    final Object status = botRestartService.restart();

    LOG.info(() -> "Response: " + status.toString());
    return new ResponseEntity<>(status.toString(), null, HttpStatus.OK);
  }
}
