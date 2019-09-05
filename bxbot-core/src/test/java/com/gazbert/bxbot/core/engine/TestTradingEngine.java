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

import static junit.framework.TestCase.assertTrue;
import static org.awaitility.Awaitility.await;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.contains;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertFalse;

import com.gazbert.bxbot.core.config.strategy.TradingStrategiesBuilder;
import com.gazbert.bxbot.core.config.strategy.TradingStrategyFactory;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.core.util.ConfigurableComponentFactory;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.services.config.EngineConfigService;
import com.gazbert.bxbot.services.config.ExchangeConfigService;
import com.gazbert.bxbot.services.config.MarketConfigService;
import com.gazbert.bxbot.services.config.StrategyConfigService;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests the behaviour of the Trading Engine is as expected.
 *
 * <p>The Exchange Adapter and Configuration subsystem are mocked out; they have their own unit
 * tests.
 *
 * <p>TradingEngine.class is prepared so we can mock constructors for Market + StrategyConfigItems
 * object creation.
 *
 * <p>There's a lot of time dependent stuff going on here; I hate these sorts of tests!
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurableComponentFactory.class, Market.class, BalanceInfo.class})
@PowerMockIgnore({
    "javax.crypto.*",
    "javax.management.*",
    "com.sun.org.apache.xerces.*",
    "javax.xml.parsers.*",
    "org.xml.sax.*",
    "org.w3c.dom.*"
})
public class TestTradingEngine {

  private enum EngineState {
    RUNNING,
    SHUTDOWN
  }

  private static final String CRITICAL_EMAIL_ALERT_SUBJECT = "CRITICAL Alert message from BX-bot";

  private static final String EXCHANGE_ADAPTER_IMPL_CLASS =
      "com.my.adapters.DummyBitstampExchangeAdapter";
  private static final String EXCHANGE_NAME = "Bitstamp";
  private static final Integer EXCHANGE_ADAPTER_NETWORK_TIMEOUT = Integer.valueOf("30");
  private static final List<Integer> EXCHANGE_ADAPTER_NONFATAL_ERROR_CODES =
      Arrays.asList(502, 503, 504);
  private static final List<String> EXCHANGE_ADAPTER_NONFATAL_ERROR_MESSAGES =
      Arrays.asList(
          "Connection reset",
          "Connection refused",
          "Remote host closed connection during handshake");
  private static final String EXCHANGE_ADAPTER_AUTHENTICATION_CONFIG_ITEM_NAME = "key";
  private static final String EXCHANGE_ADAPTER_AUTHENTICATION_CONFIG_ITEM_VALUE = "myKey123";
  private static final String EXCHANGE_ADAPTER_OTHER_CONFIG_ITEM_NAME = "sell-fee";
  private static final String EXCHANGE_ADAPTER_OTHER_CONFIG_ITEM_VALUE = "0.25";

  private static final String ENGINE_EMERGENCY_STOP_CURRENCY = "BTC";
  private static final BigDecimal ENGINE_EMERGENCY_STOP_BALANCE = new BigDecimal("0.5");
  private static final int ENGINE_TRADE_CYCLE_INTERVAL =
      1; // unrealistic, but 1 second speeds up tests ;-)

  private static final String STRATEGY_ID = "MyMacdStrategy_v3";
  private static final String STRATEGY_NAME = "MACD Shorting algo";
  private static final String STRATEGY_DESCRIPTION = "MACD Shorting algo description";
  private static final String STRATEGY_IMPL_CLASS = "com.my.strats.MyMacdStrategy";
  private static final String STRATEGY_IMPL_BEAN = null;
  private static final String STRATEGY_CONFIG_ITEM_NAME = "btc-sell-order-amount";
  private static final String STRATEGY_CONFIG_ITEM_VALUE = "0.2";

  private static final String MARKET_NAME = "BTC/USD";
  private static final String MARKET_ID = "btc_usd";
  private static final String MARKET_BASE_CURRENCY = "BTC";
  private static final String MARKET_COUNTER_CURRENCY = "USD";
  private static final boolean MARKET_IS_ENABLED = true;

