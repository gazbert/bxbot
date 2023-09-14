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

package com.gazbert.bxbot.services.runtime.impl;

import com.gazbert.bxbot.services.runtime.BotRestartService;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.stereotype.Service;

/**
 * Implementation of the Bot restart service.
 *
 * @author gazbert
 */
@Service("botRestartService")
@Log4j2
public class BotRestartServiceImpl implements BotRestartService {

  private final RestartEndpoint restartEndpoint;

  /**
   * Constructs the BotRestartService.
   *
   * @param restartEndpoint the Restart Endpoint
   */
  @Autowired
  public BotRestartServiceImpl(RestartEndpoint restartEndpoint) {
    this.restartEndpoint = restartEndpoint;
  }

  @Override
  public String restart() {
    // Spring endpoint returns a map: Collections.singletonMap("message", "Restarting");
    final Map result = (Map) restartEndpoint.restart();
    final String status = (String) result.get("message");
    log.info("Restart result: " + status);
    return status;
  }
}
