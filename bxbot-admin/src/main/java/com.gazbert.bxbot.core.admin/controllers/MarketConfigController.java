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

import com.gazbert.bxbot.core.config.market.MarketConfig;
import com.gazbert.bxbot.core.admin.services.MarketConfigService;
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
 *
 * Controller for directing Market config requests.
 *
 * @author gazbert
 * @since 20/07/2016
 */
@RestController
@RequestMapping("/api")
public class MarketConfigController {

    private final MarketConfigService marketConfigService;

    @Autowired
    public MarketConfigController(MarketConfigService marketConfigService) {
        Assert.notNull(marketConfigService, "marketConfigService dependency cannot be null!");
        this.marketConfigService = marketConfigService;
    }

    @RequestMapping(value = "/config/market/{marketId}", method = RequestMethod.GET)
    public MarketConfig getMarket(@PathVariable String marketId) {
        return marketConfigService.findById(marketId);
    }

    @RequestMapping(value = "/config/market/{marketId}", method = RequestMethod.PUT)
    ResponseEntity<?> updateMarket(@PathVariable String marketId, @RequestBody MarketConfig config) {

        final MarketConfig updatedMarketConfig = marketConfigService.updateMarket(config);
        final HttpHeaders httpHeaders = new HttpHeaders();
        return new ResponseEntity<>(updatedMarketConfig, httpHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/config/market/{marketId}", method = RequestMethod.POST)
    ResponseEntity<?> createMarket(@PathVariable String marketId, @RequestBody MarketConfig config) {

        final MarketConfig updatedMarketConfig = marketConfigService.saveMarket(config);
        final HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setLocation(ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{marketId}")
                .buildAndExpand(updatedMarketConfig.getId()).toUri());
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/config/market/{marketId}", method = RequestMethod.DELETE)
    public MarketConfig deleteMarket(@PathVariable String marketId) {
        return marketConfigService.deleteMarketById(marketId);
    }

    @RequestMapping(value = "/config/market", method = RequestMethod.GET)
    public List<MarketConfig> getAllMarkets() {
        return marketConfigService.findAllMarkets();
    }
}