  // Mocks used by all tests
  private ExchangeAdapter exchangeAdapter;
  private TradingStrategy tradingStrategy;
  private EmailAlerter emailAlerter;
  private ExchangeConfigService exchangeConfigService;
  private EngineConfigService engineConfigService;
  private StrategyConfigService strategyConfigService;
  private MarketConfigService marketConfigService;

  private TradingStrategiesBuilder tradingStrategiesBuilder;

  /**
   * Mock out Config subsystem; we're not testing it here - has its own unit tests.
   *
   * <p>Test scenarios use 1 market with 1 strategy with 1 config item to keep tests manageable.
   * Loading multiple markets/strategies is tested in the Configuration subsystem unit tests.
   */
  @Before
  public void setupForEachTest() {
    exchangeAdapter = PowerMock.createMock(ExchangeAdapter.class);
    tradingStrategy = PowerMock.createMock(TradingStrategy.class);
    emailAlerter = PowerMock.createMock(EmailAlerter.class);

    exchangeConfigService = PowerMock.createMock(ExchangeConfigService.class);
    engineConfigService = PowerMock.createMock(EngineConfigService.class);
    strategyConfigService = PowerMock.createMock(StrategyConfigService.class);
    marketConfigService = PowerMock.createMock(MarketConfigService.class);

    final TradingStrategyFactory tradingStrategyFactory = new TradingStrategyFactory();
    tradingStrategiesBuilder = new TradingStrategiesBuilder();
    tradingStrategiesBuilder.setTradingStrategyFactory(tradingStrategyFactory);

    PowerMock.mockStatic(ConfigurableComponentFactory.class);
  }

  @Test
  public void testEngineInitialisesSuccessfully() {
    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  @Test
  public void testEngineShutsDownWhenEmergencyStopBalanceIfBreached() throws Exception {
    setupConfigLoadingExpectations();

    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    final BigDecimal btcBalance = new BigDecimal("0.49999999");
    // balance limit has been breached for BTC
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, btcBalance);

    // expect BalanceInfo to be fetched using Trading API
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);

