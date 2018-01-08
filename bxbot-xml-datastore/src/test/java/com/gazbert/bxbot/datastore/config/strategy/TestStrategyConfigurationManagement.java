/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015. Gareth Jon Lynch
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

package com.gazbert.bxbot.datastore.config.strategy;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.strategy.generated.ConfigItemType;
import com.gazbert.bxbot.datastore.strategy.generated.OptionalConfigType;
import com.gazbert.bxbot.datastore.strategy.generated.StrategyType;
import com.gazbert.bxbot.datastore.strategy.generated.TradingStrategiesType;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Files;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * Tests the Trading Strategy configuration is loaded as expected.
 *
 * @author gazbert
 */
public class TestStrategyConfigurationManagement {

    /* Production XSD */
    private static final String XML_SCHEMA_FILENAME = "com/gazbert/bxbot/datastore/config/strategies.xsd";

    /* Test XML config */
    private static final String VALID_XML_CONFIG_FILENAME = "src/test/config/strategies/valid-strategies.xml";
    private static final String INVALID_XML_CONFIG_FILENAME = "src/test/config/strategies/invalid-strategies.xml";
    private static final String MISSING_XML_CONFIG_FILENAME = "src/test/config/strategies/missing-strategies.xml";
    private static final String XML_CONFIG_TO_SAVE_FILENAME = "src/test/config/strategies/saved-strategies.xml";
    private static final String INVALID_STRATEGY_INJECTION_CONFIG_FILENAME = "src/test/config/strategies/invalid-strategy-injection.xml";

