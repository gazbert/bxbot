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
 * 测试交易引擎的行为是否符合预期。
 *
 * <p>The Exchange Adapter and Configuration subsystem are mocked out; they have their own unit tests.
 * <p>Exchange 适配器和配置子系统被模拟出来；他们有自己的单元测试。
 *
 * <p>TradingEngine.class is prepared so we can mock constructors for Market + StrategyConfigItems
  object creation.
 <p>TradingEngine.class 已准备好，因此我们可以模拟 Market + StrategyConfigItems 的构造函数
 对象创建。
 *
 * <p>There's a lot of time dependent stuff going on here; I hate these sorts of tests!
 * <p>这里有很多时间相关的东西；我讨厌这样的测试！
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurableComponentFactory.class})
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

  private static final String CRITICAL_EMAIL_ALERT_SUBJECT = "CRITICAL Alert message from BX-bot 来自 BX-bot 的 CRITICAL Alert 消息";

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
   * * 模拟配置子系统；我们没有在这里测试它 - 有它自己的单元测试。
   *
   * <p>Test scenarios use 1 market with 1 strategy with 1 config item to keep tests manageable.
   * <p>测试场景使用 1 个市场和 1 个策略和 1 个配置项来保持测试的可管理性。
   *
   * Loading multiple markets/strategies is tested in the Configuration subsystem unit tests.
   * * 加载多个市场/策略在配置子系统单元测试中进行测试。
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
    // BTC 的余额已被突破
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, btcBalance);

    // expect BalanceInfo to be fetched using Trading API
    // 期望使用交易 API 获取 BalanceInfo
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);

    // expect Email Alert to be sent
    // 期望发送电子邮件警报
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains(
                "EMERGENCY STOP triggered! - Current Emergency Stop Currency [BTC] wallet balance [" +
                        "紧急停止触发！ - 当前紧急停止货币 [BTC] 钱包余额 ["
                    + new DecimalFormat("#.########").format(btcBalance))
            + "] on exchange is lower than configured Emergency Stop balance [" +
                "] 交换时低于配置的紧急停止余额 ["
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
    // 期望交易策略至少被调用一次
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

  /**
   * Tests the engine starts up and executes trade cycles successfully.
   * 测试引擎启动并成功执行交易周期。
   *
   * Scenario is at least one successful trade cycle and then we shut it down.
   * 情景至少是一个成功的交易周期，然后我们将其关闭。
   */
  @Test
  public void testEngineExecutesTradeCyclesAndCanBeShutdownSuccessfully() throws Exception {
    setupConfigLoadingExpectations();

    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    // 未违反 BTC 的余额限制
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));

    // expect BalanceInfo to be fetched using Trading API
    // 期望使用交易 API 获取 BalanceInfo
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo).atLeastOnce();
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable).atLeastOnce();

    // expect Trading Strategy to be invoked
    // 期望交易策略被调用
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

  /**
   * Tests the engine starts up, executes 1 trade cycle successfully, but then receives
   StrategyException from Trading Strategy - we expect the engine to shutdown.
   * 测试引擎启动，成功执行 1 个交易周期，但随后收到
   来自 Trading Strategy 的 StrategyException - 我们预计引擎会关闭。
   */
  @Test
  public void testEngineShutsDownWhenItReceivesStrategyExceptionFromTradingStrategy()
      throws Exception {
    setupConfigLoadingExpectations();

    final String exceptionErrorMsg = "Eeek! My strat just broke. Please shutdown! 哎呀！我的策略刚刚坏了。请关机！";
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    // 未违反 BTC 的余额限制
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

    // expect 1st trade cycle to be successful
    // 期望第一个交易周期成功
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();

    // expect StrategyException in 2nd trade cycle
    // 期望在第二个交易周期出现 StrategyException
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();
    expectLastCall().andThrow(new StrategyException(exceptionErrorMsg));

    // expect Email Alert to be sent
    // 期望发送电子邮件警报
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains("A FATAL error has occurred in Trading Strategy! Details: 交易策略出现致命错误！细节：" + exceptionErrorMsg));
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

  /**
   * Tests the engine starts up, executes 1 trade cycle successfully, but then receives unexpected
   Exception from Trading Strategy - we expect the engine to shutdown.
   * 测试引擎启动，成功执行 1 个交易周期，但随后收到意外
   交易策略的例外 - 我们预计引擎会关闭。
   */
  @Test
  public void testEngineShutsDownWhenItReceivesUnexpectedExceptionFromTradingStrategy()
      throws Exception {
    setupConfigLoadingExpectations();

    final String exceptionErrorMsg = "Ah, curse your sudden but inevitable betrayal!" +
            "啊，诅咒你突然但不可避免的背叛！";
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

    // expect 1st trade cycle to be successful
    // 期望第一个交易周期成功
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();

    // expect unexpected Exception in 2nd trade cycle
    // 预计第二个交易周期出现意外异常
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();
    expectLastCall().andThrow(new IllegalArgumentException(exceptionErrorMsg));

    // expect Email Alert to be sent
    // 期望发送电子邮件警报
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains(
            "An unexpected FATAL error has occurred in Exchange Adapter or Trading Strategy! " +
                    "Exchange 适配器或交易策略中出现意外的致命错误！"
                + "Details: 细节："
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

  /**
   * Tests the engine starts up, executes 1 trade cycle successfully, but then receives unexpected
    Exception from Exchange Adapter on the 2nd cycle. We expect the engine to shutdown.
   测试引擎启动，成功执行 1 个交易周期，但随后收到意外消息
   第 2 个周期的 Exchange 适配器异常。我们预计引擎会关闭。
   */
  @Test
  public void testEngineShutsDownWhenItReceivesUnexpectedExceptionFromExchangeAdapter()
      throws Exception {

    setupConfigLoadingExpectations();

    final String exceptionErrorMsg =
        "I had to rewire the grav thrust because somebody won't replace that crappy " +
                " 我不得不重新连接重力推力，因为有人不会取代那个蹩脚的东西"
            + "compression coil. 压缩线圈。";
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    // 未违反 BTC 的余额限制
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

    // expect 1st trade cycle to be successful
    // 期望第一个交易周期成功
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();

    // expect unexpected Exception in 2nd trade cycle
    // 预计第二个交易周期出现意外异常
    expect(exchangeAdapter.getBalanceInfo()).andThrow(new IllegalStateException(exceptionErrorMsg));

    // expect Email Alert to be sent
    // 期望发送电子邮件警报
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains(
            "An unexpected FATAL error has occurred in Exchange Adapter or Trading Strategy! " +
                    " Exchange 适配器或交易策略中出现意外的致命错误！"
                + " Details:  细节："
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

  /**
   * Tests the engine starts up, executes 1 trade cycle successfully, but then receives
    TradingApiException from Exchange Adapter on the 2nd cycle. We expect the engine to shutdown.
   * 测试引擎启动，成功执行 1 个交易周期，但随后收到
   第 2 个周期来自 Exchange 适配器的 TradingApiException。我们预计引擎会关闭。
   */
  @Test
  public void testEngineShutsDownWhenItReceivesTradingApiExceptionFromExchangeAdapter()
      throws Exception {
    setupConfigLoadingExpectations();

    final String exceptionErrorMsg =
        "Ten percent of nothin' is ... let me do the math here ... nothin' into nothin' " +
                "百分之十是……让我在这里算一算……什么都没有 "
            + " ... carry the nothin' ... ...随身携带什么...";
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    // 未违反 BTC 的余额限制
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

    // expect 1st trade cycle to be successful
    // 期望第一个交易周期成功
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();

    // expect TradingApiException in 2nd trade cycle
    // 期望在第二个交易周期出现 TradingApiException
    expect(exchangeAdapter.getBalanceInfo()).andThrow(new TradingApiException(exceptionErrorMsg));

    // expect Email Alert to be sent
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains(
            "A FATAL error has occurred in Exchange Adapter! Details: Exchange 适配器中出现致命错误！细节：" + exceptionErrorMsg));

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

  /**
   * Tests the engine continues to execute next trade cycle if it receives a
    ExchangeNetworkException. Scenario is 1 successful trade cycle, 2nd cycle Exchange Adapter
    throws ExchangeNetworkException, engine stays alive and Emergency Stop Check MUST run at start
    of every trade cycle.successfully executes subsequent trade cycles.
   测试引擎是否继续执行下一个交易周期，如果它收到一个
   交换网络异常。场景是 1 个成功的交易周期，第 2 个周期交换适配器
   抛出 ExchangeNetworkException，引擎保持活动状态并且紧急停止检查必须在启动时运行
   每个交易周期。成功执行后续交易周期。
   */
  @Test
  public void testEngineExecutesNextTradeCyclesAfterReceivingExchangeNetworkException()
      throws Exception {
    setupConfigLoadingExpectations();

    final String exceptionErrorMsg =
        "Man walks down the street in a hat like that, you know he's not afraid of anything..." +
                "男人戴着那样的帽子走在街上，你知道他什么都不怕……";

    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    // 未违反 BTC 的余额限制
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);

    // expect 1st trade cycle to be successful
    // 期望第一个交易周期成功
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);
    tradingStrategy.execute();

    // expect recoverable ExchangeNetworkException in 2nd trade cycle
    // 在第二个交易周期中期望可恢复的 ExchangeNetworkException
    expect(exchangeAdapter.getBalanceInfo())
        .andThrow(new ExchangeNetworkException(exceptionErrorMsg));

    // expect bot recover and continue 3rd cycle + any subsequent ones...
    // 期望机器人恢复并继续第 3 个周期 + 任何后续周期...
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
    // 等待几个交易周期，然后关闭机器人。
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
   * 测试发动机不能多次启动。
   */
  @Test(expected = IllegalStateException.class)
  public void testEngineCannotBeStartedMoreThanOnce() throws Exception {
    setupConfigLoadingExpectations();

    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    // balance limit NOT breached for BTC
    // 未违反 BTC 的余额限制
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));

    // expect BalanceInfo to be fetched using Trading API
    // 期望使用交易 API 获取 BalanceInfo
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo).atLeastOnce();
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable).atLeastOnce();

    // expect Trading Strategy to be invoked 1 time
    // 期望交易策略被调用 1 次
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
    // 尝试再次启动引擎
    tradingEngine.start();

    PowerMock.verifyAll();
  }

  @Test
  public void testEngineShutsDownWhenBalancesCannotBeFetchedFromExchange() throws Exception {
    setupConfigLoadingExpectations();

    // empty balances from exchange
    // 清空交易所的余额
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();

    // expect BalanceInfo to be fetched using Trading API
    // 期望使用交易 API 获取 BalanceInfo
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo);
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable);

    // expect Email Alert to be sent
    // 期望发送电子邮件警报
    emailAlerter.sendMessage(
        eq(CRITICAL_EMAIL_ALERT_SUBJECT),
        contains(
            "Emergency stop check: Failed to get current Emergency Stop Currency balance as 紧急停止检查：无法获取当前紧急停止货币余额为'"
                + ENGINE_EMERGENCY_STOP_CURRENCY
                + "' key into Balances map 进入余额地图"
                + "returned null. Balances returned: 返回空值。退回余额："
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
    // 未违反 BTC 的余额限制
    balancesAvailable.put(ENGINE_EMERGENCY_STOP_CURRENCY, new BigDecimal("0.5"));

    // expect BalanceInfo to be fetched using Trading API
    // 期望使用交易 API 获取 BalanceInfo
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andReturn(balanceInfo).atLeastOnce();
    expect(balanceInfo.getBalancesAvailable()).andReturn(balancesAvailable).atLeastOnce();

    // expect Trading Strategy to be invoked
    // 期望交易策略被调用
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
  //  private utils 私有工具
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
    engineConfig.setEmergencyStopBalance(BigDecimal.ZERO); // ZERO bypasses the check // ZERO 绕过检查
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
      // 请求启动并已启动
      if (engineState == EngineState.RUNNING && engine.isRunning()) {
        stateChanged = true;
      }

      // Shutdown requested and has shutdown
      // 请求关机并已关机
      if (engineState == EngineState.SHUTDOWN && !engine.isRunning()) {
        stateChanged = true;
      }

      return stateChanged;
    };
  }
}
