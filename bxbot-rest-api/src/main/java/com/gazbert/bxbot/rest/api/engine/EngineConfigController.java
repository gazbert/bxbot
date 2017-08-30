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

package com.gazbert.bxbot.rest.api.engine;

import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.services.EngineConfigService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Controller for directing Engine config requests.
 * <p>
 * Engine config can only be fetched and updated - there is only 1 Trading Engine per bot.
 *
 * @author gazbert
 * @since 1.0
 */
@RestController
@RequestMapping("/api/config")
class EngineConfigController {

    private static final Logger LOG = LogManager.getLogger();
    private final EngineConfigService engineConfigService;

    @Autowired
    public EngineConfigController(EngineConfigService engineConfigService) {
        Assert.notNull(engineConfigService, "engineConfigService dependency cannot be null!");
        this.engineConfigService = engineConfigService;
    }

    /**
     * Returns Engine configuration for the bot.
     *
     * @return the Engine configuration.
     */
    @RequestMapping(value = "/engine", method = RequestMethod.GET)
    public EngineConfig getEngine(@AuthenticationPrincipal User user) {

        LOG.info("GET /engine - getEngine() - caller: " + user.getUsername());

        final EngineConfig engineConfig = engineConfigService.getEngineConfig();
        LOG.info("Response: " + engineConfig);

        return engineConfig;
    }

    /**
     * Updates Engine configuration for the bot.
     *
     * @return 204 'No Content' HTTP status code if engine config was updated successfully, some other HTTP status
     * code otherwise.
     */
    @RequestMapping(value = "/engine", method = RequestMethod.PUT)
    public ResponseEntity<?> updateEngine(@AuthenticationPrincipal User user, @RequestBody EngineConfig config) {

        LOG.info("PUT /engine - updateEngine() - caller: " + user.getUsername());
        LOG.info("Request: " + config);

        engineConfigService.updateEngineConfig(config);
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().path("/").buildAndExpand().toUri());
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.NO_CONTENT);
    }
}