    private static final String STRAT_ID_1 = "macd-long-position";
    private static final String STRAT_NAME_1 = "MACD Long Position Algo";
    private static final String STRAT_DESCRIPTION_1 = "Uses MACD as indicator and takes long position in base currency.";
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
    public void testLoadingValidXmlConfigFileIsSuccessful() {

        final TradingStrategiesType tradingStrategiesType = ConfigurationManager.loadConfig(TradingStrategiesType.class,
                VALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);

        assertEquals(3, tradingStrategiesType.getStrategies().size());

        /*
         * Strat 1
         */
        assertEquals("scalping-strategy", tradingStrategiesType.getStrategies().get(0).getId());
        assertEquals("Basic Scalping Strat", tradingStrategiesType.getStrategies().get(0).getName());
        assertTrue(tradingStrategiesType.getStrategies().get(0).getDescription().trim().equals(
                "A simple trend following scalper that buys at the current BID price, holds until current market " +
                "price has reached a configurable minimum percentage gain, and then sells at current ASK price, thereby " +
                "taking profit from the spread. Don't forget to factor in the exchange fees!"));
        assertEquals("com.gazbert.bxbot.strategies.ExampleScalpingStrategy", tradingStrategiesType.getStrategies().get(0).getClassName());
        assertNull(tradingStrategiesType.getStrategies().get(0).getBeanName());

        assertTrue(2 == tradingStrategiesType.getStrategies().get(0).getOptionalConfig().getConfigItem().size());
        assertEquals("counter-currency-buy-order-amount", tradingStrategiesType.getStrategies().get(0).getOptionalConfig().getConfigItem().get(0).getName());
        assertEquals("20", tradingStrategiesType.getStrategies().get(0).getOptionalConfig().getConfigItem().get(0).getValue());
        assertEquals("minimum-percentage-gain", tradingStrategiesType.getStrategies().get(0).getOptionalConfig().getConfigItem().get(1).getName());
        assertEquals("1", tradingStrategiesType.getStrategies().get(0).getOptionalConfig().getConfigItem().get(1).getValue());

        /*
         * Strat 2
         */
        assertEquals("ema-shorting-strategy", tradingStrategiesType.getStrategies().get(1).getId());
        assertEquals("EMA Based Shorting Strat", tradingStrategiesType.getStrategies().get(1).getName());
        assertNull(tradingStrategiesType.getStrategies().get(1).getDescription()); // optional element check
        assertEquals("com.gazbert.bxbot.strategies.YourEmaShortingStrategy", tradingStrategiesType.getStrategies().get(1).getClassName());
        assertNull(tradingStrategiesType.getStrategies().get(0).getBeanName());

        assertTrue(4 == tradingStrategiesType.getStrategies().get(1).getOptionalConfig().getConfigItem().size());

        assertEquals("btc-sell-order-amount", tradingStrategiesType.getStrategies().get(1).getOptionalConfig().getConfigItem().get(0).getName());
        assertEquals("0.5", tradingStrategiesType.getStrategies().get(1).getOptionalConfig().getConfigItem().get(0).getValue());

        assertEquals("shortEmaInterval", tradingStrategiesType.getStrategies().get(1).getOptionalConfig().getConfigItem().get(1).getName());
        assertEquals("5", tradingStrategiesType.getStrategies().get(1).getOptionalConfig().getConfigItem().get(1).getValue());

        assertEquals("mediumEmaInterval", tradingStrategiesType.getStrategies().get(1).getOptionalConfig().getConfigItem().get(2).getName());
        assertEquals("10", tradingStrategiesType.getStrategies().get(1).getOptionalConfig().getConfigItem().get(2).getValue());

        assertEquals("longEmaInterval", tradingStrategiesType.getStrategies().get(1).getOptionalConfig().getConfigItem().get(3).getName());
        assertEquals("20", tradingStrategiesType.getStrategies().get(1).getOptionalConfig().getConfigItem().get(3).getValue());

        /*
         * Strat 3
         */
        assertEquals("macd-strategy", tradingStrategiesType.getStrategies().get(2).getId());
        assertEquals("MACD Based Strat", tradingStrategiesType.getStrategies().get(2).getName());
        assertTrue(tradingStrategiesType.getStrategies().get(2).getDescription().trim().equals(
                "Strat uses MACD data to take long position in USD."));
        assertEquals("myMacdStratBean", tradingStrategiesType.getStrategies().get(2).getBeanName());
        assertNull(tradingStrategiesType.getStrategies().get(2).getClassName());
        assertNull(tradingStrategiesType.getStrategies().get(2).getOptionalConfig()); // optional element check
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadingMissingXmlConfigFileThrowsException() {

        ConfigurationManager.loadConfig(TradingStrategiesType.class,
                MISSING_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadingInvalidXmlConfigFileThrowsException() {

        ConfigurationManager.loadConfig(TradingStrategiesType.class,
                INVALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }    

    @Test
    public void testSavingConfigToXmlIsSuccessful() throws Exception {

        final ConfigItemType configItem1 = new ConfigItemType();
        configItem1.setName(BUY_PRICE_CONFIG_ITEM_KEY);
        configItem1.setValue(BUY_PRICE_CONFIG_ITEM_VALUE);

        final ConfigItemType configItem2 = new ConfigItemType();
        configItem2.setName(AMOUNT_TO_BUY_CONFIG_ITEM_KEY);
        configItem2.setValue(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

        // Strat 1
        final OptionalConfigType strat1Config = new OptionalConfigType();
        strat1Config.getConfigItem().add(configItem1);
        strat1Config.getConfigItem().add(configItem2);

        final StrategyType strategy1 = new StrategyType();
        strategy1.setId(STRAT_ID_1);
        strategy1.setName(STRAT_NAME_1);
        strategy1.setDescription(STRAT_DESCRIPTION_1);
        strategy1.setClassName(STRAT_CLASSNAME_1);
        strategy1.setOptionalConfig(strat1Config);

        // Strat 2
        final OptionalConfigType strat2Config = new OptionalConfigType();
        strat2Config.getConfigItem().add(configItem1);
        strat2Config.getConfigItem().add(configItem2);

        final StrategyType strategy2 = new StrategyType();
        strategy2.setId(STRAT_ID_2);
        strategy2.setName(STRAT_NAME_2);
        strategy2.setDescription(STRAT_DESCRIPTION_2);
        strategy2.setBeanName(STRAT_BEAN_NAME_2);
        strategy2.setOptionalConfig(strat2Config);

        final TradingStrategiesType strategiesConfig = new TradingStrategiesType();
        strategiesConfig.getStrategies().add(strategy1);
        strategiesConfig.getStrategies().add(strategy2);

        ConfigurationManager.saveConfig(TradingStrategiesType.class, strategiesConfig, XML_CONFIG_TO_SAVE_FILENAME);

        // Read it back in
        final TradingStrategiesType strategiesReloaded = ConfigurationManager.loadConfig(TradingStrategiesType.class,
                XML_CONFIG_TO_SAVE_FILENAME, XML_SCHEMA_FILENAME);

        // Strat 1
        assertThat(strategiesReloaded.getStrategies().get(0).getId()).isEqualTo(STRAT_ID_1);
        assertThat(strategiesReloaded.getStrategies().get(0).getName()).isEqualTo(STRAT_NAME_1);
        assertThat(strategiesReloaded.getStrategies().get(0).getDescription()).isEqualTo(STRAT_DESCRIPTION_1);
        assertThat(strategiesReloaded.getStrategies().get(0).getClassName()).isEqualTo(STRAT_CLASSNAME_1);
        assertThat(strategiesReloaded.getStrategies().get(0).getBeanName()).isNull();

        assertThat(strategiesReloaded.getStrategies().get(0).getOptionalConfig().getConfigItem().get(0).getName())
                .isEqualTo(BUY_PRICE_CONFIG_ITEM_KEY);
        assertThat(strategiesReloaded.getStrategies().get(0).getOptionalConfig().getConfigItem().get(0).getValue())
                .isEqualTo(BUY_PRICE_CONFIG_ITEM_VALUE);
        assertThat(strategiesReloaded.getStrategies().get(0).getOptionalConfig().getConfigItem().get(1).getName())
                .isEqualTo(AMOUNT_TO_BUY_CONFIG_ITEM_KEY);
        assertThat(strategiesReloaded.getStrategies().get(0).getOptionalConfig().getConfigItem().get(1).getValue())
                .isEqualTo(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

        // Strat 2
        assertThat(strategiesReloaded.getStrategies().get(1).getId()).isEqualTo(STRAT_ID_2);
        assertThat(strategiesReloaded.getStrategies().get(1).getName()).isEqualTo(STRAT_NAME_2);
        assertThat(strategiesReloaded.getStrategies().get(1).getDescription()).isEqualTo(STRAT_DESCRIPTION_2);
        assertThat(strategiesReloaded.getStrategies().get(1).getClassName()).isNull();
        assertThat(strategiesReloaded.getStrategies().get(1).getBeanName()).isEqualTo(STRAT_BEAN_NAME_2);

        assertThat(strategiesReloaded.getStrategies().get(1).getOptionalConfig().getConfigItem().get(0).getName())
                .isEqualTo(BUY_PRICE_CONFIG_ITEM_KEY);
        assertThat(strategiesReloaded.getStrategies().get(1).getOptionalConfig().getConfigItem().get(0).getValue())
                .isEqualTo(BUY_PRICE_CONFIG_ITEM_VALUE);
        assertThat(strategiesReloaded.getStrategies().get(1).getOptionalConfig().getConfigItem().get(1).getName())
                .isEqualTo(AMOUNT_TO_BUY_CONFIG_ITEM_KEY);
        assertThat(strategiesReloaded.getStrategies().get(1).getOptionalConfig().getConfigItem().get(1).getValue())
                .isEqualTo(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

        // cleanup
        Files.delete(FileSystems.getDefault().getPath(XML_CONFIG_TO_SAVE_FILENAME));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadingInvalidStrategyInjectionConfigFileThrowsException() {
        ConfigurationManager.loadConfig(TradingStrategiesType.class,
                INVALID_STRATEGY_INJECTION_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }
}