    // expect Email Alert to be sent
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains(
                "EMERGENCY STOP triggered! - Current Emergency Stop Currency [BTC] wallet balance ["
                    + new DecimalFormat("#.########").format(btcBalance))
            + "] on exchange is lower than configured Emergency Stop balance ["
            + new DecimalFormat("#.########").format(ENGINE_EMERGENCY_STOP_BALANCE)
            + "] BTC");

    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);
    tradingEngine.start();

    await().until(engineStateChanged(tradingEngine, EngineState.SHUTDOWN));
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  @Test
  public void testEngineDoesNotPerformEmergencyStopCheckWhenEmergencyStopBalanceIsZero()
      throws Exception {
    setupConfigLoadingExpectationsForNoEmergencyStopCheck();

    // expect Trading Strategy to be invoked at least once
    tradingStrategy.execute();
    expectLastCall().atLeastOnce();

    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);

    final Executor executor = Executors.newSingleThreadExecutor();
    executor.execute(tradingEngine::start);

    await().until(engineStateChanged(tradingEngine, EngineState.RUNNING));
    assertTrue(tradingEngine.isRunning());

    tradingEngine.shutdown();

    await().until(engineStateChanged(tradingEngine, EngineState.SHUTDOWN));
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  /*
   * Tests the engine starts up and executes trade cycles successfully.
   * Scenario is at least one successful trade cycle and then we shut it down.
   */
  @Test
  public void testEngineExecutesTradeCyclesAndCanBeShutdownSuccessfully() throws Exception {
    setupConfigLoadingExpectations();

    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));

    // expect BalanceInfo to be fetched using Trading API
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo).atLeastOnce();
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable).atLeastOnce();

    // expect Trading Strategy to be invoked
    tradingStrategy.execute();
    expectLastCall().atLeastOnce();

    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);

    final Executor executor = Executors.newSingleThreadExecutor();
    executor.execute(tradingEngine::start);

    await().until(engineStateChanged(tradingEngine, EngineState.RUNNING));
    assertTrue(tradingEngine.isRunning());

    tradingEngine.shutdown();

    await().until(engineStateChanged(tradingEngine, EngineState.SHUTDOWN));
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  /*
   * Tests the engine starts up, executes 1 trade cycle successfully, but then receives
   * StrategyException from Trading Strategy - we expect the engine to shutdown.
   */
  @Test
  public void testEngineShutsDownWhenItReceivesStrategyExceptionFromTradingStrategy()
      throws Exception {
    setupConfigLoadingExpectations();

    final String exceptionErrorMsg = "Eeek! My strat just broke. Please shutdown!";
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

    // expect 1st trade cycle to be successful
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();

    // expect StrategyException in 2nd trade cycle
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();
    expectLastCall().andThrow(new StrategyException(exceptionErrorMsg));

    // expect Email Alert to be sent
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains("A FATAL error has occurred in Trading Strategy! Details: " + exceptionErrorMsg));
    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);

    tradingEngine.start();

    await().until(engineStateChanged(tradingEngine, EngineState.SHUTDOWN));
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  /*
   * Tests the engine starts up, executes 1 trade cycle successfully, but then receives unexpected
   * Exception from Trading Strategy - we expect the engine to shutdown.
   */
  @Test
  public void testEngineShutsDownWhenItReceivesUnexpectedExceptionFromTradingStrategy()
      throws Exception {
    setupConfigLoadingExpectations();

    final String exceptionErrorMsg = "Ah, curse your sudden but inevitable betrayal!";
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

    // expect 1st trade cycle to be successful
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();

    // expect unexpected Exception in 2nd trade cycle
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();
    expectLastCall().andThrow(new IllegalArgumentException(exceptionErrorMsg));

    // expect Email Alert to be sent
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains(
            "An unexpected FATAL error has occurred in Exchange Adapter or Trading Strategy! "
                + "Details: "
                + exceptionErrorMsg));

    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);

    tradingEngine.start();

    await().until(engineStateChanged(tradingEngine, EngineState.SHUTDOWN));
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  /*
   * Tests the engine starts up, executes 1 trade cycle successfully, but then receives unexpected
   * Exception from Exchange Adapter on the 2nd cycle. We expect the engine to shutdown.
   */
  @Test
  public void testEngineShutsDownWhenItReceivesUnexpectedExceptionFromExchangeAdapter()
      throws Exception {

    setupConfigLoadingExpectations();

    final String exceptionErrorMsg =
        "I had to rewire the grav thrust because somebody won't replace that crappy "
            + "compression coil.";
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

    // expect 1st trade cycle to be successful
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();

    // expect unexpected Exception in 2nd trade cycle
    expect(exchangeAdapter.getBalanceInfo()).andThrow(new IllegalStateException(exceptionErrorMsg));

    // expect Email Alert to be sent
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains(
            "An unexpected FATAL error has occurred in Exchange Adapter or Trading Strategy!"
                + " Details: "
                + exceptionErrorMsg));

    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);

    tradingEngine.start();

    await().until(engineStateChanged(tradingEngine, EngineState.SHUTDOWN));
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  /*
   * Tests the engine starts up, executes 1 trade cycle successfully, but then receives
   * TradingApiException from Exchange Adapter on the 2nd cycle. We expect the engine to shutdown.
   */
  @Test
  public void testEngineShutsDownWhenItReceivesTradingApiExceptionFromExchangeAdapter()
      throws Exception {
    setupConfigLoadingExpectations();

    final String exceptionErrorMsg =
        "Ten percent of nothin' is ... let me do the math here ... nothin' into nothin'"
            + " ... carry the nothin' ...";
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

    // expect 1st trade cycle to be successful
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();

    // expect TradingApiException in 2nd trade cycle
    expect(exchangeAdapter.getBalanceInfo()).andThrow(new TradingApiException(exceptionErrorMsg));

    // expect Email Alert to be sent
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains(
            "A FATAL error has occurred in Exchange" + " Adapter! Details: " + exceptionErrorMsg));

    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);

    tradingEngine.start();

    await().until(engineStateChanged(tradingEngine, EngineState.SHUTDOWN));
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  /*
   * Tests the engine continues to execute next trade cycle if it receives a
   * ExchangeNetworkException. Scenario is 1 successful trade cycle, 2nd cycle Exchange Adapter
   * throws ExchangeNetworkException, engine stays alive and Emergency Stop Check MUST run at start
   * of every trade cycle.successfully executes subsequent trade cycles.
   */
  @Test
  public void testEngineExecutesNextTradeCyclesAfterReceivingExchangeNetworkException()
      throws Exception {
    setupConfigLoadingExpectations();

    final String exceptionErrorMsg =
        "Man walks down the street in a hat like that, you know he's not afraid of anything...";

    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

    // expect 1st trade cycle to be successful
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();

    // expect recoverable ExchangeNetworkException in 2nd trade cycle
    expect(exchangeAdapter.getBalanceInfo())
        .andThrow(new ExchangeNetworkException(exceptionErrorMsg));

    // expect bot recover and continue 3rd cycle + any subsequent ones...
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo).atLeastOnce();
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable).atLeastOnce();
    tradingStrategy.execute();
    expectLastCall().atLeastOnce();

    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);

    final Executor executor = Executors.newSingleThreadExecutor();
    executor.execute(tradingEngine::start);

    await().until(engineStateChanged(tradingEngine, EngineState.RUNNING));
    assertTrue(tradingEngine.isRunning());

    // wait for a few trade cycles, then shutdown the bot.
    try {
      Thread.sleep(3 * (ENGINE_TRADE_CYCLE_INTERVAL * 1000));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    tradingEngine.shutdown();

    await().until(engineStateChanged(tradingEngine, EngineState.SHUTDOWN));
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  /*
   * Tests the engine cannot be started more than once.
   */
  @Test(expected = IllegalStateException.class)
  public void testEngineCannotBeStartedMoreThanOnce() throws Exception {
    setupConfigLoadingExpectations();

    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));

    // expect BalanceInfo to be fetched using Trading API
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo).atLeastOnce();
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable).atLeastOnce();

    // expect Trading Strategy to be invoked 1 time
    tradingStrategy.execute();
    expectLastCall().atLeastOnce();

    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);
    final Executor executor = Executors.newSingleThreadExecutor();
    executor.execute(tradingEngine::start);

    await().until(engineStateChanged(tradingEngine, EngineState.RUNNING));
    assertTrue(tradingEngine.isRunning());

    // try start the engine again
    tradingEngine.start();

    PowerMock.verifyAll();
  }

  @Test
  public void testEngineShutsDownWhenBalancesCannotBeFetchedFromExchange() throws Exception {
    setupConfigLoadingExpectations();

    // empty balances from exchange
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();

    // expect BalanceInfo to be fetched using Trading API
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);

    // expect Email Alert to be sent
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains(
            "Emergency stop check: Failed to get current Emergency Stop Currency balance as '"
                + ENGINE_EMERGENCY_STOP_CURRENCY
                + "' key into Balances map "
                + "returned null. Balances returned: "
                + balancesAvailable));
    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);
    tradingEngine.start();

    await().until(engineStateChanged(tradingEngine, EngineState.SHUTDOWN));
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  @Test
  public void testEngineInitialisesSuccessfullyWithoutNetworkConfig() throws Exception {
    setupExchangeAdapterConfigWithNoNetworkConfigExpectations();
    setupEngineConfigExpectations();
    setupStrategyAndMarketConfigExpectations();

    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));

    // expect BalanceInfo to be fetched using Trading API
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo).atLeastOnce();
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable).atLeastOnce();

    // expect Trading Strategy to be invoked
    tradingStrategy.execute();
    expectLastCall().atLeastOnce();

    PowerMock.replayAll();

    final TradingEngine tradingEngine =
        new TradingEngine(
            exchangeConfigService,
            engineConfigService,
            strategyConfigService,
            marketConfigService,
            emailAlerter,
            tradingStrategiesBuilder);

    final Executor executor = Executors.newSingleThreadExecutor();
    executor.execute(tradingEngine::start);

    await().until(engineStateChanged(tradingEngine, EngineState.RUNNING));
    assertTrue(tradingEngine.isRunning());

    tradingEngine.shutdown();

    await().until(engineStateChanged(tradingEngine, EngineState.SHUTDOWN));
    assertFalse(tradingEngine.isRunning());

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  private utils
  // --------------------------------------------------------------------------

  private void setupExchangeAdapterConfigExpectations() {
    expect(exchangeConfigService.getExchangeConfig()).andReturn(someExchangeConfig());
    expect(ConfigurableComponentFactory.createComponent(EXCHANGE_ADAPTER_IMPL_CLASS))
        .andReturn(exchangeAdapter);
    expect(exchangeAdapter.getImplName()).andReturn(EXCHANGE_NAME).anyTimes();
    exchangeAdapter.init(anyObject(ExchangeConfig.class));
  }

  private void setupExchangeAdapterConfigWithNoNetworkConfigExpectations() {
    final com.gazbert.bxbot.domain.exchange.ExchangeConfig exchangeConfig =
        someExchangeConfigWithoutNetworkConfig();
    expect(exchangeConfigService.getExchangeConfig()).andReturn(exchangeConfig);
    expect(ConfigurableComponentFactory.createComponent(EXCHANGE_ADAPTER_IMPL_CLASS))
        .andReturn(exchangeAdapter);
    expect(exchangeAdapter.getImplName()).andReturn(EXCHANGE_NAME).anyTimes();
    exchangeAdapter.init(anyObject(ExchangeConfig.class));
  }

  private void setupEngineConfigExpectations() {
    expect(engineConfigService.getEngineConfig()).andReturn(someEngineConfig());
  }

  private void setupEngineConfigForNoEmergencyStopCheckExpectations() {
    expect(engineConfigService.getEngineConfig())
        .andReturn(someEngineConfigForNoEmergencyStopCheck());
  }

  private void setupStrategyAndMarketConfigExpectations() {
    expect(strategyConfigService.getAllStrategyConfig()).andReturn(allTheStrategiesConfig());
    expect(marketConfigService.getAllMarketConfig()).andReturn(allTheMarketsConfig());
    expect(ConfigurableComponentFactory.createComponent(STRATEGY_IMPL_CLASS))
        .andReturn(tradingStrategy);
    tradingStrategy.init(
        eq(exchangeAdapter),
        anyObject(Market.class),
        anyObject(com.gazbert.bxbot.strategy.api.StrategyConfig.class));
  }

  private void setupConfigLoadingExpectations() {
    setupExchangeAdapterConfigExpectations();
    setupEngineConfigExpectations();
    setupStrategyAndMarketConfigExpectations();
  }

  private void setupConfigLoadingExpectationsForNoEmergencyStopCheck() {
    setupExchangeAdapterConfigExpectations();
    setupEngineConfigForNoEmergencyStopCheckExpectations();
    setupStrategyAndMarketConfigExpectations();
  }

  private static com.gazbert.bxbot.domain.exchange.ExchangeConfig someExchangeConfig() {
    final Map<String, String> authenticationConfig = someAuthenticationConfig();
    final NetworkConfig networkConfig = someNetworkConfig();
    final Map<String, String> otherConfig = someOtherConfig();

    final com.gazbert.bxbot.domain.exchange.ExchangeConfig exchangeConfig =
        new com.gazbert.bxbot.domain.exchange.ExchangeConfig();
    exchangeConfig.setAuthenticationConfig(authenticationConfig);
    exchangeConfig.setName(EXCHANGE_NAME);
    exchangeConfig.setAdapter(EXCHANGE_ADAPTER_IMPL_CLASS);
    exchangeConfig.setNetworkConfig(networkConfig);
    exchangeConfig.setOtherConfig(otherConfig);

    return exchangeConfig;
  }

  private static com.gazbert.bxbot.domain.exchange.ExchangeConfig
      someExchangeConfigWithoutNetworkConfig() {
    final Map<String, String> authenticationConfig = someAuthenticationConfig();
    final Map<String, String> otherConfig = someOtherConfig();
    final com.gazbert.bxbot.domain.exchange.ExchangeConfig exchangeConfig =
        new com.gazbert.bxbot.domain.exchange.ExchangeConfig();
    exchangeConfig.setAuthenticationConfig(authenticationConfig);
    exchangeConfig.setName(EXCHANGE_NAME);
    exchangeConfig.setAdapter(EXCHANGE_ADAPTER_IMPL_CLASS);
    exchangeConfig.setOtherConfig(otherConfig);
    return exchangeConfig;
  }

  private static NetworkConfig someNetworkConfig() {
    final NetworkConfig networkConfig = new NetworkConfig();
    networkConfig.setConnectionTimeout(EXCHANGE_ADAPTER_NETWORK_TIMEOUT);
    networkConfig.setNonFatalErrorCodes(EXCHANGE_ADAPTER_NONFATAL_ERROR_CODES);
    networkConfig.setNonFatalErrorMessages(EXCHANGE_ADAPTER_NONFATAL_ERROR_MESSAGES);
    return networkConfig;
  }

  private static Map<String, String> someAuthenticationConfig() {
    final Map<String, String> authenticationConfig = new HashMap<>();
    authenticationConfig.put(
        EXCHANGE_ADAPTER_AUTHENTICATION_CONFIG_ITEM_NAME,
        EXCHANGE_ADAPTER_AUTHENTICATION_CONFIG_ITEM_VALUE);
    return authenticationConfig;
  }

  private static Map<String, String> someOtherConfig() {
    final Map<String, String> otherConfig = new HashMap<>();
    otherConfig.put(
        EXCHANGE_ADAPTER_OTHER_CONFIG_ITEM_NAME, EXCHANGE_ADAPTER_OTHER_CONFIG_ITEM_VALUE);
    return otherConfig;
  }

  private static EngineConfig someEngineConfig() {
    final EngineConfig engineConfig = new EngineConfig();
    engineConfig.setEmergencyStopCurrency(ENGINE_EMERGENCY_STOP_CURRENCY);
    engineConfig.setEmergencyStopBalance(ENGINE_EMERGENCY_STOP_BALANCE);
    engineConfig.setTradeCycleInterval(ENGINE_TRADE_CYCLE_INTERVAL);
    return engineConfig;
  }

  private static EngineConfig someEngineConfigForNoEmergencyStopCheck() {
    final EngineConfig engineConfig = new EngineConfig();
    engineConfig.setEmergencyStopCurrency(ENGINE_EMERGENCY_STOP_CURRENCY);
    engineConfig.setEmergencyStopBalance(BigDecimal.ZERO); // ZERO bypasses the check
    engineConfig.setTradeCycleInterval(ENGINE_TRADE_CYCLE_INTERVAL);
    return engineConfig;
  }

  private static List<StrategyConfig> allTheStrategiesConfig() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(STRATEGY_CONFIG_ITEM_NAME, STRATEGY_CONFIG_ITEM_VALUE);

    final StrategyConfig strategyConfig1 =
        new StrategyConfig(
            STRATEGY_ID,
            STRATEGY_NAME,
            STRATEGY_DESCRIPTION,
            STRATEGY_IMPL_CLASS,
            STRATEGY_IMPL_BEAN,
            configItems);

    final List<StrategyConfig> allStrategies = new ArrayList<>();
    allStrategies.add(strategyConfig1);
    return allStrategies;
  }

  private static List<MarketConfig> allTheMarketsConfig() {
    final MarketConfig marketConfig1 =
        new MarketConfig(
            MARKET_ID,
            MARKET_NAME,
            MARKET_BASE_CURRENCY,
            MARKET_COUNTER_CURRENCY,
            MARKET_IS_ENABLED,
            STRATEGY_ID);
    final List<MarketConfig> allMarkets = new ArrayList<>();
    allMarkets.add(marketConfig1);
    return allMarkets;
  }

  private Callable<Boolean> engineStateChanged(TradingEngine engine, EngineState engineState) {
    return () -> {
      boolean stateChanged = false;

      // Startup requested and has started
      if (engineState == EngineState.RUNNING && engine.isRunning()) {
        stateChanged = true;
      }

      // Shutdown requested and has shutdown
      if (engineState == EngineState.SHUTDOWN && !engine.isRunning()) {
        stateChanged = true;
      }

      return stateChanged;
    };
  }
}
