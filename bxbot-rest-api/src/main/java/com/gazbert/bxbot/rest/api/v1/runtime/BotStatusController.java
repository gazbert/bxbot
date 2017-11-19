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

import com.gazbert.bxbot.domain.bot.BotStatus;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.services.EngineConfigService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static com.gazbert.bxbot.rest.api.v1.runtime.AbstractRuntimeController.RUNTIME_ENDPOINT_BASE_URI;

/**
 * Controller for directing Bot Status requests.
 *
 * @author gazbert
 * @since 1.0
 */
@RestController
@RequestMapping(RUNTIME_ENDPOINT_BASE_URI)
public class BotStatusController extends AbstractRuntimeController {

    private static final Logger LOG = LogManager.getLogger();
    private static final String STATUS_RESOURCE_PATH = "/status";
    private final EngineConfigService engineConfigService;

    @Autowired
    public BotStatusController(EngineConfigService engineConfigService) {
        this.engineConfigService = engineConfigService;
    }

    /**
     * Returns the process status for the bot.
     *
     * @param user the authenticated user making the request.
     * @return the process status.
     */
    @RequestMapping(value = STATUS_RESOURCE_PATH, method = RequestMethod.GET)
    public BotStatus getStatus(@AuthenticationPrincipal User user) {

        LOG.info("GET " + STATUS_RESOURCE_PATH + " - getStatus() - caller: " + user.getUsername());

        final EngineConfig engineConfig = engineConfigService.getEngineConfig();

        // TODO - hacked up for now until work properly starts on runtime features ;-)
        final BotStatus botStatus = new BotStatus();
        botStatus.setBotId(engineConfig.getBotId());
        botStatus.setDisplayName(engineConfig.getBotName());
        botStatus.setStatus("running"); // use enum for defining states at some point

        LOG.info("Response: " + botStatus);
        return botStatus;
    }
}

