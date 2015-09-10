/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

package com.gazbert.bxbot.core.engine;


import com.gazbert.bxbot.core.api.trading.*;
import com.gazbert.bxbot.core.api.strategy.StrategyException;
import com.gazbert.bxbot.core.api.strategy.TradingStrategy;
import com.gazbert.bxbot.core.config.ConfigurableComponentFactory;
import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.engine.generated.EngineType;
import com.gazbert.bxbot.core.config.exchange.generated.ExchangeType;
import com.gazbert.bxbot.core.config.market.generated.MarketType;
import com.gazbert.bxbot.core.config.market.generated.MarketsType;
import com.gazbert.bxbot.core.config.strategy.StrategyConfigImpl;
import com.gazbert.bxbot.core.config.strategy.generated.ConfigItemType;
import com.gazbert.bxbot.core.config.strategy.generated.ConfigurationType;
import com.gazbert.bxbot.core.config.strategy.generated.StrategyType;
import com.gazbert.bxbot.core.config.strategy.generated.TradingStrategiesType;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.core.mail.SmtpConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertTrue;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;


/*
 * Tests the behaviour of the Trading Engine is as expected.
 *
 * The Exchange Adapter and Configuration subsystem are mocked out; they have their own unit tests.
 *
 * TradingEngine.class is prepared so we can mock constructors for Market + StrategyConfigImpl object creation.
 *
 * There's a lot of time dependent stuff going on here; I hate these sorts of tests!
 *
 * TODO Break up the setupForEachTest into more manageable parts.
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationManager.class, ConfigurableComponentFactory.class, Market.class, StrategyConfigImpl.class,
        TradingEngine.class, BalanceInfo.class, SmtpConfig.class, EmailAlerter.class})
public class TestTradingEngine {
    
    // for email alerts
    private static final String CRITICAL_EMAIL_ALERT_SUBJECT = "CRITICAL Alert message from BX-bot";

    // Exchange Adapter config
    private static final String EXCHANGE_ADAPTER_IMPL_CLASS = "com.my.adapters.DummyBtceExchangeAdapter";
    private static final String EXCHANGE_ADAPTER_NAME = "My BTC-e Adapter";

    // Engine config
    private static final String ENGINE_EMERGENCY_STOP_CURRENCY = "BTC";
    private static final BigDecimal ENGINE_EMERGENCY_STOP_BALANCE = new BigDecimal("0.5");
    private static final int ENGINE_TRADE_CYCLE_INTERVAL = 1; // unrealistic, but 1 second speeds up tests ;-)

    // Strategies config
    private static final String STRATEGY_ID = "MyMacdStrategy_v3";
    private static final String STRATEGY_LABEL = "MACD Shorting algo";
    private static final String STRATEGY_IMPL_CLASS = "com.my.strats.MyMacdStrategy";
    private static final String STRATEGY_CONFIG_ITEM_NAME = "btc-sell-order-amount";
    private static final String STRATEGY_CONFIG_ITEM_VALUE = "0.2";

    // Markets config
    private static final String MARKET_LABEL = "BTC/USD";
    private static final String MARKET_ID = "btc_usd";
    private static final String MARKET_BASE_CURRENCY = "BTC";
    private static final String MARKET_COUNTER_CURRENCY = "USD";
    private static final boolean MARKET_IS_ENABLED = true;

    // Mocks used by all tests
    private TradingApi tradingApi;
    private TradingStrategy tradingStrategy;

    // Hack to expose these fields for testing duplicate Markets error condition.
    private MarketType marketType;
    private List<MarketType> marketTypes;


    /*
     * Brutal use of mocks, but need to be sure we get this right!
     *
     * Mock out Config subsystem; we're not testing it here - has its own unit tests.
     *
     * Test scenarios use 1 market with 1 strategy with 1 config item to keep tests manageable.
     * Loading multiple markets/strategies is tested in the Configuration subsystem unit tests.
     */
    @Before
    public void setupForEachTest() throws Exception {
        
        // Mock out the Config subsystem
        PowerMock.mockStatic(ConfigurationManager.class);
        PowerMock.mockStatic(ConfigurableComponentFactory.class);
        
        // expect to load Exchange Adapter config and then create it
        final ExchangeType exchangeType = PowerMock.createMock(ExchangeType.class);
        expect(ConfigurationManager.loadConfig(eq(ExchangeType.class), anyString(), anyString())).andReturn(exchangeType);
        expect(exchangeType.getAdapter()).andReturn(EXCHANGE_ADAPTER_IMPL_CLASS);
        tradingApi = PowerMock.createMock(TradingApi.class);
        expect(ConfigurableComponentFactory.createComponent(EXCHANGE_ADAPTER_IMPL_CLASS)).andReturn(tradingApi);
        expect(tradingApi.getImplName()).andReturn(EXCHANGE_ADAPTER_NAME);

        // expect to load Engine config
        final EngineType engineType = PowerMock.createMock(EngineType.class);
        expect(ConfigurationManager.loadConfig(eq(EngineType.class), anyString(), anyString())).andReturn(engineType);
        expect(engineType.getEmergencyStopCurrency()).andReturn(ENGINE_EMERGENCY_STOP_CURRENCY);
        expect(engineType.getEmergencyStopBalance()).andReturn(ENGINE_EMERGENCY_STOP_BALANCE);
        expect(engineType.getTradeCycleInterval()).andReturn(ENGINE_TRADE_CYCLE_INTERVAL);
        
        // expect to load Strategies config
        final TradingStrategiesType tradingStrategiesType = PowerMock.createMock(TradingStrategiesType.class);
        final StrategyType strategyType = PowerMock.createMock(StrategyType.class);
        final List<StrategyType> strategyTypes = new ArrayList<>();
        strategyTypes.add(strategyType); // just the 1 strat being loaded here
        expect(ConfigurationManager.loadConfig(eq(TradingStrategiesType.class), anyString(), anyString())).andReturn(tradingStrategiesType);
        expect(tradingStrategiesType.getStrategies()).andReturn(strategyTypes);
        expect(strategyType.getId()).andReturn(STRATEGY_ID).anyTimes();

        // expect to load Markets config and bind chosen Strategy to Market.
        final MarketsType marketsType = PowerMock.createMock(MarketsType.class);
        marketType = PowerMock.createMock(MarketType.class);
        marketTypes = new ArrayList<>();
        marketTypes.add(marketType); // just the 1 market being loaded here
        expect(ConfigurationManager.loadConfig(eq(MarketsType.class), anyString(), anyString())).andReturn(marketsType);
        expect(marketsType.getMarkets()).andReturn(marketTypes);
        expect(marketType.getLabel()).andReturn(MARKET_LABEL);
        expect(marketType.getId()).andReturn(MARKET_ID);
        expect(marketType.getBaseCurrency()).andReturn(MARKET_BASE_CURRENCY);
        expect(marketType.getCounterCurrency()).andReturn(MARKET_COUNTER_CURRENCY);
        expect(marketType.isEnabled()).andReturn(MARKET_IS_ENABLED);

        // expect the Exchange API StrategyConfig object to be created
        final Market market = PowerMock.createMock(Market.class);
        PowerMock.expectNew(Market.class, MARKET_LABEL, MARKET_ID, MARKET_BASE_CURRENCY, MARKET_COUNTER_CURRENCY).andStubReturn(market);

        // expect Strategy to be selected for the Market
        expect(marketType.getTradingStrategy()).andReturn(STRATEGY_ID); // matches our strat loaded earlier
        expect(strategyType.getClassName()).andReturn(STRATEGY_IMPL_CLASS).atLeastOnce(); // might be called +1 if logging

        // expect the Trading API Market domain object to be created
        final StrategyConfigImpl strategyConfig = PowerMock.createMock(StrategyConfigImpl.class);
        PowerMock.expectNew(StrategyConfigImpl.class).andStubReturn(strategyConfig);

        // expect to load up Strategy config
        final ConfigurationType configurationType = PowerMock.createMock(ConfigurationType.class);
        expect(strategyType.getConfiguration()).andReturn(configurationType);
        final ConfigItemType configItemType = PowerMock.createMock(ConfigItemType.class);
        final List<ConfigItemType> configItemTypes = new ArrayList<>();
        configItemTypes.add(configItemType); // just 1 config item being loaded here
        expect(configurationType.getConfigItem()).andReturn(configItemTypes);
        expect(configItemType.getName()).andReturn(STRATEGY_CONFIG_ITEM_NAME);
        expect(configItemType.getValue()).andReturn(STRATEGY_CONFIG_ITEM_VALUE);
        strategyConfig.addConfigItem(STRATEGY_CONFIG_ITEM_NAME, STRATEGY_CONFIG_ITEM_VALUE); // add to Exchange API object

        // finally(!) expect Strategy to be created and initialised
        tradingStrategy = PowerMock.createMock(TradingStrategy.class);
        expect(ConfigurableComponentFactory.createComponent(STRATEGY_IMPL_CLASS)).andReturn(tradingStrategy);

        PowerMock.replay(market);
        PowerMock.replay(strategyConfig);
        tradingStrategy.init(tradingApi, market, strategyConfig);
        expect(strategyType.getLabel()).andReturn(STRATEGY_LABEL).anyTimes(); // might be called if logging
    }

    /*
     * Tests Trading Engine is initialised as expected.
     */
    @Test
    public void testEngineInitialisesSuccessfully() throws Exception {

        // Expect Email Alerter to be initialised
        PowerMock.mockStatic(EmailAlerter.class);
        final EmailAlerter emailAlerter = PowerMock.createMock(EmailAlerter.class);
        expect(EmailAlerter.getInstance()).andReturn(emailAlerter);

        // activate mocks
        PowerMock.replayAll();

        // run test
        final TradingEngine tradingEngine = TradingEngine.newInstance();

        // it should not be started yet though
        assertFalse(tradingEngine.isRunning());

        // mocks all called as expected?
        PowerMock.verifyAll();
    }

    /*
     * Tests Trading Engine fails initialisation if a duplicate Market is loaded from config.
     */
    @Test (expected = IllegalArgumentException.class)
    public void testEngineInitialisesFailsIfDuplicateMarketFoundInConfig() throws Exception {

        // add a duplicate market - this is nasty, but no time to refactor...
        marketTypes.add(marketType);
        expect(marketType.getLabel()).andReturn(MARKET_LABEL);
        expect(marketType.getId()).andReturn(MARKET_ID);
        expect(marketType.getBaseCurrency()).andReturn(MARKET_BASE_CURRENCY);
        expect(marketType.getCounterCurrency()).andReturn(MARKET_COUNTER_CURRENCY);
        expect(marketType.isEnabled()).andReturn(MARKET_IS_ENABLED);

        // activate mocks
        PowerMock.replayAll();

        // run test
        TradingEngine.newInstance();

        // mocks all called as expected?
        PowerMock.verifyAll();
    }

    /*
     * Tests the engine is shutdown if Emergency Stop Currency wallet balance on the exchange drops below
     * configured limit.
     */
    @Test
    public void testEngineShutsDownWhenEmergencyStopBalanceIfBreached() throws Exception {

        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        // balance limit has been breached for BTC
        balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.49999999"));

        // Expect Email Alerter to be initialised
        PowerMock.mockStatic(EmailAlerter.class);
        final EmailAlerter emailAlerter = PowerMock.createMock(EmailAlerter.class);
        expect(EmailAlerter.getInstance()).andReturn(emailAlerter);

        // expect BalanceInfo to be fetched using Trading API
        final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);

        // expect Email Alert to be sent
        emailAlerter.sendMessage(eq(CRITICAL_EMAIL_ALERT_SUBJECT),
                contains("EMERGENCY STOP triggered! - Current Emergency Stop Currency [BTC] wallet balance [0.49999999]" +
                        " on exchange is lower than configured Emergency Stop balance [0.5] BTC"));

        // activate mocks
        PowerMock.replayAll();

        // run test
        final TradingEngine tradingEngine = TradingEngine.newInstance();
        tradingEngine.start();

        // sleep for 1s and check if shutdown ok
        Thread.sleep(1 * 1000);
        assertFalse(tradingEngine.isRunning());

        // mocks all called as expected?
        PowerMock.verifyAll();
    }

    /*
     * Tests the engine starts up and executes trade cycles successfully.
     * Scenario is 2 successful trade cycles and then we shut it down.
     */
    @Test
    public void testEngineExecutesTradeCyclesAndCanBeShutdownSuccessfully() throws Exception {

        final int numberOfTradeCycles = 2;
        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        // balance limit NOT breached for BTC
        balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));

        // Expect Email Alerter to be initialised
        PowerMock.mockStatic(EmailAlerter.class);
        final EmailAlerter emailAlerter = PowerMock.createMock(EmailAlerter.class);
        expect(EmailAlerter.getInstance()).andReturn(emailAlerter);

        // expect BalanceInfo to be fetched using Trading API
        final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo).times(numberOfTradeCycles);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable).times(numberOfTradeCycles);

        // expect Trading Strategy to be invoked 2 times, once every 1s
        tradingStrategy.execute();
        expectLastCall().times(numberOfTradeCycles);

        // activate mocks
        PowerMock.replayAll();

        // run test
        final TradingEngine tradingEngine = TradingEngine.newInstance();
        final Executor executor = Executors.newSingleThreadExecutor();

