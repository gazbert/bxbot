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

import static com.gazbert.bxbot.datastore.yaml.FileLocations.STRATEGIES_CONFIG_YAML_FILENAME;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.datastore.yaml.strategy.StrategiesType;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.repository.StrategyConfigRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests YAML backed Strategy configuration repository behaves as expected.
 ** 测试 YAML 支持的策略配置存储库的行为是否符合预期。
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationManager.class, StrategyConfigYamlRepository.class})
@PowerMockIgnore({
    "javax.crypto.*",
    "javax.management.*",
    "com.sun.org.apache.xerces.*",
    "javax.xml.parsers.*",
    "org.xml.sax.*",
    "org.w3c.dom.*"
})
public class TestStrategyConfigYamlRepository {

  private static final String MOCKED_GENERATE_UUID_METHOD = "generateUuid";

  private static final String UNKNOWN_STRAT_ID = "unknown-or-new-strat-id";
  private static final String GENERATED_STRAT_ID = "new-strat-id-123";

  private static final String STRAT_ID_1 = "macd-long-position";
  private static final String STRAT_NAME_1 = "MACD Long Position Algo";
  private static final String STRAT_DESCRIPTION_1 =
      "Uses MACD as indicator and takes long position in base currency.";
  private static final String STRAT_CLASSNAME_1 = "com.gazbert.nova.algos.MacdLongBase";
  private static final String STRAT_BEANAME_1 = "macdLongBase";

  private static final String STRAT_ID_2 = "long-scalper";
  private static final String STRAT_NAME_2 = "Long Position Scalper Algo";
  private static final String STRAT_DESCRIPTION_2 = "Scalps and goes long...";
  private static final String STRAT_CLASSNAME_2 = "com.gazbert.nova.algos.LongScalper";
  private static final String STRAT_BEANAME_2 = "longScalper";

  private static final String NEW_STRAT_NAME = "Short Position Scalper Algo";
  private static final String NEW_STRAT_DESCRIPTION = "Scalps and goes short...";
  private static final String NEW_STRAT_CLASSNAME = "com.gazbert.nova.algos.ShortScalper";

  private static final String BUY_PRICE_CONFIG_ITEM_KEY = "buy-price";
  private static final String BUY_PRICE_CONFIG_ITEM_VALUE = "671.15";
  private static final String AMOUNT_TO_BUY_CONFIG_ITEM_KEY = "buy-amount";
  private static final String AMOUNT_TO_BUY_CONFIG_ITEM_VALUE = "0.5";

  @Before
  public void setup() {
    PowerMock.mockStatic(ConfigurationManager.class);
  }

