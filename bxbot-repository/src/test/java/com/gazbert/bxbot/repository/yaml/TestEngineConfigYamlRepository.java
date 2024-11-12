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

package com.gazbert.bxbot.repository.yaml;

import static com.gazbert.bxbot.datastore.yaml.FileLocations.ENGINE_CONFIG_YAML_FILENAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.datastore.yaml.engine.EngineType;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.repository.EngineConfigRepository;
import java.math.BigDecimal;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests YAML backed Engine configuration repository behaves as expected.
 *
 * @author gazbert
 */
class TestEngineConfigYamlRepository {

  private static final String BOT_ID = "avro-707_1";
  private static final String BOT_NAME = "Avro 707";
  private static final String ENGINE_EMERGENCY_STOP_CURRENCY = "BTC";
  private static final BigDecimal ENGINE_EMERGENCY_STOP_BALANCE = new BigDecimal("0.5");
  private static final int ENGINE_TRADE_CYCLE_INTERVAL = 60;

  private ConfigurationManager configurationManager;

  @BeforeEach
  void setup() {
    configurationManager = EasyMock.createMock(ConfigurationManager.class);
  }

  @Test
  void whenGetCalledThenExpectEngineConfigToBeReturned() {
    expect(configurationManager.loadConfig(eq(EngineType.class), eq(ENGINE_CONFIG_YAML_FILENAME)))
        .andReturn(someInternalEngineConfig());

    EasyMock.replay(configurationManager);

    final EngineConfigRepository engineConfigRepository =
        new EngineConfigYamlRepository(configurationManager);
    final EngineConfig engineConfig = engineConfigRepository.get();
    assertThat(engineConfig.getBotId()).isEqualTo(BOT_ID);
    assertThat(engineConfig.getBotName()).isEqualTo(BOT_NAME);
    assertThat(engineConfig.getEmergencyStopCurrency()).isEqualTo(ENGINE_EMERGENCY_STOP_CURRENCY);
    assertThat(engineConfig.getEmergencyStopBalance()).isEqualTo(ENGINE_EMERGENCY_STOP_BALANCE);
    assertThat(engineConfig.getTradeCycleInterval()).isEqualTo(ENGINE_TRADE_CYCLE_INTERVAL);

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenSaveCalledThenExpectRepositoryToSaveItAndReturnSavedEngineConfig() {
    configurationManager.saveConfig(
        eq(EngineType.class), anyObject(EngineType.class), eq(ENGINE_CONFIG_YAML_FILENAME));

    expect(configurationManager.loadConfig(eq(EngineType.class), eq(ENGINE_CONFIG_YAML_FILENAME)))
        .andReturn(someInternalEngineConfig());

    EasyMock.replay(configurationManager);

    final EngineConfigRepository engineConfigRepository =
        new EngineConfigYamlRepository(configurationManager);
    final EngineConfig savedConfig = engineConfigRepository.save(someExternalEngineConfig());

    assertThat(savedConfig.getBotId()).isEqualTo(BOT_ID);
    assertThat(savedConfig.getBotName()).isEqualTo(BOT_NAME);
    assertThat(savedConfig.getEmergencyStopCurrency()).isEqualTo(ENGINE_EMERGENCY_STOP_CURRENCY);
    assertThat(savedConfig.getEmergencyStopBalance()).isEqualTo(ENGINE_EMERGENCY_STOP_BALANCE);
    assertThat(savedConfig.getTradeCycleInterval()).isEqualTo(ENGINE_TRADE_CYCLE_INTERVAL);

    EasyMock.verify(configurationManager);
  }

  // --------------------------------------------------------------------------
  // Private utils
  // --------------------------------------------------------------------------

  private static EngineType someInternalEngineConfig() {
    final EngineConfig engineConfig = new EngineConfig();
    engineConfig.setBotId(BOT_ID);
    engineConfig.setBotName(BOT_NAME);
    engineConfig.setEmergencyStopBalance(ENGINE_EMERGENCY_STOP_BALANCE);
    engineConfig.setEmergencyStopCurrency(ENGINE_EMERGENCY_STOP_CURRENCY);
    engineConfig.setTradeCycleInterval(ENGINE_TRADE_CYCLE_INTERVAL);

    final EngineType internalConfig = new EngineType();
    internalConfig.setEngine(engineConfig);
    return internalConfig;
  }

  private static EngineConfig someExternalEngineConfig() {
    final EngineConfig externalConfig = new EngineConfig();
    externalConfig.setBotId(BOT_ID);
    externalConfig.setBotName(BOT_NAME);
    externalConfig.setEmergencyStopBalance(ENGINE_EMERGENCY_STOP_BALANCE);
    externalConfig.setEmergencyStopCurrency(ENGINE_EMERGENCY_STOP_CURRENCY);
    externalConfig.setTradeCycleInterval(ENGINE_TRADE_CYCLE_INTERVAL);
    return externalConfig;
  }
}
