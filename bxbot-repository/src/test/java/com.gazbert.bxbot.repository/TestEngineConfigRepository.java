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

package com.gazbert.bxbot.repository;

import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.engine.EngineConfig;
import com.gazbert.bxbot.core.config.engine.generated.EngineType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;

import static com.gazbert.bxbot.core.config.engine.EngineConfig.ENGINE_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.core.config.engine.EngineConfig.ENGINE_CONFIG_XSD_FILENAME;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.easymock.EasyMock.*;

/**
 * Tests Engine configuration repository behaves as expected.
 *
 * @author gazbert
 * @since 14/08/2016
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationManager.class})
public class TestEngineConfigRepository {

    private static final String ENGINE_EMERGENCY_STOP_CURRENCY = "BTC";
    private static final BigDecimal ENGINE_EMERGENCY_STOP_BALANCE = new BigDecimal("0.5");
    private static final int ENGINE_TRADE_CYCLE_INTERVAL = 60;


    @Before
    public void setup() throws Exception {
        PowerMock.mockStatic(ConfigurationManager.class);
    }

    @Test
    public void whenGetConfigCalledThenExpectRepositoryToLoadIt() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(EngineType.class),
                eq(ENGINE_CONFIG_XML_FILENAME),
                eq(ENGINE_CONFIG_XSD_FILENAME))).
                andReturn(someInternalEngineConfig());

        PowerMock.replayAll();

        final EngineConfigRepository engineConfigRepository = new EngineConfigRepositoryImpl();
        final EngineConfig engineConfig = engineConfigRepository.getConfig();
        assertThat(engineConfig.getEmergencyStopCurrency()).isEqualTo(ENGINE_EMERGENCY_STOP_CURRENCY);
        assertThat(engineConfig.getEmergencyStopBalance()).isEqualTo(ENGINE_EMERGENCY_STOP_BALANCE);
        assertThat(engineConfig.getTradeCycleInterval()).isEqualTo(ENGINE_TRADE_CYCLE_INTERVAL);

        PowerMock.verifyAll();
    }

    @Test
    public void whenUpdateConfigCalledThenExpectRepositoryToSaveIt() throws Exception {

        ConfigurationManager.saveConfig(eq(EngineType.class), anyObject(EngineType.class), eq(ENGINE_CONFIG_XML_FILENAME));
        PowerMock.replayAll();

        final EngineConfigRepository engineConfigRepository = new EngineConfigRepositoryImpl();
        engineConfigRepository.updateConfig(withSomeExternalEngineConfig());

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static EngineType someInternalEngineConfig() {
        final EngineType internalConfig = new EngineType();
        internalConfig.setEmergencyStopBalance(ENGINE_EMERGENCY_STOP_BALANCE);
        internalConfig.setEmergencyStopCurrency(ENGINE_EMERGENCY_STOP_CURRENCY);
        internalConfig.setTradeCycleInterval(ENGINE_TRADE_CYCLE_INTERVAL);
        return internalConfig;
    }

    private static EngineConfig withSomeExternalEngineConfig() {
        final EngineConfig externalConfig = new EngineConfig();
        externalConfig.setEmergencyStopBalance(ENGINE_EMERGENCY_STOP_BALANCE);
        externalConfig.setEmergencyStopCurrency(ENGINE_EMERGENCY_STOP_CURRENCY);
        externalConfig.setTradeCycleInterval(ENGINE_TRADE_CYCLE_INTERVAL);
        return externalConfig;
    }
}
