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

package com.gazbert.bxbot.core.admin.controllers;

import com.gazbert.bxbot.core.admin.services.StrategyConfigService;
import com.gazbert.bxbot.core.config.strategy.StrategyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * TODO Javadoc this - it's a public API!
 * <p>
 * Controller for directing Strategy config requests.
 *
 * @author gazbert
 * @since 12/08/2016
 */
@RestController
@RequestMapping("/api")
public class StrategyConfigController {

    private final StrategyConfigService strategyConfigService;

    @Autowired
    public StrategyConfigController(StrategyConfigService strategyConfigService) {
        Assert.notNull(strategyConfigService, "strategyConfigService dependency cannot be null!");
        this.strategyConfigService = strategyConfigService;
    }

    @RequestMapping(value = "/config/strategy", method = RequestMethod.GET)
    public List<StrategyConfig> getAllStrategies() {
        return strategyConfigService.findAllStrategies();
    }

    @RequestMapping(value = "/config/strategy/{strategyId}", method = RequestMethod.GET)
    public ResponseEntity<?> getStrategy(@PathVariable String strategyId) {

        final StrategyConfig strategyConfig = strategyConfigService.findById(strategyId);

        return strategyConfig.getId() != null
                ? new ResponseEntity<>(strategyConfig, null, HttpStatus.OK)
                : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/config/strategy/{strategyId}", method = RequestMethod.PUT)
    ResponseEntity<?> updateStrategy(@PathVariable String strategyId, @RequestBody StrategyConfig config) {

        final StrategyConfig updatedConfig = strategyConfigService.updateStrategy(config);
        final HttpHeaders httpHeaders = new HttpHeaders();
        return new ResponseEntity<>(updatedConfig, httpHeaders, HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/config/strategy/{strategyId}", method = RequestMethod.POST)
    ResponseEntity<?> createStrategy(@PathVariable String strategyId, @RequestBody StrategyConfig config) {

        final StrategyConfig updatedConfig = strategyConfigService.saveStrategy(config);
        final HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setLocation(ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{strategyId}")
                .buildAndExpand(updatedConfig.getId()).toUri());
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/config/strategy/{strategyId}", method = RequestMethod.DELETE)
    public StrategyConfig deleteStrategy(@PathVariable String strategyId) {
        return strategyConfigService.deleteStrategyById(strategyId);
    }
}

