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

package com.gazbert.bxbot.datastore.yaml.strategy;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests the Strategy configuration is loaded as expected.
 * * 测试策略配置是否按预期加载。
 *
 * @author gazbert
 */
class TestStrategyConfigurationManagement {

  private static final String VALID_YAML_CONFIG_FILENAME =
      "src/test/config/strategies/valid-strategies.yaml";
  private static final String INVALID_YAML_CONFIG_FILENAME =
      "src/test/config/strategies/invalid-strategies.yaml";
  private static final String MISSING_YAML_CONFIG_FILENAME =
      "src/test/config/strategies/missing-strategies.yaml";
  private static final String YAML_CONFIG_TO_SAVE_FILENAME =
      "src/test/config/strategies/saved-strategies.yaml";
  private static final String INVALID_YAML_CONFIG_TO_SAVE_FILENAME =
      "src/test/config/not-there/saved-strategies.yaml";

  private static final String STRAT_ID_1 = "macd-long-position";
  private static final String STRAT_NAME_1 = "MACD Long Position Algo";
  private static final String STRAT_DESCRIPTION_1 =
      "Uses MACD as indicator and takes long position in base currency.";
  private static final String STRAT_CLASSNAME_1 = "com.gazbert.nova.algos.MacdLongBase";

  private static final String STRAT_ID_2 = "long-scalper";
  private static final String STRAT_NAME_2 = "Long Position Scalper Algo";
  private static final String STRAT_DESCRIPTION_2 = "Scalps and goes long...";
  private static final String STRAT_BEAN_NAME_2 = "myMacdStratBean";

  private static final String BUY_PRICE_CONFIG_ITEM_KEY = "buy-price";
  private static final String BUY_PRICE_CONFIG_ITEM_VALUE = "671.15";
  private static final String AMOUNT_TO_BUY_CONFIG_ITEM_KEY = "buy-amount";
  private static final String AMOUNT_TO_BUY_CONFIG_ITEM_VALUE = "0.5";

  @Test
  void testLoadingValidYamlConfigFileIsSuccessful() {
    final StrategiesType strategyConfig =
        ConfigurationManager.loadConfig(StrategiesType.class, VALID_YAML_CONFIG_FILENAME);

    assertEquals(3, strategyConfig.getStrategies().size());

    /*
     * Strat 1
     */
    assertEquals("Basic Scalping Strat", strategyConfig.getStrategies().get(0).getName());
    assertEquals(
        strategyConfig.getStrategies().get(0).getDescription().trim(),
        "A simple trend following scalper"
            + " that buys at the current BID price, holds until current market "
            + "price has reached a configurable minimum percentage gain, and then sells at current"
            + " ASK price, thereby taking profit from the spread. Don't forget to factor in the "
            + "exchange fees!" +
                "一个简单的趋势跟随黄牛，以当前 BID 价格买入，一直保持到当前市场价格达到可配置的最小百分比收益，" +
                "然后以当前 ASK 价格卖出，从而从价差中获利。不要忘记考虑兑换费用！");
    assertEquals(
        "com.gazbert.bxbot.strategies.ExampleScalpingStrategy",
        strategyConfig.getStrategies().get(0).getClassName());
    assertNull(strategyConfig.getStrategies().get(0).getBeanName());

    assertEquals(2, strategyConfig.getStrategies().get(0).getConfigItems().size());
    assertEquals(
        "20",
        strategyConfig
            .getStrategies()
            .get(0)
            .getConfigItems()
            .get("counter-currency-buy-order-amount"));
    assertEquals(
        "1", strategyConfig.getStrategies().get(0).getConfigItems().get("minimum-percentage-gain"));

    /**
     * Strat 2
     * * 战略 2
     */
    assertEquals("ema-shorting-strategy", strategyConfig.getStrategies().get(1).getId());
    assertEquals("EMA Based Shorting Strat", strategyConfig.getStrategies().get(1).getName());
    assertNull(strategyConfig.getStrategies().get(1).getDescription()); // optional element check
    assertEquals(
        "com.gazbert.bxbot.strategies.YourEmaShortingStrategy",
        strategyConfig.getStrategies().get(1).getClassName());
    assertNull(strategyConfig.getStrategies().get(0).getBeanName());

    assertEquals(4, strategyConfig.getStrategies().get(1).getConfigItems().size());
    assertEquals(
        "0.5", strategyConfig.getStrategies().get(1).getConfigItems().get("btc-sell-order-amount"));
    assertEquals(
        "5", strategyConfig.getStrategies().get(1).getConfigItems().get("shortEmaInterval"));
    assertEquals(
        "10", strategyConfig.getStrategies().get(1).getConfigItems().get("mediumEmaInterval"));
    assertEquals(
        "20", strategyConfig.getStrategies().get(1).getConfigItems().get("longEmaInterval"));

    /**
     * Strat 3 * 战略 3
     */
    assertEquals("macd-strategy", strategyConfig.getStrategies().get(2).getId());
    assertEquals("MACD Based Strat", strategyConfig.getStrategies().get(2).getName());
    assertEquals(
        "Strat uses MACD data to take long position in USD.",
        strategyConfig.getStrategies().get(2).getDescription().trim());
    assertEquals("myMacdStratBean", strategyConfig.getStrategies().get(2).getBeanName());
    assertNull(strategyConfig.getStrategies().get(2).getClassName());
    assertEquals(
        new HashMap(),
        strategyConfig.getStrategies().get(2).getConfigItems()); // optional element check
  }

  @Test
  void testLoadingMissingYamlConfigFileThrowsException() {
    assertThrows(
        IllegalStateException.class,
        () -> ConfigurationManager.loadConfig(StrategiesType.class, MISSING_YAML_CONFIG_FILENAME));
  }

