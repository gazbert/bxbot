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

package com.gazbert.bxbot.datastore.config.market;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.market.generated.MarketType;
import com.gazbert.bxbot.datastore.market.generated.MarketsType;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Files;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * Tests the Market configuration is loaded as expected.
 *
 * @author gazbert
 */
public class TestMarketConfigurationManagement {

    /* Production XSD */
    private static final String XML_SCHEMA_FILENAME = "com/gazbert/bxbot/datastore/config/market/markets.xsd";

    /* Test XML config */
    private static final String VALID_XML_CONFIG_FILENAME = "src/test/config/markets/valid-markets.xml";
    private static final String MISSING_XML_CONFIG_FILENAME = "src/test/config/markets/missing-markets.xml";
    private static final String XML_CONFIG_TO_SAVE_FILENAME = "src/test/config/markets/saved-markets.xml";

    private static final String MARKET_1_LABEL = "BTC/USD";
    private static final String MARKET_1_ID = "gemini_usd/btc";
    private static final String MARKET_1_BASE_CURRENCY = "BTC";
    private static final String MARKET_1_COUNTER_CURRENCY = "USD";
    private static final boolean MARKET_1_IS_ENABLED = true;
    private static final String MARKET_1_TRADING_STRATEGY = "macd_trend_follower";

    private static final String MARKET_2_LABEL = "BTC/GBP";
    private static final String MARKET_2_ID = "gdax_gbp/btc";
    private static final String MARKET_2_BASE_CURRENCY = "BTC";
    private static final String MARKET_2_COUNTER_CURRENCY = "GBP";
    private static final boolean MARKET_2_IS_ENABLED = false;
    private static final String MARKET_2_TRADING_STRATEGY = "scalper";


    @Test
    public void testLoadingValidXmlConfigFileIsSuccessful() {

        final MarketsType marketsType = ConfigurationManager.loadConfig(MarketsType.class,
                VALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);

        assertEquals(2, marketsType.getMarkets().size());

        assertEquals("BTC/USD", marketsType.getMarkets().get(0).getLabel());
        assertEquals("btc_usd", marketsType.getMarkets().get(0).getId());
        assertEquals("BTC", marketsType.getMarkets().get(0).getBaseCurrency());
        assertEquals("USD", marketsType.getMarkets().get(0).getCounterCurrency());
        assertTrue(marketsType.getMarkets().get(0).isEnabled());
        assertEquals("scalping-strategy", marketsType.getMarkets().get(0).getTradingStrategy());

        assertEquals("LTC/BTC", marketsType.getMarkets().get(1).getLabel());
        assertEquals("ltc_usd", marketsType.getMarkets().get(1).getId());
        assertEquals("LTC", marketsType.getMarkets().get(1).getBaseCurrency());
        assertEquals("BTC", marketsType.getMarkets().get(1).getCounterCurrency());
        assertFalse(marketsType.getMarkets().get(1).isEnabled());
        assertEquals("scalping-strategy", marketsType.getMarkets().get(1).getTradingStrategy());
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadingMissingXmlConfigFileThrowsException() {

        ConfigurationManager.loadConfig(MarketsType.class,
                MISSING_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }

    @Test
    public void testSavingConfigToXmlIsSuccessful() throws Exception {

        final MarketType market1 = new MarketType();
        market1.setEnabled(MARKET_1_IS_ENABLED);
        market1.setId(MARKET_1_ID);
        market1.setLabel(MARKET_1_LABEL);
        market1.setBaseCurrency(MARKET_1_BASE_CURRENCY);
        market1.setCounterCurrency(MARKET_1_COUNTER_CURRENCY);
        market1.setTradingStrategy(MARKET_1_TRADING_STRATEGY);

        final MarketType market2 = new MarketType();
        market2.setEnabled(MARKET_2_IS_ENABLED);
        market2.setId(MARKET_2_ID);
        market2.setLabel(MARKET_2_LABEL);
        market2.setBaseCurrency(MARKET_2_BASE_CURRENCY);
        market2.setCounterCurrency(MARKET_2_COUNTER_CURRENCY);
        market2.setTradingStrategy(MARKET_2_TRADING_STRATEGY);

        final MarketsType marketsConfig = new MarketsType();
        marketsConfig.getMarkets().add(market1);
        marketsConfig.getMarkets().add(market2);

        ConfigurationManager.saveConfig(MarketsType.class, marketsConfig, XML_CONFIG_TO_SAVE_FILENAME);

        // Read it back in
        final MarketsType marketsReloaded = ConfigurationManager.loadConfig(MarketsType.class,
                XML_CONFIG_TO_SAVE_FILENAME, XML_SCHEMA_FILENAME);

        assertThat(marketsReloaded.getMarkets().get(0).isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
        assertThat(marketsReloaded.getMarkets().get(0).getId()).isEqualTo(MARKET_1_ID);
        assertThat(marketsReloaded.getMarkets().get(0).getLabel()).isEqualTo(MARKET_1_LABEL);
        assertThat(marketsReloaded.getMarkets().get(0).getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
        assertThat(marketsReloaded.getMarkets().get(0).getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
        assertThat(marketsReloaded.getMarkets().get(0).getTradingStrategy()).isEqualTo(MARKET_1_TRADING_STRATEGY);

        assertThat(marketsReloaded.getMarkets().get(1).isEnabled()).isEqualTo(MARKET_2_IS_ENABLED);
        assertThat(marketsReloaded.getMarkets().get(1).getId()).isEqualTo(MARKET_2_ID);
        assertThat(marketsReloaded.getMarkets().get(1).getLabel()).isEqualTo(MARKET_2_LABEL);
        assertThat(marketsReloaded.getMarkets().get(1).getBaseCurrency()).isEqualTo(MARKET_2_BASE_CURRENCY);
        assertThat(marketsReloaded.getMarkets().get(1).getCounterCurrency()).isEqualTo(MARKET_2_COUNTER_CURRENCY);
        assertThat(marketsReloaded.getMarkets().get(1).getTradingStrategy()).isEqualTo(MARKET_2_TRADING_STRATEGY);

        // cleanup
        Files.delete(FileSystems.getDefault().getPath(XML_CONFIG_TO_SAVE_FILENAME));
    }
}