//        // java 7 way
//        executor.execute(new Runnable() {
//            @Override
//            public void run() {
//                tradingEngine.start();
//            }
//        });

//        // java 8 lambda
//        executor.execute(() -> tradingEngine.start());

        // java 8 reference method
        executor.execute(tradingEngine::start);

        // sleep for 2s to let 2 trade cycles occur
        Thread.sleep(numberOfTradeCycles * 1000);
        assertTrue(tradingEngine.isRunning());

        // shut it down
        tradingEngine.shutdown();

        // sleep for 1s and check if shutdown ok
        Thread.sleep(1 * 1000);
        assertFalse(tradingEngine.isRunning());

        // mocks all called as expected?
        PowerMock.verifyAll();
    }

    /*
     * Tests the engine starts up, executes 1 trade cycle successfully, but then receives StrategyException from
     * Trading Strategy on the 2nd cycle. We expect the engine to shutdown.
     */
    @Test
    public void testEngineShutsDownWhenItReceivesStrategyExceptionFromTradingStrategy() throws Exception {

        final String exceptionErrorMsg = "Eeek! My strat just broke. Please shutdown!";
        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        // balance limit NOT breached for BTC
        balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
        final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

        // Expect Email Alerter to be initialised
        PowerMock.mockStatic(EmailAlerter.class);
        final EmailAlerter emailAlerter = PowerMock.createMock(EmailAlerter.class);
        expect(EmailAlerter.getInstance()).andReturn(emailAlerter);

        // expect 1st trade cycle to be successful
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
        tradingStrategy.execute();

        // expect StrategyException in 2nd trade cycle
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
        tradingStrategy.execute();
        expectLastCall().andThrow(new StrategyException(exceptionErrorMsg));

        // expect Email Alert to be sent
        emailAlerter.sendMessage(eq(CRITICAL_EMAIL_ALERT_SUBJECT), contains("A FATAL error has occurred in Trading" +
                " Strategy! Details: " + exceptionErrorMsg));

        // activate mocks
        PowerMock.replayAll();

        // run test
        final TradingEngine tradingEngine = TradingEngine.newInstance();
        tradingEngine.start();

        // sleep for 1s and check if shutdown ok
        Thread.sleep(1 * 1000);
        assertFalse(tradingEngine.isRunning());

        // mocks all called as expected?
        PowerMock.verifyAll();
    }

    /*
     * Tests the engine starts up, executes 1 trade cycle successfully, but then receives unexpected Exception from
     * Trading Strategy on the 2nd cycle. We expect the engine to shutdown.
     */
    @Test
    public void testEngineShutsDownWhenItReceivesUnexpectedExceptionFromTradingStrategy() throws Exception {

        final String exceptionErrorMsg = "Ah, curse your sudden but inevitable betrayal!";
        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        // balance limit NOT breached for BTC
        balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
        final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

        // Expect Email Alerter to be initialised
        PowerMock.mockStatic(EmailAlerter.class);
        final EmailAlerter emailAlerter = PowerMock.createMock(EmailAlerter.class);
        expect(EmailAlerter.getInstance()).andReturn(emailAlerter);

        // expect 1st trade cycle to be successful
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
        tradingStrategy.execute();

        // expect unexpected Exception in 2nd trade cycle
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
        tradingStrategy.execute();
        expectLastCall().andThrow(new IllegalArgumentException(exceptionErrorMsg));

        // expect Email Alert to be sent
        emailAlerter.sendMessage(eq(CRITICAL_EMAIL_ALERT_SUBJECT), contains("An unexpected FATAL error has occurred in" +
                " Exchange Adapter or Trading Strategy! Details: " + exceptionErrorMsg));

        // activate mocks
        PowerMock.replayAll();

        // run test
        final TradingEngine tradingEngine = TradingEngine.newInstance();
        tradingEngine.start();

        // sleep for 1s and check if shutdown ok
        Thread.sleep(1 * 1000);
        assertFalse(tradingEngine.isRunning());

        // mocks all called as expected?
        PowerMock.verifyAll();
    }

    /*
     * Tests the engine starts up, executes 1 trade cycle successfully, but then receives unexpected Exception from
     * Exchange Adapter on the 2nd cycle. We expect the engine to shutdown.
     */
    @Test
    public void testEngineShutsDownWhenItReceivesUnexpectedExceptionFromExchangeAdapter() throws Exception {

        final String exceptionErrorMsg = "I had to rewire the grav thrust because somebody won't replace that crappy compression coil.";
        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        // balance limit NOT breached for BTC
        balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
        final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

        // Expect Email Alerter to be initialised
        PowerMock.mockStatic(EmailAlerter.class);
        final EmailAlerter emailAlerter = PowerMock.createMock(EmailAlerter.class);
        expect(EmailAlerter.getInstance()).andReturn(emailAlerter);

        // expect 1st trade cycle to be successful
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
        tradingStrategy.execute();

        // expect unexpected Exception in 2nd trade cycle
        expect(tradingApi.getBalanceInfo()).andThrow(new IllegalStateException(exceptionErrorMsg));

        // expect Email Alert to be sent
        emailAlerter.sendMessage(eq(CRITICAL_EMAIL_ALERT_SUBJECT), contains("An unexpected FATAL error has occurred in" +
                " Exchange Adapter or Trading Strategy! Details: " + exceptionErrorMsg));

        // activate mocks
        PowerMock.replayAll();

        // run test
        final TradingEngine tradingEngine = TradingEngine.newInstance();
        tradingEngine.start();

        // sleep for 1s and check if shutdown ok
        Thread.sleep(1 * 1000);
        assertFalse(tradingEngine.isRunning());

        // mocks all called as expected?
        PowerMock.verifyAll();
    }

    /*
     * Tests the engine starts up, executes 1 trade cycle successfully, but then receives TradingApiException from
     * Exchange Adapter on the 2nd cycle. We expect the engine to shutdown.
     */
    @Test
    public void testEngineShutsDownWhenItReceivesTradingApiExceptionFromExchangeAdapter() throws Exception {

        final String exceptionErrorMsg = "Ten percent of nothin' is ... let me do the math here ... nothin' into nothin' ... carry the nothin' ...";
        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        // balance limit NOT breached for BTC
        balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
        final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

        // Expect Email Alerter to be initialised
        PowerMock.mockStatic(EmailAlerter.class);
        final EmailAlerter emailAlerter = PowerMock.createMock(EmailAlerter.class);
        expect(EmailAlerter.getInstance()).andReturn(emailAlerter);

        // expect 1st trade cycle to be successful
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
        tradingStrategy.execute();

        // expect TradingApiException in 2nd trade cycle
        expect(tradingApi.getBalanceInfo()).andThrow(new TradingApiException(exceptionErrorMsg));

        // expect Email Alert to be sent
        emailAlerter.sendMessage(eq(CRITICAL_EMAIL_ALERT_SUBJECT), contains("A FATAL error has occurred in Exchange" +
                " Adapter! Details: " + exceptionErrorMsg));

        // activate mocks
        PowerMock.replayAll();

        // run test
        final TradingEngine tradingEngine = TradingEngine.newInstance();
        tradingEngine.start();

        // sleep for 1s and check if shutdown ok
        Thread.sleep(1 * 1000);
        assertFalse(tradingEngine.isRunning());

        // mocks all called as expected?
        PowerMock.verifyAll();
    }

    /*
     * Tests the engine continues to execute next trade cycle if it receives a ExchangeTimeoutException.
     * Scenario is 1 successful trade cycle, 2nd cycle Exchange Adapter throws ExchangeTimeoutException, engine stays alive and
     * successfully executes 3rd trade cycle.
     */
    @Test
    public void testEngineExecutesNextTradeCyclesAfterReceivingExchangeTimeoutException() throws Exception {

        final String exceptionErrorMsg = "Man walks down the street in a hat like that, you know he's not afraid of anything...";
        final int numberOfTradeCycles = 3;
        final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        // balance limit NOT breached for BTC
        balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));

        // Expect Email Alerter to be initialised
        PowerMock.mockStatic(EmailAlerter.class);
        final EmailAlerter emailAlerter = PowerMock.createMock(EmailAlerter.class);
        expect(EmailAlerter.getInstance()).andReturn(emailAlerter);

        // expect 1st trade cycle to be successful
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
        tradingStrategy.execute();

        // expect BalanceInfo fetch to fail with ExchangeTimeoutException on 2nd cycle
        expect(tradingApi.getBalanceInfo()).andThrow(new ExchangeTimeoutException(exceptionErrorMsg));

        // expect 3rd trade cycle to be successful
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
        tradingStrategy.execute();

        // activate mocks
        PowerMock.replayAll();

        // run test
        final TradingEngine tradingEngine = TradingEngine.newInstance();
        final Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(tradingEngine::start);

        // sleep for 3s to let 3 trade cycles occur
        Thread.sleep(numberOfTradeCycles * 1000);
        assertTrue(tradingEngine.isRunning());

        // shut it down
        tradingEngine.shutdown();

        // sleep for 1s and check if shutdown ok
        Thread.sleep(1 * 1000);
        assertFalse(tradingEngine.isRunning());

        // mocks all called as expected?
        PowerMock.verifyAll();
    }

    /*
     * Tests the engine cannot be started more than once.
     */
    @Test (expected = IllegalStateException.class)
    public void testEngineCannotBeStartedMoreThanOnce() throws Exception {

        final int numberOfTradeCycles = 1;
        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        // balance limit NOT breached for BTC
        balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));

        // Expect Email Alerter to be initialised
        PowerMock.mockStatic(EmailAlerter.class);
        final EmailAlerter emailAlerter = PowerMock.createMock(EmailAlerter.class);
        expect(EmailAlerter.getInstance()).andReturn(emailAlerter);

        // expect BalanceInfo to be fetched using Trading API
        final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
        expect(tradingApi.getBalanceInfo()).andReturn(balanceInfo).times(numberOfTradeCycles);
        expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable).times(numberOfTradeCycles);

        // expect Trading Strategy to be invoked 1 time
        tradingStrategy.execute();
        expectLastCall().times(numberOfTradeCycles);

        // activate mocks
        PowerMock.replayAll();

        // run test
        final TradingEngine tradingEngine = TradingEngine.newInstance();
        final Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(tradingEngine::start);

        // sleep for 1s to let 1 trade cycles occur
        Thread.sleep(numberOfTradeCycles * 1000);
        assertTrue(tradingEngine.isRunning());

        // try start the engine again
        tradingEngine.start();

        // mocks all called as expected?
        PowerMock.verifyAll();
    }
}
