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

package com.gazbert.bxbot.rest.api.config;

import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.services.StrategyConfigService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for directing Strategy config requests.
 *
 * @author gazbert
 * @since 1.0
 */
@RestController
@RequestMapping("/api/config")
public class StrategyConfigController {

    private static final Logger LOG = LogManager.getLogger();
    private final StrategyConfigService strategyConfigService;

    @Autowired
    public StrategyConfigController(StrategyConfigService strategyConfigService) {
        this.strategyConfigService = strategyConfigService;
    }

    /**
     * Returns all of the Strategy configuration for the bot.
     *
     * @param user the authenticated user.
     * @return all the Strategy configurations.
     */
    @RequestMapping(value = "/strategies", method = RequestMethod.GET)
    public List<StrategyConfig> getAllStrategies(@AuthenticationPrincipal User user) {

        LOG.info("GET /strategies - getAllStrategies() - caller: " + user.getUsername());

        final List<StrategyConfig> strategyConfigs = strategyConfigService.getAllStrategyConfig();

        LOG.info("Response: " + strategyConfigs);
        return strategyConfigs;
    }

    /**
     * Returns the Strategy configuration for a given id.
     *
     * @param user       the authenticated user.
     * @param strategyId the id of the Strategy to fetch.
     * @return the Strategy configuration.
     */
    @RequestMapping(value = "/strategies/{strategyId}", method = RequestMethod.GET)
    public ResponseEntity<?> getStrategy(@AuthenticationPrincipal User user, @PathVariable String strategyId) {

        LOG.info("GET /strategies/" + strategyId + " - getStrategy() - caller: " + user.getUsername());

        final StrategyConfig strategyConfig = strategyConfigService.getStrategyConfig(strategyId);
        LOG.info("Response: " + strategyConfig);

        return strategyConfig.getId() != null
                ? new ResponseEntity<>(strategyConfig, null, HttpStatus.OK)
                : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Updates a given Strategy configuration.
     *
     * @param user       the authenticated user.
     * @param strategyId id of the Strategy config to update.
     * @param config     the updated Strategy config.
     * @return 200 'OK' HTTP status code and updated Strategy config in the body if update successful,
     *         404 'Not Found' HTTP status code if Strategy config not found.
     */
    @RequestMapping(value = "/strategies/{strategyId}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateStrategy(@AuthenticationPrincipal User user, @PathVariable String strategyId,
                                     @RequestBody StrategyConfig config) {

        LOG.info("PUT /strategies/" + strategyId + " - updateStrategy() - caller: " + user.getUsername());
        LOG.info("Request: " + config);

        if (config == null || config.getId() == null || !strategyId.equals(config.getId())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        final StrategyConfig updatedConfig = strategyConfigService.updateStrategyConfig(config);
        return updatedConfig.getId() != null
                ? new ResponseEntity<>(updatedConfig, HttpStatus.OK)
                : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Creates a new Strategy configuration.
     *
     * @param user       the authenticated user.
     * @param strategyId id of the Strategy config to create.
     * @param config     the new Strategy config.
     * @return 201 'Created' HTTP status code and created Strategy config in response body if create successful,
     *         409 'Conflict' HTTP status code if Strategy config already exists.
     */
    @RequestMapping(value = "/strategies/{strategyId}", method = RequestMethod.POST)
    public ResponseEntity<?> createStrategy(@AuthenticationPrincipal User user, @PathVariable String strategyId,
                                     @RequestBody StrategyConfig config) {

        LOG.info("POST /strategies/" + strategyId + " - createStrategy() - caller: " + user.getUsername());
        LOG.info("Request: " + config);

        if (config == null || config.getId() == null || !strategyId.equals(config.getId())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        final StrategyConfig createdConfig = strategyConfigService.createStrategyConfig(config);
        if (createdConfig.getId() != null) {
            return new ResponseEntity<>(createdConfig, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    /**
     * Deletes a Strategy configuration for a given id.
     *
     * @param user       the authenticated user.
     * @param strategyId the id of the Strategy configuration to delete.
     * @return 204 'No Content' HTTP status code if update successful, 404 'Not Found' HTTP status code if
     *         Strategy config not found.
     */
    @RequestMapping(value = "/strategies/{strategyId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteStrategy(@AuthenticationPrincipal User user, @PathVariable String strategyId) {

        LOG.info("DELETE /strategies/" + strategyId + " - deleteStrategy() - caller: " + user.getUsername());

        final StrategyConfig deletedConfig = strategyConfigService.deleteStrategyConfig(strategyId);
        return deletedConfig.getId() != null
                ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}