  @Test
  void testLoadingInvalidYamlConfigFileThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ConfigurationManager.loadConfig(StrategiesType.class, INVALID_YAML_CONFIG_FILENAME));
  }

  @Test
  void testSavingConfigToYamlIsSuccessful() throws Exception {
    // Strat 1 第 1 层
    final StrategyConfig strategy1 = new StrategyConfig();
    strategy1.setId(STRAT_ID_1);
    strategy1.setName(STRAT_NAME_1);
    strategy1.setDescription(STRAT_DESCRIPTION_1);
    strategy1.setClassName(STRAT_CLASSNAME_1);

    final Map<String, String> strat1ConfigItems = new HashMap<>();
    strat1ConfigItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    strat1ConfigItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
    strategy1.setConfigItems(strat1ConfigItems);

    // Strat 2 第 2
    final StrategyConfig strategy2 = new StrategyConfig();
    strategy2.setId(STRAT_ID_2);
    strategy2.setName(STRAT_NAME_2);
    strategy2.setDescription(STRAT_DESCRIPTION_2);
    strategy2.setBeanName(STRAT_BEAN_NAME_2);

    final Map<String, String> strat2ConfigItems = new HashMap<>();
    strat2ConfigItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    strat2ConfigItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
    strategy2.setConfigItems(strat2ConfigItems);

    final StrategiesType strategiesConfig = new StrategiesType();
    strategiesConfig.getStrategies().add(strategy1);
    strategiesConfig.getStrategies().add(strategy2);

    ConfigurationManager.saveConfig(
        StrategiesType.class, strategiesConfig, YAML_CONFIG_TO_SAVE_FILENAME);

    // Read it back in 读回来
    final StrategiesType strategiesReloaded =
        ConfigurationManager.loadConfig(StrategiesType.class, YAML_CONFIG_TO_SAVE_FILENAME);

    // Strat 1 // 层 1
    assertThat(strategiesReloaded.getStrategies().get(0).getId()).isEqualTo(STRAT_ID_1);
    assertThat(strategiesReloaded.getStrategies().get(0).getName()).isEqualTo(STRAT_NAME_1);
    assertThat(strategiesReloaded.getStrategies().get(0).getDescription())
        .isEqualTo(STRAT_DESCRIPTION_1);
    assertThat(strategiesReloaded.getStrategies().get(0).getClassName())
        .isEqualTo(STRAT_CLASSNAME_1);
    assertThat(strategiesReloaded.getStrategies().get(0).getBeanName()).isNull();

    assertThat(
            strategiesReloaded
                .getStrategies()
                .get(0)
                .getConfigItems()
                .get(BUY_PRICE_CONFIG_ITEM_KEY))
        .isEqualTo(BUY_PRICE_CONFIG_ITEM_VALUE);
    assertThat(
            strategiesReloaded
                .getStrategies()
                .get(0)
                .getConfigItems()
                .get(AMOUNT_TO_BUY_CONFIG_ITEM_KEY))
        .isEqualTo(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

    // Strat 2 // 策略 2
    assertThat(strategiesReloaded.getStrategies().get(1).getId()).isEqualTo(STRAT_ID_2);
    assertThat(strategiesReloaded.getStrategies().get(1).getName()).isEqualTo(STRAT_NAME_2);
    assertThat(strategiesReloaded.getStrategies().get(1).getDescription())
        .isEqualTo(STRAT_DESCRIPTION_2);
    assertThat(strategiesReloaded.getStrategies().get(1).getClassName()).isNull();
    assertThat(strategiesReloaded.getStrategies().get(1).getBeanName())
        .isEqualTo(STRAT_BEAN_NAME_2);

    assertThat(
            strategiesReloaded
                .getStrategies()
                .get(1)
                .getConfigItems()
                .get(BUY_PRICE_CONFIG_ITEM_KEY))
        .isEqualTo(BUY_PRICE_CONFIG_ITEM_VALUE);
    assertThat(
            strategiesReloaded
                .getStrategies()
                .get(1)
                .getConfigItems()
                .get(AMOUNT_TO_BUY_CONFIG_ITEM_KEY))
        .isEqualTo(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

    // cleanup
    Files.delete(FileSystems.getDefault().getPath(YAML_CONFIG_TO_SAVE_FILENAME));
  }

  @Test
  void testSavingConfigToInvalidYamlFileIsHandled() {
    // Strat 1
    final StrategyConfig strategy1 = new StrategyConfig();
    strategy1.setId(STRAT_ID_1);
    strategy1.setName(STRAT_NAME_1);
    strategy1.setDescription(STRAT_DESCRIPTION_1);
    strategy1.setClassName(STRAT_CLASSNAME_1);

    final Map<String, String> strat1ConfigItems = new HashMap<>();
    strat1ConfigItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    strat1ConfigItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
    strategy1.setConfigItems(strat1ConfigItems);

    // Strat 2
    final StrategyConfig strategy2 = new StrategyConfig();
    strategy2.setId(STRAT_ID_2);
    strategy2.setName(STRAT_NAME_2);
    strategy2.setDescription(STRAT_DESCRIPTION_2);
    strategy2.setBeanName(STRAT_BEAN_NAME_2);

    final Map<String, String> strat2ConfigItems = new HashMap<>();
    strat2ConfigItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    strat2ConfigItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
    strategy2.setConfigItems(strat2ConfigItems);

    final StrategiesType strategiesConfig = new StrategiesType();
    strategiesConfig.getStrategies().add(strategy1);
    strategiesConfig.getStrategies().add(strategy2);

    assertThrows(
        IllegalStateException.class,
        () ->
            ConfigurationManager.saveConfig(
                StrategiesType.class, strategiesConfig, INVALID_YAML_CONFIG_TO_SAVE_FILENAME));
  }
}