  @Test
  public void whenFindAllCalledThenExpectServiceToReturnAllStrategyConfigs() {
    expect(
            ConfigurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    PowerMock.replayAll();

    final StrategyConfigRepository strategyConfigRepository = new StrategyConfigYamlRepository();
    final List<StrategyConfig> strategyConfigItems = strategyConfigRepository.findAll();

    assertThat(strategyConfigItems.size()).isEqualTo(2);

    assertThat(strategyConfigItems.get(0).getId()).isEqualTo(STRAT_ID_1);
    assertThat(strategyConfigItems.get(0).getName()).isEqualTo(STRAT_NAME_1);
    assertThat(strategyConfigItems.get(0).getDescription()).isEqualTo(STRAT_DESCRIPTION_1);
    assertThat(strategyConfigItems.get(0).getClassName()).isEqualTo(STRAT_CLASSNAME_1);
    assertThat(strategyConfigItems.get(0).getConfigItems().containsKey(BUY_PRICE_CONFIG_ITEM_KEY))
        .isTrue();
    assertThat(
            strategyConfigItems.get(0).getConfigItems().containsValue(BUY_PRICE_CONFIG_ITEM_VALUE))
        .isTrue();
    assertThat(
            strategyConfigItems.get(0).getConfigItems().containsKey(AMOUNT_TO_BUY_CONFIG_ITEM_KEY))
        .isTrue();
    assertThat(
            strategyConfigItems
                .get(0)
                .getConfigItems()
                .containsValue(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE))
        .isTrue();

    assertThat(strategyConfigItems.get(1).getId()).isEqualTo(STRAT_ID_2);
    assertThat(strategyConfigItems.get(1).getName()).isEqualTo(STRAT_NAME_2);
    assertThat(strategyConfigItems.get(1).getDescription()).isEqualTo(STRAT_DESCRIPTION_2);
    assertThat(strategyConfigItems.get(1).getClassName()).isEqualTo(STRAT_CLASSNAME_2);
    assertThat(strategyConfigItems.get(1).getConfigItems().containsKey(BUY_PRICE_CONFIG_ITEM_KEY))
        .isTrue();
    assertThat(
            strategyConfigItems.get(1).getConfigItems().containsValue(BUY_PRICE_CONFIG_ITEM_VALUE))
        .isTrue();
    assertThat(
            strategyConfigItems.get(1).getConfigItems().containsKey(AMOUNT_TO_BUY_CONFIG_ITEM_KEY))
        .isTrue();
    assertThat(
            strategyConfigItems
                .get(1)
                .getConfigItems()
                .containsValue(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE))
        .isTrue();

    PowerMock.verifyAll();
  }

  @Test
  public void whenFindByIdCalledWithKnownIdThenReturnMatchingStrategyConfig() {
    expect(
            ConfigurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    PowerMock.replayAll();

    final StrategyConfigRepository strategyConfigRepository = new StrategyConfigYamlRepository();
    final StrategyConfig strategyConfig = strategyConfigRepository.findById(STRAT_ID_1);

    assertThat(strategyConfig.getId()).isEqualTo(STRAT_ID_1);
    assertThat(strategyConfig.getName()).isEqualTo(STRAT_NAME_1);
    assertThat(strategyConfig.getDescription()).isEqualTo(STRAT_DESCRIPTION_1);
    assertThat(strategyConfig.getClassName()).isEqualTo(STRAT_CLASSNAME_1);
    assertThat(strategyConfig.getConfigItems().containsKey(BUY_PRICE_CONFIG_ITEM_KEY)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsValue(BUY_PRICE_CONFIG_ITEM_VALUE)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsKey(AMOUNT_TO_BUY_CONFIG_ITEM_KEY)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsValue(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE))
        .isTrue();

    PowerMock.verifyAll();
  }

  @Test
  public void whenFindByIdCalledWithUnknownIdThenReturnNullStrategyConfig() {
    expect(
            ConfigurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    PowerMock.replayAll();

    final StrategyConfigRepository strategyConfigRepository = new StrategyConfigYamlRepository();
    final StrategyConfig strategyConfig = strategyConfigRepository.findById(UNKNOWN_STRAT_ID);

    assertThat(strategyConfig).isEqualTo(null);
    PowerMock.verifyAll();
  }

  @Test
  public void whenSaveCalledWithKnownIdThenReturnUpdatedStrategyConfig() {
    expect(
            ConfigurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    ConfigurationManager.saveConfig(
        eq(StrategiesType.class),
        anyObject(StrategiesType.class),
        eq(STRATEGIES_CONFIG_YAML_FILENAME));

    expect(
            ConfigurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    PowerMock.replayAll();

    final StrategyConfigRepository strategyConfigRepository = new StrategyConfigYamlRepository();
    final StrategyConfig strategyConfig =
        strategyConfigRepository.save(someExternalStrategyConfig());

    assertThat(strategyConfig.getId()).isEqualTo(STRAT_ID_1);
    assertThat(strategyConfig.getName()).isEqualTo(STRAT_NAME_1);
    assertThat(strategyConfig.getDescription()).isEqualTo(STRAT_DESCRIPTION_1);
    assertThat(strategyConfig.getClassName()).isEqualTo(STRAT_CLASSNAME_1);
    assertThat(strategyConfig.getConfigItems().containsKey(BUY_PRICE_CONFIG_ITEM_KEY)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsValue(BUY_PRICE_CONFIG_ITEM_VALUE)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsKey(AMOUNT_TO_BUY_CONFIG_ITEM_KEY)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsValue(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE))
        .isTrue();

    PowerMock.verifyAll();
  }

  @Test
  public void whenSaveCalledWithUnknownIdThenReturnEmptyStrategyConfig() {
    expect(
            ConfigurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    PowerMock.replayAll();

    final StrategyConfigRepository strategyConfigRepository = new StrategyConfigYamlRepository();
    final StrategyConfig strategyConfig =
        strategyConfigRepository.save(someExternalStrategyConfigWithUnknownId());

    assertThat(strategyConfig).isEqualTo(null);
    PowerMock.verifyAll();
  }

  @Test
  public void whenSaveCalledWithEmptyIdThenExpectCreatedStrategyConfigToBeReturned()
      throws Exception {
    expect(
            ConfigurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    ConfigurationManager.saveConfig(
        eq(StrategiesType.class),
        anyObject(StrategiesType.class),
        eq(STRATEGIES_CONFIG_YAML_FILENAME));

    expect(
            ConfigurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfigPlusNewOne());

    final StrategyConfigRepository strategyConfigRepository =
        PowerMock.createPartialMock(
            StrategyConfigYamlRepository.class, MOCKED_GENERATE_UUID_METHOD);
    PowerMock.expectPrivate(strategyConfigRepository, MOCKED_GENERATE_UUID_METHOD)
        .andReturn(GENERATED_STRAT_ID);

    PowerMock.replayAll();

    final StrategyConfig strategyConfig =
        strategyConfigRepository.save(someNewExternalStrategyConfig());

    assertThat(strategyConfig.getId()).isEqualTo(GENERATED_STRAT_ID);
    assertThat(strategyConfig.getName()).isEqualTo(NEW_STRAT_NAME);
    assertThat(strategyConfig.getDescription()).isEqualTo(NEW_STRAT_DESCRIPTION);
    assertThat(strategyConfig.getClassName()).isEqualTo(NEW_STRAT_CLASSNAME);
    assertThat(strategyConfig.getConfigItems().containsKey(BUY_PRICE_CONFIG_ITEM_KEY)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsValue(BUY_PRICE_CONFIG_ITEM_VALUE)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsKey(AMOUNT_TO_BUY_CONFIG_ITEM_KEY)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsValue(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE))
        .isTrue();

    PowerMock.verifyAll();
  }

  @Test
  public void whenDeleteCalledWithKnownIdThenReturnDeletedStrategyConfig() {
    expect(
            ConfigurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    ConfigurationManager.saveConfig(
        eq(StrategiesType.class),
        anyObject(StrategiesType.class),
        eq(STRATEGIES_CONFIG_YAML_FILENAME));

    PowerMock.replayAll();

    final StrategyConfigRepository strategyConfigRepository = new StrategyConfigYamlRepository();
    final StrategyConfig strategyConfig = strategyConfigRepository.delete(STRAT_ID_1);

    assertThat(strategyConfig.getId()).isEqualTo(STRAT_ID_1);
    assertThat(strategyConfig.getName()).isEqualTo(STRAT_NAME_1);
    assertThat(strategyConfig.getDescription()).isEqualTo(STRAT_DESCRIPTION_1);
    assertThat(strategyConfig.getClassName()).isEqualTo(STRAT_CLASSNAME_1);
    assertThat(strategyConfig.getConfigItems().containsKey(BUY_PRICE_CONFIG_ITEM_KEY)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsValue(BUY_PRICE_CONFIG_ITEM_VALUE)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsKey(AMOUNT_TO_BUY_CONFIG_ITEM_KEY)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsValue(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE))
        .isTrue();

    PowerMock.verifyAll();
  }

  @Test
  public void whenDeleteCalledWithUnknownIdThenReturnEmptyStrategyConfig() {
    expect(
            ConfigurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    PowerMock.replayAll();

    final StrategyConfigRepository strategyConfigRepository = new StrategyConfigYamlRepository();
    final StrategyConfig strategyConfig = strategyConfigRepository.delete(UNKNOWN_STRAT_ID);

    assertThat(strategyConfig).isEqualTo(null);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  // Private utils  // 私有工具
  // --------------------------------------------------------------------------

  private static StrategiesType allTheInternalStrategiesConfig() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

    final StrategyConfig strategyConfig1 = new StrategyConfig();
    strategyConfig1.setId(STRAT_ID_1);
    strategyConfig1.setName(STRAT_NAME_1);
    strategyConfig1.setDescription(STRAT_DESCRIPTION_1);
    strategyConfig1.setClassName(STRAT_CLASSNAME_1);
    strategyConfig1.setConfigItems(configItems);

    final StrategyConfig strategyConfig2 = new StrategyConfig();
    strategyConfig2.setId(STRAT_ID_2);
    strategyConfig2.setName(STRAT_NAME_2);
    strategyConfig2.setDescription(STRAT_DESCRIPTION_2);
    strategyConfig2.setClassName(STRAT_CLASSNAME_2);
    strategyConfig2.setConfigItems(configItems);

    final StrategiesType strategiesType = new StrategiesType();
    strategiesType.getStrategies().add(strategyConfig1);
    strategiesType.getStrategies().add(strategyConfig2);

    return strategiesType;
  }

  private static StrategiesType allTheInternalStrategiesConfigPlusNewOne() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

    final StrategyConfig newStrat = new StrategyConfig();
    newStrat.setId(GENERATED_STRAT_ID);
    newStrat.setName(NEW_STRAT_NAME);
    newStrat.setDescription(NEW_STRAT_DESCRIPTION);
    newStrat.setClassName(NEW_STRAT_CLASSNAME);
    newStrat.setConfigItems(configItems);

    final StrategiesType existingStatsPlusNewOne = allTheInternalStrategiesConfig();
    existingStatsPlusNewOne.getStrategies().add(newStrat);
    return existingStatsPlusNewOne;
  }

  private static StrategyConfig someExternalStrategyConfig() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
    return new StrategyConfig(
        STRAT_ID_1,
        STRAT_NAME_1,
        STRAT_DESCRIPTION_1,
        STRAT_CLASSNAME_1,
        STRAT_BEANAME_1,
        configItems);
  }

  private static StrategyConfig someNewExternalStrategyConfig() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
    return new StrategyConfig(
        null,
        NEW_STRAT_NAME,
        NEW_STRAT_DESCRIPTION,
        NEW_STRAT_CLASSNAME,
        STRAT_BEANAME_2,
        configItems);
  }

  private static StrategyConfig someExternalStrategyConfigWithUnknownId() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
    return new StrategyConfig(
        UNKNOWN_STRAT_ID,
        STRAT_NAME_1,
        STRAT_DESCRIPTION_1,
        STRAT_CLASSNAME_1,
        STRAT_BEANAME_1,
        configItems);
  }
}
