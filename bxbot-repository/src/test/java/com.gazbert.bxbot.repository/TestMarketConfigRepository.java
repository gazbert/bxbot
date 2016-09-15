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

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.market.generated.MarketType;
import com.gazbert.bxbot.datastore.market.generated.MarketsType;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.repository.impl.MarketConfigRepositoryXmlImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static com.gazbert.bxbot.datastore.FileLocations.MARKETS_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.datastore.FileLocations.MARKETS_CONFIG_XSD_FILENAME;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

/**
 * Tests Market configuration repository behaves as expected.
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationManager.class})
public class TestMarketConfigRepository {

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


    @Before
    public void setup() throws Exception {
        PowerMock.mockStatic(ConfigurationManager.class);
    }

    @Test
    public void whenFindAllMarketsCalledThenExpectServiceToReturnThemAll() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        PowerMock.replayAll();

        final MarketConfigRepository marketConfigRepository = new MarketConfigRepositoryXmlImpl();
        final List<MarketConfig> marketConfigItems = marketConfigRepository.findAllMarkets();

        assertThat(marketConfigItems.size()).isEqualTo(2);

        assertThat(marketConfigItems.get(0).getId()).isEqualTo(MARKET_1_ID);
        assertThat(marketConfigItems.get(0).getLabel()).isEqualTo(MARKET_1_LABEL);
        assertThat(marketConfigItems.get(0).isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
        assertThat(marketConfigItems.get(0).getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
        assertThat(marketConfigItems.get(0).getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
        assertThat(marketConfigItems.get(0).getTradingStrategy()).isEqualTo(MARKET_1_TRADING_STRATEGY);

        assertThat(marketConfigItems.get(1).getId()).isEqualTo(MARKET_2_ID);
        assertThat(marketConfigItems.get(1).getLabel()).isEqualTo(MARKET_2_LABEL);
        assertThat(marketConfigItems.get(1).isEnabled()).isEqualTo(MARKET_2_IS_ENABLED);
        assertThat(marketConfigItems.get(1).getBaseCurrency()).isEqualTo(MARKET_2_BASE_CURRENCY);
        assertThat(marketConfigItems.get(1).getCounterCurrency()).isEqualTo(MARKET_2_COUNTER_CURRENCY);
        assertThat(marketConfigItems.get(1).getTradingStrategy()).isEqualTo(MARKET_2_TRADING_STRATEGY);

        PowerMock.verifyAll();
    }

    @Test
    public void whenFindByIdCalledWithRecognizedIdThenReturnMatchingMarket() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        PowerMock.replayAll();

        final MarketConfigRepository marketConfigRepository = new MarketConfigRepositoryXmlImpl();
        final MarketConfig marketConfig = marketConfigRepository.findById(MARKET_1_ID);

        assertThat(marketConfig.getId()).isEqualTo(MARKET_1_ID);
        assertThat(marketConfig.getLabel()).isEqualTo(MARKET_1_LABEL);
        assertThat(marketConfig.isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
        assertThat(marketConfig.getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
        assertThat(marketConfig.getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
        assertThat(marketConfig.getTradingStrategy()).isEqualTo(MARKET_1_TRADING_STRATEGY);

        PowerMock.verifyAll();
    }

    @Test
    public void whenFindByIdCalledWithUnrecognizedIdThenReturnEmptyMarket() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        PowerMock.replayAll();

        final MarketConfigRepository marketConfigRepository = new MarketConfigRepositoryXmlImpl();
        final MarketConfig marketConfig = marketConfigRepository.findById("unknown-id");

        assertThat(marketConfig.getId()).isNull();
        assertThat(marketConfig.getLabel()).isNull();
        assertThat(marketConfig.isEnabled()).isFalse();
        assertThat(marketConfig.getBaseCurrency()).isNull();
        assertThat(marketConfig.getCounterCurrency()).isNull();
        assertThat(marketConfig.getTradingStrategy()).isNull();

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static MarketsType allTheInternalMarketsConfig() {

        final MarketType marketType1 = new MarketType();
        marketType1.setId(MARKET_1_ID);
        marketType1.setLabel(MARKET_1_LABEL);
        marketType1.setEnabled(MARKET_1_IS_ENABLED);
        marketType1.setBaseCurrency(MARKET_1_BASE_CURRENCY);
        marketType1.setCounterCurrency(MARKET_1_COUNTER_CURRENCY);
        marketType1.setTradingStrategy(MARKET_1_TRADING_STRATEGY);

        final MarketType marketType2 = new MarketType();
        marketType2.setId(MARKET_2_ID);
        marketType2.setLabel(MARKET_2_LABEL);
        marketType2.setEnabled(MARKET_2_IS_ENABLED);
        marketType2.setBaseCurrency(MARKET_2_BASE_CURRENCY);
        marketType2.setCounterCurrency(MARKET_2_COUNTER_CURRENCY);
        marketType2.setTradingStrategy(MARKET_2_TRADING_STRATEGY);

        final MarketsType marketsType = new MarketsType();
        marketsType.getMarkets().add(marketType1);
        marketsType.getMarkets().add(marketType2);

        return marketsType;
    }
}
