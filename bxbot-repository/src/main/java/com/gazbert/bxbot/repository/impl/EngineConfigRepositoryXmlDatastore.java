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

package com.gazbert.bxbot.repository.impl;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.engine.generated.EngineType;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.repository.EngineConfigRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.gazbert.bxbot.datastore.FileLocations.ENGINE_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.datastore.FileLocations.ENGINE_CONFIG_XSD_FILENAME;

/**
 * An XML datastore implementation of the Engine config repository.
 *
 * @author gazbert
 */
@Repository("engineConfigRepository")
@Transactional
public class EngineConfigRepositoryXmlDatastore implements EngineConfigRepository {

    private static final Logger LOG = LogManager.getLogger();

    @Override
    public EngineConfig get() {

        LOG.info(() -> "Fetching EngineConfig...");

        final EngineType internalEngineConfig = ConfigurationManager.loadConfig(EngineType.class,
                ENGINE_CONFIG_XML_FILENAME, ENGINE_CONFIG_XSD_FILENAME);
        return adaptInternalToExternalConfig(internalEngineConfig);
    }

    @Override
    public EngineConfig save(EngineConfig config) {

        LOG.info(() -> "About to save EngineConfig: " + config);

        final EngineType internalEngineConfig = adaptExternalToInternalConfig(config);
        ConfigurationManager.saveConfig(EngineType.class, internalEngineConfig, ENGINE_CONFIG_XML_FILENAME);

        final EngineType savedEngineConfig = ConfigurationManager.loadConfig(EngineType.class,
                ENGINE_CONFIG_XML_FILENAME, ENGINE_CONFIG_XSD_FILENAME);
        return adaptInternalToExternalConfig(savedEngineConfig);
    }

    // ------------------------------------------------------------------------------------------------
    // Adapter methods
    // ------------------------------------------------------------------------------------------------

    private static EngineConfig adaptInternalToExternalConfig(EngineType internalEngineConfig) {

        final EngineConfig externalEngineConfig = new EngineConfig();
        externalEngineConfig.setBotId(internalEngineConfig.getBotId());
        externalEngineConfig.setBotName(internalEngineConfig.getBotName());
        externalEngineConfig.setEmergencyStopCurrency(internalEngineConfig.getEmergencyStopCurrency());
        externalEngineConfig.setEmergencyStopBalance(internalEngineConfig.getEmergencyStopBalance());
        externalEngineConfig.setTradeCycleInterval(internalEngineConfig.getTradeCycleInterval());
        return externalEngineConfig;
    }

    private static EngineType adaptExternalToInternalConfig(EngineConfig externalEngineConfig) {

        final EngineType internalEngineConfig = new EngineType();
        internalEngineConfig.setBotId(externalEngineConfig.getBotId());
        internalEngineConfig.setBotName(externalEngineConfig.getBotName());
        internalEngineConfig.setEmergencyStopCurrency(externalEngineConfig.getEmergencyStopCurrency());
        internalEngineConfig.setEmergencyStopBalance(externalEngineConfig.getEmergencyStopBalance());
        internalEngineConfig.setTradeCycleInterval(externalEngineConfig.getTradeCycleInterval());
        return internalEngineConfig;
    }
}
