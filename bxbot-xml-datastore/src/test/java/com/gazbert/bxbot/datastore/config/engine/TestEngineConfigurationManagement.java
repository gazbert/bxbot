/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

package com.gazbert.bxbot.datastore.config.engine;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.engine.generated.EngineType;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the Trading Engine configuration is loaded as expected.
 *
 * @author gazbert
 */
public class TestEngineConfigurationManagement {

    /* Production XSD */
    private static final String XML_SCHEMA_FILENAME = "com/gazbert/bxbot/datastore/config/engine.xsd";

    /* Test XML config */
    private static final String VALID_XML_CONFIG_FILENAME = "src/test/config/engine/valid-engine.xml";
    private static final String INVALID_XML_CONFIG_FILENAME = "src/test/config/engine/invalid-engine.xml";
    private static final String MISSING_XML_CONFIG_FILENAME = "src/test/config/engine/missing-engine.xml";
    private static final String XML_CONFIG_TO_SAVE_FILENAME = "src/test/config/engine/saved-engine.xml";

    private static final String BOT_ID = "avro-707_1";
    private static final String BOT_NAME = "Avro 707";
    private static final String EMERGENCY_STOP_CURRENCY = "BTC";
    private static final BigDecimal EMERGENCY_STOP_BALANCE = new BigDecimal("0.5");
    private static final int TRADE_CYCLE_INTERVAL = 60;


    @Test
    public void testLoadingValidXmlConfigFileIsSuccessful() {

        final EngineType engine = ConfigurationManager.loadConfig(EngineType.class,
                VALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);

        assertEquals(BOT_ID, engine.getBotId());
        assertEquals(BOT_NAME, engine.getBotName());
        assertEquals(EMERGENCY_STOP_CURRENCY, engine.getEmergencyStopCurrency());
        assertTrue(EMERGENCY_STOP_BALANCE.compareTo(engine.getEmergencyStopBalance()) == 0);
        assertTrue(TRADE_CYCLE_INTERVAL == engine.getTradeCycleInterval());
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadingMissingXmlConfigThrowsException() {
        ConfigurationManager.loadConfig(EngineType.class, MISSING_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadingInvalidXmlConfigThrowsException() {
        ConfigurationManager.loadConfig(EngineType.class, INVALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }

    @Test
    public void testSavingConfigToXmlIsSuccessful() throws Exception {

        final EngineType engineConfig = new EngineType();
        engineConfig.setBotId(BOT_ID);
        engineConfig.setBotName(BOT_NAME);
        engineConfig.setEmergencyStopCurrency(EMERGENCY_STOP_CURRENCY);
        engineConfig.setEmergencyStopBalance(EMERGENCY_STOP_BALANCE);
        engineConfig.setTradeCycleInterval(TRADE_CYCLE_INTERVAL);

        ConfigurationManager.saveConfig(EngineType.class, engineConfig, XML_CONFIG_TO_SAVE_FILENAME);

        // Read it back in
        final EngineType engineReloaded = ConfigurationManager.loadConfig(EngineType.class,
                XML_CONFIG_TO_SAVE_FILENAME, XML_SCHEMA_FILENAME);

        assertEquals(BOT_ID, engineReloaded.getBotId());
        assertEquals(BOT_NAME, engineReloaded.getBotName());
        assertEquals(EMERGENCY_STOP_CURRENCY, engineReloaded.getEmergencyStopCurrency());
        assertTrue(EMERGENCY_STOP_BALANCE.compareTo(engineReloaded.getEmergencyStopBalance()) == 0);
        assertTrue(TRADE_CYCLE_INTERVAL == engineReloaded.getTradeCycleInterval());

        // cleanup
        Files.delete(FileSystems.getDefault().getPath(XML_CONFIG_TO_SAVE_FILENAME));
    }
}
