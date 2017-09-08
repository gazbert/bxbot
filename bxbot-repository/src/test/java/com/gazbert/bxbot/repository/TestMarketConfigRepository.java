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
import com.gazbert.bxbot.repository.impl.MarketConfigRepositoryXmlDatastore;
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
import static org.easymock.EasyMock.*;

/**
 * Tests Market configuration repository behaves as expected.
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationManager.class, MarketConfigRepositoryXmlDatastore.class})
public class TestMarketConfigRepository {

    // Mocked out methods
    private static final String MOCKED_GENERATE_UUID_METHOD = "generateUuid";

    private static final String UNKNOWN_MARKET_ID = "unknown-or-new-market-id";
    private static final String GENERATED_MARKET_ID = "new-market-id-123";

    private static final String MARKET_1_ID = "gemini_usd/btc";
    private static final String MARKET_1_NAME = "BTC/USD";
    private static final String MARKET_1_BASE_CURRENCY = "BTC";
    private static final String MARKET_1_COUNTER_CURRENCY = "USD";
    private static final boolean MARKET_1_IS_ENABLED = true;
    private static final String MARKET_1_TRADING_STRATEGY_ID = "macd_trend_follower";

    private static final String MARKET_2_ID = "gdax_gbp/btc";
    private static final String MARKET_2_NAME = "BTC/GBP";
    private static final String MARKET_2_BASE_CURRENCY = "BTC";
    private static final String MARKET_2_COUNTER_CURRENCY = "GBP";
    private static final boolean MARKET_2_IS_ENABLED = false;
    private static final String MARKET_2_TRADING_STRATEGY_ID = "scalper";

    private static final String NEW_MARKET_NAME = "BTC/ETH";
    private static final String NEW_MARKET_BASE_CURRENCY = "BTC";
    private static final String NEW_MARKET_COUNTER_CURRENCY = "ETH";
    private static final boolean NEW_MARKET_IS_ENABLED = false;
    private static final String NEW_MARKET_TRADING_STRATEGY_ID = "macd-jobby";


    @Before
    public void setup() throws Exception {
        PowerMock.mockStatic(ConfigurationManager.class);
    }

    @Test
    public void whenFindAllCalledThenExpectServiceToReturnAllMarketConfigs() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        PowerMock.replayAll();

        final MarketConfigRepository marketConfigRepository = new MarketConfigRepositoryXmlDatastore();
        final List<MarketConfig> marketConfigItems = marketConfigRepository.findAll();

        assertThat(marketConfigItems.size()).isEqualTo(2);

        assertThat(marketConfigItems.get(0).getId()).isEqualTo(MARKET_1_ID);
        assertThat(marketConfigItems.get(0).getName()).isEqualTo(MARKET_1_NAME);
        assertThat(marketConfigItems.get(0).isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
        assertThat(marketConfigItems.get(0).getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
        assertThat(marketConfigItems.get(0).getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
        assertThat(marketConfigItems.get(0).getTradingStrategyId()).isEqualTo(MARKET_1_TRADING_STRATEGY_ID);

        assertThat(marketConfigItems.get(1).getId()).isEqualTo(MARKET_2_ID);
        assertThat(marketConfigItems.get(1).getName()).isEqualTo(MARKET_2_NAME);
        assertThat(marketConfigItems.get(1).isEnabled()).isEqualTo(MARKET_2_IS_ENABLED);
        assertThat(marketConfigItems.get(1).getBaseCurrency()).isEqualTo(MARKET_2_BASE_CURRENCY);
        assertThat(marketConfigItems.get(1).getCounterCurrency()).isEqualTo(MARKET_2_COUNTER_CURRENCY);
        assertThat(marketConfigItems.get(1).getTradingStrategyId()).isEqualTo(MARKET_2_TRADING_STRATEGY_ID);

        PowerMock.verifyAll();
    }

    @Test
    public void whenFindByIdCalledWithKnownIdThenReturnMatchingMarketConfig() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        PowerMock.replayAll();

        final MarketConfigRepository marketConfigRepository = new MarketConfigRepositoryXmlDatastore();
        final MarketConfig marketConfig = marketConfigRepository.findById(MARKET_1_ID);

        assertThat(marketConfig.getId()).isEqualTo(MARKET_1_ID);
        assertThat(marketConfig.getName()).isEqualTo(MARKET_1_NAME);
        assertThat(marketConfig.isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
        assertThat(marketConfig.getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
        assertThat(marketConfig.getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
        assertThat(marketConfig.getTradingStrategyId()).isEqualTo(MARKET_1_TRADING_STRATEGY_ID);

        PowerMock.verifyAll();
    }

    @Test
    public void whenFindByIdCalledWithUnknownIdThenReturnEmptyMarketConfig() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        PowerMock.replayAll();

        final MarketConfigRepository marketConfigRepository = new MarketConfigRepositoryXmlDatastore();
        final MarketConfig marketConfig = marketConfigRepository.findById(UNKNOWN_MARKET_ID);

        assertThat(marketConfig.getId()).isNull();
        assertThat(marketConfig.getName()).isNull();
        assertThat(marketConfig.isEnabled()).isFalse();
        assertThat(marketConfig.getBaseCurrency()).isNull();
        assertThat(marketConfig.getCounterCurrency()).isNull();
        assertThat(marketConfig.getTradingStrategyId()).isNull();

        PowerMock.verifyAll();
    }

    @Test
    public void whenSaveCalledWithKnownIdThenReturnUpdatedMarketConfig() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        ConfigurationManager.saveConfig(
                eq(MarketsType.class),
                anyObject(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME));

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        PowerMock.replayAll();

        final MarketConfigRepository marketConfigRepository = new MarketConfigRepositoryXmlDatastore();
        final MarketConfig marketConfig = marketConfigRepository.save(someExternalMarketConfig());

        assertThat(marketConfig.getId()).isEqualTo(MARKET_1_ID);
        assertThat(marketConfig.getName()).isEqualTo(MARKET_1_NAME);
        assertThat(marketConfig.isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
        assertThat(marketConfig.getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
        assertThat(marketConfig.getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
        assertThat(marketConfig.getTradingStrategyId()).isEqualTo(MARKET_1_TRADING_STRATEGY_ID);

        PowerMock.verifyAll();
    }

    @Test
    public void whenSaveCalledWithUnknownIdThenReturnEmptyMarketConfig() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        PowerMock.replayAll();

        final MarketConfigRepository marketConfigRepository = new MarketConfigRepositoryXmlDatastore();
        final MarketConfig marketConfig = marketConfigRepository.save(someExternalMarketConfigWithUnknownId());

        assertThat(marketConfig.getId()).isEqualTo(null);
        assertThat(marketConfig.getName()).isEqualTo(null);
        assertThat(marketConfig.getBaseCurrency()).isEqualTo(null);
        assertThat(marketConfig.getCounterCurrency()).isEqualTo(null);
        assertThat(marketConfig.getTradingStrategyId()).isEqualTo(null);
        assertThat(marketConfig.isEnabled()).isFalse();

        PowerMock.verifyAll();
    }

    @Test
    public void whenSaveCalledWithEmptyIdThenExpectCreatedMarketConfigToBeReturned() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        ConfigurationManager.saveConfig(
                eq(MarketsType.class),
                anyObject(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME));

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfigPlusNewOne());

        final MarketConfigRepository marketConfigRepository = PowerMock.createPartialMock(
                MarketConfigRepositoryXmlDatastore.class, MOCKED_GENERATE_UUID_METHOD);
        PowerMock.expectPrivate(marketConfigRepository, MOCKED_GENERATE_UUID_METHOD).andReturn(GENERATED_MARKET_ID);

        PowerMock.replayAll();

        final MarketConfig marketConfig = marketConfigRepository.save(someNewExternalMarketConfig());

        assertThat(marketConfig.getId()).isEqualTo(GENERATED_MARKET_ID);
        assertThat(marketConfig.getName()).isEqualTo(NEW_MARKET_NAME);
        assertThat(marketConfig.isEnabled()).isEqualTo(NEW_MARKET_IS_ENABLED);
        assertThat(marketConfig.getBaseCurrency()).isEqualTo(NEW_MARKET_BASE_CURRENCY);
        assertThat(marketConfig.getCounterCurrency()).isEqualTo(NEW_MARKET_COUNTER_CURRENCY);
        assertThat(marketConfig.getTradingStrategyId()).isEqualTo(NEW_MARKET_TRADING_STRATEGY_ID);

        PowerMock.verifyAll();
    }

    @Test
    public void whenDeleteCalledWithKnownIdThenReturnMatchingMarketConfig() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        ConfigurationManager.saveConfig(
                eq(MarketsType.class),
                anyObject(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME));

        PowerMock.replayAll();

        final MarketConfigRepository marketConfigRepository = new MarketConfigRepositoryXmlDatastore();
        final MarketConfig marketConfig = marketConfigRepository.delete(MARKET_1_ID);

        assertThat(marketConfig.getId()).isEqualTo(MARKET_1_ID);
        assertThat(marketConfig.getName()).isEqualTo(MARKET_1_NAME);
        assertThat(marketConfig.isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
        assertThat(marketConfig.getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
        assertThat(marketConfig.getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
        assertThat(marketConfig.getTradingStrategyId()).isEqualTo(MARKET_1_TRADING_STRATEGY_ID);

        PowerMock.verifyAll();
    }

    @Test
    public void whenDeleteCalledWithUnknownIdThenReturnEmptyMarket() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(MarketsType.class),
                eq(MARKETS_CONFIG_XML_FILENAME),
                eq(MARKETS_CONFIG_XSD_FILENAME))).
                andReturn(allTheInternalMarketsConfig());

        PowerMock.replayAll();

        final MarketConfigRepository marketConfigRepository = new MarketConfigRepositoryXmlDatastore();
        final MarketConfig marketConfig = marketConfigRepository.delete(UNKNOWN_MARKET_ID);

        assertThat(marketConfig.getId()).isNull();
        assertThat(marketConfig.getName()).isNull();
        assertThat(marketConfig.isEnabled()).isFalse();
        assertThat(marketConfig.getBaseCurrency()).isNull();
        assertThat(marketConfig.getCounterCurrency()).isNull();
        assertThat(marketConfig.getTradingStrategyId()).isNull();

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static MarketsType allTheInternalMarketsConfig() {

        final MarketType marketType1 = new MarketType();
        marketType1.setId(MARKET_1_ID);
        marketType1.setName(MARKET_1_NAME);
        marketType1.setEnabled(MARKET_1_IS_ENABLED);
        marketType1.setBaseCurrency(MARKET_1_BASE_CURRENCY);
        marketType1.setCounterCurrency(MARKET_1_COUNTER_CURRENCY);
        marketType1.setTradingStrategyId(MARKET_1_TRADING_STRATEGY_ID);

        final MarketType marketType2 = new MarketType();
        marketType2.setId(MARKET_2_ID);
        marketType2.setName(MARKET_2_NAME);
        marketType2.setEnabled(MARKET_2_IS_ENABLED);
        marketType2.setBaseCurrency(MARKET_2_BASE_CURRENCY);
        marketType2.setCounterCurrency(MARKET_2_COUNTER_CURRENCY);
        marketType2.setTradingStrategyId(MARKET_2_TRADING_STRATEGY_ID);

        final MarketsType marketsType = new MarketsType();
        marketsType.getMarkets().add(marketType1);
        marketsType.getMarkets().add(marketType2);

        return marketsType;
    }

    private static MarketsType allTheInternalMarketsConfigPlusNewOne() {

        final MarketType newMarket = new MarketType();
        newMarket.setId(GENERATED_MARKET_ID);
        newMarket.setName(NEW_MARKET_NAME);
        newMarket.setEnabled(NEW_MARKET_IS_ENABLED);
        newMarket.setBaseCurrency(NEW_MARKET_BASE_CURRENCY);
        newMarket.setCounterCurrency(NEW_MARKET_COUNTER_CURRENCY);
        newMarket.setTradingStrategyId(NEW_MARKET_TRADING_STRATEGY_ID);

        final MarketsType existingMarketsPlusNewOne = allTheInternalMarketsConfig();
        existingMarketsPlusNewOne.getMarkets().add(newMarket);
        return existingMarketsPlusNewOne;
    }

    private static MarketConfig someExternalMarketConfig() {
        return new MarketConfig(MARKET_1_ID, MARKET_1_NAME, MARKET_1_BASE_CURRENCY, MARKET_1_COUNTER_CURRENCY,
                MARKET_1_IS_ENABLED, MARKET_1_TRADING_STRATEGY_ID);
    }

    private static MarketConfig someNewExternalMarketConfig() {
        return new MarketConfig(null, NEW_MARKET_NAME, NEW_MARKET_BASE_CURRENCY, NEW_MARKET_COUNTER_CURRENCY,
                NEW_MARKET_IS_ENABLED, NEW_MARKET_TRADING_STRATEGY_ID);
    }

    private static MarketConfig someExternalMarketConfigWithUnknownId() {
        return new MarketConfig(UNKNOWN_MARKET_ID, MARKET_1_NAME, MARKET_1_BASE_CURRENCY, MARKET_1_COUNTER_CURRENCY,
                MARKET_1_IS_ENABLED, MARKET_1_TRADING_STRATEGY_ID);
    }
}
