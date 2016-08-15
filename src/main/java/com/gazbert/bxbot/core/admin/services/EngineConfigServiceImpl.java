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

package com.gazbert.bxbot.core.admin.services;

import com.gazbert.bxbot.core.config.ConfigurationException;
import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.engine.EngineConfig;
import com.gazbert.bxbot.core.config.engine.generated.EngineType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * TODO Work in progress...
 *
 * @author gazbert
 * @since 11/08/2016
 */
@Service("engineConfigService")
@Transactional
public class EngineConfigServiceImpl implements EngineConfigService {

    private static final Logger LOG = LogManager.getLogger();

    @Override
    public EngineConfig getConfig() {

        // TODO tweak stuff to use repository etc...
        final EngineType internalEngineConfig = ConfigurationManager.loadConfig(EngineType.class,
                EngineConfig.ENGINE_CONFIG_XML_FILENAME, EngineConfig.ENGINE_CONFIG_XSD_FILENAME);

        return adaptInternalToExternalConfig(internalEngineConfig);
        //return getCannedEngineConfig();
    }

    @Override
    public void updateConfig(EngineConfig config) {

        LOG.info( () -> "About to update: " + config);

        final EngineType internalEngineConfig = adaptExternalToInternalConfig(config);

        try {
            ConfigurationManager.saveConfig(EngineType.class, internalEngineConfig, EngineConfig.ENGINE_CONFIG_XML_FILENAME);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

    }

    /*
     * TODO Hard code these for now - will come from Repository later...
     */
    private static EngineConfig getCannedEngineConfig() {

        final EngineConfig engineConfig = new EngineConfig();
        engineConfig.setEmergencyStopCurrency("BTC");
        engineConfig.setEmergencyStopBalance(new BigDecimal("0.923232"));
        engineConfig.setTradeCycleInterval(60);
        return engineConfig;
    }

    /*
     * TODO Mock this out for unit testing
     */
    private EngineConfig adaptInternalToExternalConfig(EngineType internalEngineConfig) {

        final EngineConfig externalEngineConfig = new EngineConfig();
        externalEngineConfig.setEmergencyStopCurrency(internalEngineConfig.getEmergencyStopCurrency());
        externalEngineConfig.setEmergencyStopBalance(internalEngineConfig.getEmergencyStopBalance());
        externalEngineConfig.setTradeCycleInterval(internalEngineConfig.getTradeCycleInterval());
        return externalEngineConfig;
    }

    /*
 * TODO Mock this out for unit testing
 */
    private EngineType adaptExternalToInternalConfig(EngineConfig externalEngineConfig) {

        final EngineType internalEngineConfig = new EngineType();
        internalEngineConfig.setEmergencyStopCurrency(externalEngineConfig.getEmergencyStopCurrency());
        internalEngineConfig.setEmergencyStopBalance(externalEngineConfig.getEmergencyStopBalance());
        internalEngineConfig.setTradeCycleInterval(externalEngineConfig.getTradeCycleInterval());
        return internalEngineConfig;
    }
}
