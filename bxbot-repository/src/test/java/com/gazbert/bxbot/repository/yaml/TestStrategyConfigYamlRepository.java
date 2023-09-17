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
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests YAML backed Strategy configuration repository behaves as expected.
 *
 * @author gazbert
 */
class TestStrategyConfigYamlRepository {

  private static final String UNKNOWN_STRAT_ID = "unknown-or-new-strat-id";

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

  private ConfigurationManager configurationManager;

  @BeforeEach
  void setup() {
    configurationManager = EasyMock.createMock(ConfigurationManager.class);
  }

  @Test
  void whenFindAllCalledThenExpectServiceToReturnAllStrategyConfigs() {
    expect(
            configurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    EasyMock.replay(configurationManager);

    final StrategyConfigRepository strategyConfigRepository =
        new StrategyConfigYamlRepository(configurationManager);
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

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenFindByIdCalledWithKnownIdThenReturnMatchingStrategyConfig() {
    expect(
            configurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    EasyMock.replay(configurationManager);

    final StrategyConfigRepository strategyConfigRepository =
        new StrategyConfigYamlRepository(configurationManager);
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

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenFindByIdCalledWithUnknownIdThenReturnNullStrategyConfig() {
    expect(
            configurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    EasyMock.replay(configurationManager);

    final StrategyConfigRepository strategyConfigRepository =
        new StrategyConfigYamlRepository(configurationManager);
    final StrategyConfig strategyConfig = strategyConfigRepository.findById(UNKNOWN_STRAT_ID);

    assertThat(strategyConfig).isEqualTo(null);
    EasyMock.verify(configurationManager);
  }

  @Test
  void whenSaveCalledWithKnownIdThenReturnUpdatedStrategyConfig() {
    expect(
            configurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    configurationManager.saveConfig(
        eq(StrategiesType.class),
        anyObject(StrategiesType.class),
        eq(STRATEGIES_CONFIG_YAML_FILENAME));

    expect(
            configurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    EasyMock.replay(configurationManager);

    final StrategyConfigRepository strategyConfigRepository =
        new StrategyConfigYamlRepository(configurationManager);
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

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenSaveCalledWithUnknownIdThenReturnEmptyStrategyConfig() {
    expect(
            configurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    EasyMock.replay(configurationManager);

    final StrategyConfigRepository strategyConfigRepository =
        new StrategyConfigYamlRepository(configurationManager);
    final StrategyConfig strategyConfig =
        strategyConfigRepository.save(someExternalStrategyConfigWithUnknownId());

    assertThat(strategyConfig).isEqualTo(null);
    EasyMock.verify(configurationManager);
  }

  @Test
  void whenSaveCalledWithEmptyIdThenExpectCreatedStrategyConfigToBeReturned() {

    expect(
            configurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    configurationManager.saveConfig(
        eq(StrategiesType.class),
        anyObject(StrategiesType.class),
        eq(STRATEGIES_CONFIG_YAML_FILENAME));

    EasyMock.replay(configurationManager);

    final StrategyConfigRepository strategyConfigRepository =
        new StrategyConfigYamlRepository(configurationManager);

    final StrategyConfig strategyConfig =
        strategyConfigRepository.save(someNewExternalStrategyConfig());

    assertThat(strategyConfig.getId()).isNotBlank();
    assertThat(strategyConfig.getName()).isEqualTo(NEW_STRAT_NAME);
    assertThat(strategyConfig.getDescription()).isEqualTo(NEW_STRAT_DESCRIPTION);
    assertThat(strategyConfig.getClassName()).isEqualTo(NEW_STRAT_CLASSNAME);
    assertThat(strategyConfig.getConfigItems().containsKey(BUY_PRICE_CONFIG_ITEM_KEY)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsValue(BUY_PRICE_CONFIG_ITEM_VALUE)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsKey(AMOUNT_TO_BUY_CONFIG_ITEM_KEY)).isTrue();
    assertThat(strategyConfig.getConfigItems().containsValue(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE))
        .isTrue();

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenDeleteCalledWithKnownIdThenReturnDeletedStrategyConfig() {
    expect(
            configurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    configurationManager.saveConfig(
        eq(StrategiesType.class),
        anyObject(StrategiesType.class),
        eq(STRATEGIES_CONFIG_YAML_FILENAME));

    EasyMock.replay(configurationManager);

    final StrategyConfigRepository strategyConfigRepository =
        new StrategyConfigYamlRepository(configurationManager);
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

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenDeleteCalledWithUnknownIdThenReturnEmptyStrategyConfig() {
    expect(
            configurationManager.loadConfig(
                eq(StrategiesType.class), eq(STRATEGIES_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalStrategiesConfig());

    EasyMock.replay(configurationManager);

    final StrategyConfigRepository strategyConfigRepository =
        new StrategyConfigYamlRepository(configurationManager);
    final StrategyConfig strategyConfig = strategyConfigRepository.delete(UNKNOWN_STRAT_ID);

    assertThat(strategyConfig).isEqualTo(null);
    EasyMock.verify(configurationManager);
  }

  // --------------------------------------------------------------------------
  // Private utils
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
