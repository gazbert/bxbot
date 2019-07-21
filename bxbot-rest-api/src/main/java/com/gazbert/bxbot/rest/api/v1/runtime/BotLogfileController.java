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

import com.gazbert.bxbot.services.runtime.BotLogfileService;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for directing Bot Logfile requests.
 *
 * @author gazbert
 * @since 1.0
 */
@RestController
@RequestMapping(RUNTIME_ENDPOINT_BASE_URI)
public class BotLogfileController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String LOGFILE_RESOURCE_PATH = "/logfile";

  private final BotLogfileService botLogfileService;

  @Autowired
  public BotLogfileController(BotLogfileService botLogfileService) {
    this.botLogfileService = botLogfileService;
  }

  /**
   * Returns the logfile for the bot.
   *
   * <p>If the file is larger than {@link BotLogfileService#MAX_LOGFILE_FETCH_SIZE_IN_BYTES}, the
   * end of the log will be truncated.
   *
   * @param user the authenticated user making the request.
   * @return the logfile.
   */
  @GetMapping(value = LOGFILE_RESOURCE_PATH)
  public ResponseEntity<String> getLogfile(@AuthenticationPrincipal User user) {

    LOG.info(
        () -> "GET " + LOGFILE_RESOURCE_PATH + " - getLogfile() - caller: " + user.getUsername());

    String logfile;
    try {
      logfile = botLogfileService.getLogfile();
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(logfile, null, HttpStatus.OK);
  }
}
