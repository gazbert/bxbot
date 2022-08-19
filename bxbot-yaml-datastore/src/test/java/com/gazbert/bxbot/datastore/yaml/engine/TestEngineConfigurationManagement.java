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

package com.gazbert.bxbot.datastore.yaml.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/**
 * Tests the Trading Engine configuration is loaded as expected.
 * * 测试交易引擎配置是否按预期加载。
 *
 * @author gazbert
 */
class TestEngineConfigurationManagement {

  private static final String VALID_YAML_CONFIG_FILENAME =
      "src/test/config/engine/valid-engine.yaml";
  private static final String INVALID_YAML_CONFIG_FILENAME =
      "src/test/config/engine/invalid-engine.yaml";
  private static final String MISSING_YAML_CONFIG_FILENAME =
      "src/test/config/engine/missing-engine.yaml";
  private static final String YAML_CONFIG_TO_SAVE_FILENAME =
      "src/test/config/engine/saved-engine.yaml";
  private static final String INVALID_YAML_CONFIG_TO_SAVE_FILENAME =
      "src/test/config/not-here/saved-engine.yaml";

  private static final String BOT_ID = "avro-707_1";
  private static final String BOT_NAME = "Avro 707";
  private static final String EMERGENCY_STOP_CURRENCY = "BTC";
  private static final BigDecimal EMERGENCY_STOP_BALANCE = new BigDecimal("0.5");
  private static final int TRADE_CYCLE_INTERVAL = 60;

  @Test
  void testLoadingValidYamlConfigFileIsSuccessful() {
    final EngineType engineType =
        ConfigurationManager.loadConfig(EngineType.class, VALID_YAML_CONFIG_FILENAME);
    assertEquals(BOT_ID, engineType.getEngine().getBotId());
    assertEquals(BOT_NAME, engineType.getEngine().getBotName());
    assertEquals(EMERGENCY_STOP_CURRENCY, engineType.getEngine().getEmergencyStopCurrency());
    assertEquals(
        0, EMERGENCY_STOP_BALANCE.compareTo(engineType.getEngine().getEmergencyStopBalance()));
    assertEquals(TRADE_CYCLE_INTERVAL, engineType.getEngine().getTradeCycleInterval());
  }

  @Test
  void testLoadingMissingYamlConfigThrowsException() {
    assertThrows(
        IllegalStateException.class,
        () -> ConfigurationManager.loadConfig(EngineType.class, MISSING_YAML_CONFIG_FILENAME));
  }

  @Test
  void testLoadingInvalidYamlConfigThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ConfigurationManager.loadConfig(EngineType.class, INVALID_YAML_CONFIG_FILENAME));
  }

  @Test
  void testSavingConfigToYamlIsSuccessful() throws Exception {
    final EngineConfig engineConfig = new EngineConfig();
    engineConfig.setBotId(BOT_ID);
    engineConfig.setBotName(BOT_NAME);
    engineConfig.setEmergencyStopCurrency(EMERGENCY_STOP_CURRENCY);
    engineConfig.setEmergencyStopBalance(EMERGENCY_STOP_BALANCE);
    engineConfig.setTradeCycleInterval(TRADE_CYCLE_INTERVAL);

    final EngineType engineType = new EngineType();
    engineType.setEngine(engineConfig);

    ConfigurationManager.saveConfig(EngineType.class, engineType, YAML_CONFIG_TO_SAVE_FILENAME);

    // Read it back in
    // 读回
    final EngineType engineTypeReloaded =
        ConfigurationManager.loadConfig(EngineType.class, YAML_CONFIG_TO_SAVE_FILENAME);

    assertEquals(BOT_ID, engineTypeReloaded.getEngine().getBotId());
    assertEquals(BOT_NAME, engineTypeReloaded.getEngine().getBotName());
    assertEquals(
        EMERGENCY_STOP_CURRENCY, engineTypeReloaded.getEngine().getEmergencyStopCurrency());
    assertEquals(
        0,
        EMERGENCY_STOP_BALANCE.compareTo(engineTypeReloaded.getEngine().getEmergencyStopBalance()));
    assertEquals(TRADE_CYCLE_INTERVAL, engineTypeReloaded.getEngine().getTradeCycleInterval());

    // cleanup
    // 清理
    Files.delete(FileSystems.getDefault().getPath(YAML_CONFIG_TO_SAVE_FILENAME));
  }

  @Test
  void testSavingConfigToInvalidYamlFileIsHandled() {
    final EngineConfig engineConfig = new EngineConfig();
    engineConfig.setBotId(BOT_ID);
    engineConfig.setBotName(BOT_NAME);
    engineConfig.setEmergencyStopCurrency(EMERGENCY_STOP_CURRENCY);
    engineConfig.setEmergencyStopBalance(EMERGENCY_STOP_BALANCE);
    engineConfig.setTradeCycleInterval(TRADE_CYCLE_INTERVAL);

    final EngineType engineType = new EngineType();
    engineType.setEngine(engineConfig);

    assertThrows(
        IllegalStateException.class,
        () ->
            ConfigurationManager.saveConfig(
                EngineType.class, engineType, INVALID_YAML_CONFIG_TO_SAVE_FILENAME));
  }
}
