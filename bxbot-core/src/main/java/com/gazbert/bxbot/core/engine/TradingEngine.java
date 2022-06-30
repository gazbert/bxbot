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

import com.gazbert.bxbot.core.config.exchange.ExchangeApiConfigBuilder;
import com.gazbert.bxbot.core.config.exchange.ExchangeConfigImpl;
import com.gazbert.bxbot.core.config.strategy.TradingStrategiesBuilder;
import com.gazbert.bxbot.core.mail.EmailAlertMessageBuilder;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.core.util.ConfigurableComponentFactory;
import com.gazbert.bxbot.core.util.EmergencyStopChecker;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.services.config.EngineConfigService;
import com.gazbert.bxbot.services.config.ExchangeConfigService;
import com.gazbert.bxbot.services.config.MarketConfigService;
import com.gazbert.bxbot.services.config.StrategyConfigService;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**主要的交易引擎。
 * The main Trading Engine.
 *
 * //引擎已经被编写成当意外事件发生时就会迅速出现故障。如果
 *  电子邮件提醒是启用的，一个消息将发送与问题的详细信息之前的机器人
 *  关闭
 * <p>The engine has been coded to fail *hard and fast* whenever something unexpected happens. If
 Email Alerts are enabled, a message will be sent with details of the problem before the bot is
 shutdown.

 *
 * //机器人唯一不会快速失败的情况是在网络问题时连接到
 * Exchange -它记录错误并在下一个交易周期重试
 * <p>The only time the bot does not fail hard and fast is for network issues connecting to the
  exchange - it logs the error and retries at next trade cycle.
//为了让事情变得简单
 * <p>To keep things simple:
 *
 * <ul> //引擎是单线程的
 *   <li>The engine is single threaded.
 *
 *   //引擎只支持每个机器人实例的1个交易所交易，即1个交易所
 * 适配器/过程。
 *   <li>The engine only supports trading on 1 exchange per instance of the bot, i.e. 1 Exchange
 *       Adapter per process.
 *    //引擎只支持1个交易策略每个市场
 *   <li>The engine only supports 1 Trading Strategy per Market.
 * </ul>
 *
 * @author gazbert
 */
@Component
@ComponentScan(basePackages = {"com.gazbert.bxbot"})
public class TradingEngine {

  private static final Logger LOG = LogManager.getLogger();

  private static final String CRITICAL_EMAIL_ALERT_SUBJECT = "CRITICAL Alert message from BX-bot";
  private static final String DETAILS_ERROR_MSG_LABEL = " Details: ";
  private static final String CAUSE_ERROR_MSG_LABEL = " Cause: ";

  private static final Object IS_RUNNING_MONITOR = new Object();
  private Thread engineThread;
  private volatile boolean keepAlive = true;
  private boolean isRunning = false;

  private final EmailAlerter emailAlerter;
  private List<TradingStrategy> tradingStrategies;
  private EngineConfig engineConfig;
  private ExchangeAdapter exchangeAdapter;

  private final ExchangeConfigService exchangeConfigService;
  private final EngineConfigService engineConfigService;
  private final StrategyConfigService strategyConfigService;
  private final MarketConfigService marketConfigService;

  private final TradingStrategiesBuilder tradingStrategiesBuilder;

  /**
   * Creates the Trading Engine.
   * 创建交易引擎。
   *
   * @param exchangeConfigService the Exchange config service.
   *                              Exchange 配置服务。
   *
   * @param engineConfigService the Engine config service.
   *                            引擎配置服务。
   *
   * @param strategyConfigService the Strategy config service.
   *                              策略配置服务。
   *
   * @param marketConfigService the Market config service.
   *                            市场配置服务。
   *
   * @param emailAlerter the Email Alerter.
   *                     电子邮件警报器。
   *
   * @param tradingStrategiesBuilder the Trading Strategies Builder.
   *                                 交易策略生成器。
   */
  @Autowired
  public TradingEngine(
      ExchangeConfigService exchangeConfigService,
      EngineConfigService engineConfigService,
      StrategyConfigService strategyConfigService,
      MarketConfigService marketConfigService,
      EmailAlerter emailAlerter,
      TradingStrategiesBuilder tradingStrategiesBuilder) {

    this.exchangeConfigService = exchangeConfigService;
    this.engineConfigService = engineConfigService;
    this.strategyConfigService = strategyConfigService;
    this.marketConfigService = marketConfigService;
    this.emailAlerter = emailAlerter;
    this.tradingStrategiesBuilder = tradingStrategiesBuilder;
  }

  /** Starts the bot.
   * 启动机器人。*/
  public void start() {
    synchronized (IS_RUNNING_MONITOR) {
      if (isRunning) {
        final String errorMsg = "Cannot start Trading Engine because it is already running! 无法启动交易引擎，因为它已经在运行！";
        LOG.error(() -> errorMsg);
        throw new IllegalStateException(errorMsg);
      }
      isRunning = true;
    }

    // store this so we can shutdown the engine later   // 存储这个，以便我们以后可以关闭引擎
    engineThread = Thread.currentThread();

    init();
    runMainControlLoop();
  }

  private void init() {
    LOG.info(() -> "Initialising Trading Engine...正在初始化交易引擎...");
    // the sequence order of these methods is significant - don't change it.  ` // 这些方法的顺序很重要——不要改变它。
    exchangeAdapter = loadExchangeAdapter();
    engineConfig = loadEngineConfig();
    tradingStrategies = loadTradingStrategies();
  }

  /*
   * The main control loop.   主控制回路。
   * We loop infinitely unless an unexpected exception occurs.    我们无限循环，除非发生意外异常。
   * The code fails hard and fast if an unexpected occurs. Network exceptions *should* recover.  * 如果发生意外，代码会快速失败。网络异常*应该*恢复。
   */
  private void runMainControlLoop() {
    LOG.info(() -> "Starting Trading Engine for  启动交易引擎" + engineConfig.getBotId() + " ...");
    while (keepAlive) {
      try {
        LOG.info(() -> "*** Starting next trade cycle... *** *** 开始下一个交易周期... ***");

        // Emergency Stop Check MUST run at start of every trade cycle.  紧急停止检查必须在每个交易周期开始时运行。
        if (isEmergencyStopLimitBreached()) {
          break;
        }

        for (final TradingStrategy tradingStrategy : tradingStrategies) {
          LOG.info(
              () ->
                  "Executing Trading Strategy --->  执行交易策略--->" + tradingStrategy.getClass().getSimpleName());
          tradingStrategy.execute();
        }

        sleepUntilNextTradingCycle();

      } catch (ExchangeNetworkException e) {
        handleExchangeNetworkException(e);

      } catch (TradingApiException e) {
        handleTradingApiException(e);

      } catch (StrategyException e) {
        handleStrategyException(e);

      } catch (Exception e) {
        handleUnexpectedException(e);
      }
    }

    // We've broken out of the control loop due to error or admin shutdown request  // 由于错误或管理员关闭请求，我们已经脱离了控制循环
    LOG.fatal(() -> "BX-bot " + engineConfig.getBotId() + " is shutting down NOW! 现在正在关闭！");
    synchronized (IS_RUNNING_MONITOR) {
      isRunning = false;
    }
  }

  /*
   * Shutdown the Trading Engine.   关闭交易引擎。
   * Might be called from a different thread.   可能从不同的线程调用。
   * Currently not used, but will eventually be called from BX-bot UI.    * 目前未使用，但最终会从 BX-bot UI 调用。
   */
  void shutdown() {
    LOG.info(() -> "Shutdown request received! 收到关机请求！");
    LOG.info(() -> "Engine originally started in thread:  引擎最初在线程中启动：" + engineThread);
    keepAlive = false;
    engineThread.interrupt(); // poke it in case bot is sleeping  // 戳它以防机器人正在睡觉
  }

  synchronized boolean isRunning() {
    LOG.info(() -> "isRunning: 在跑：" + isRunning);
    return isRunning;
  }

  private void sleepUntilNextTradingCycle() {
    LOG.info(
        () ->
            "*** Sleeping 睡眠 "
                + engineConfig.getTradeCycleInterval()
                + "s til next trade cycle... *** 直到下一个交易周期... ***");
    try {
      Thread.sleep(engineConfig.getTradeCycleInterval() * 1000L);
    } catch (InterruptedException e) {
      LOG.warn(() -> "Control Loop thread interrupted when sleeping before next trade cycle 在下一个交易周期之前休眠时控制循环线程中断");
      Thread.currentThread().interrupt();
    }
  }

  /**
   * We have a network connection issue reported by Exchange Adapter when called directly from Trading Engine. Current policy is to log it and sleep until next trade cycle.
   * 当前的策略是记录它并休眠到下一个交易周期。
   */
  private void handleExchangeNetworkException(ExchangeNetworkException e) {
    final String errorMessage =
        "A network error has occurred in Exchange Adapter! “Exchange 适配器发生网络错误！”"
            + "BX-bot will try again in BX-bot 将在 "
            + engineConfig.getTradeCycleInterval()
            + "s...";
    LOG.error(() -> errorMessage, e);

    try {
      Thread.sleep(engineConfig.getTradeCycleInterval() * 1000L);
    } catch (InterruptedException e1) {
      LOG.warn(() -> "Control Loop thread interrupted when sleeping before next trade cycle 在下一个交易周期之前休眠时控制循环线程中断");
      Thread.currentThread().interrupt();
    }
  }

  /**
   * A serious issue has occurred in the Exchange Adapter.
   * Exchange 适配器中出现了严重问题。
   *
   *Current policy is to log it, send email alert if required, and shutdown bot.
   * 当前的政策是记录它，在需要时发送电子邮件警报，然后关闭机器人。
   */
  private void handleTradingApiException(TradingApiException e) {
    final String fatalErrorMessage = "A FATAL error has occurred in Exchange Adapter! Exchange 适配器中出现致命错误！";
    LOG.fatal(() -> fatalErrorMessage, e);
    emailAlerter.sendMessage(
        CRITICAL_EMAIL_ALERT_SUBJECT,
        EmailAlertMessageBuilder.buildCriticalMsgContent(
            fatalErrorMessage
                + DETAILS_ERROR_MSG_LABEL
                + e.getMessage()
                + CAUSE_ERROR_MSG_LABEL
                + e.getCause(),
            e,
            engineConfig.getBotId(),
            engineConfig.getBotName(),
            exchangeAdapter.getClass().getName()));
    keepAlive = false;
  }

  /**
   * A serious issue has occurred in the Trading Strategy.
   *  交易策略出现了严重问题。
   *
   * Current policy is to log it, send email alert if required, and shutdown bot.
   * 当前的政策是记录它，在需要时发送电子邮件警报，然后关闭机器人。
   */
  private void handleStrategyException(StrategyException e) {
    final String fatalErrorMsg = "A FATAL error has occurred in Trading Strategy!";
    LOG.fatal(() -> fatalErrorMsg, e);
    emailAlerter.sendMessage(
        CRITICAL_EMAIL_ALERT_SUBJECT,
        EmailAlertMessageBuilder.buildCriticalMsgContent(
            fatalErrorMsg
                + DETAILS_ERROR_MSG_LABEL
                + e.getMessage()
                + CAUSE_ERROR_MSG_LABEL
                + e.getCause(),
            e,
            engineConfig.getBotId(),
            engineConfig.getBotName(),
            exchangeAdapter.getClass().getName()));
    keepAlive = false;
  }

  /**
   * A serious and *unexpected* issue has occurred in the Exchange Adapter or Trading
   Strategy. Current policy is to log it, send email alert if required, and shutdown bot.
   Exchange Adapter 或 Trading 中发生了严重且*意外*的问题
   战略。当前的政策是记录它，在需要时发送电子邮件警报，然后关闭机器人。
   */
  private void handleUnexpectedException(Exception e) {
    final String fatalErrorMsg =
        " An unexpected FATAL error has occurred in Exchange Adapter or " + "Trading Strategy! Exchange 适配器或交易策略中出现意外的致命错误！";
    LOG.fatal(() -> fatalErrorMsg, e);
    emailAlerter.sendMessage(
        CRITICAL_EMAIL_ALERT_SUBJECT,
        EmailAlertMessageBuilder.buildCriticalMsgContent(
            fatalErrorMsg
                + DETAILS_ERROR_MSG_LABEL
                + e.getMessage()
                + CAUSE_ERROR_MSG_LABEL
                + e.getCause(),
            e,
            engineConfig.getBotId(),
            engineConfig.getBotName(),
            exchangeAdapter.getClass().getName()));
    keepAlive = false;
  }

  private boolean isEmergencyStopLimitBreached()
      throws TradingApiException, ExchangeNetworkException {
    if (engineConfig.getEmergencyStopBalance().compareTo(BigDecimal.ZERO) == 0) {
      return false; // by-pass the emergency stop check  //绕过紧急停止检查
    }
    return EmergencyStopChecker.isEmergencyStopLimitBreached(
        exchangeAdapter, engineConfig, emailAlerter);
  }

  private ExchangeAdapter loadExchangeAdapter() {
    final ExchangeConfig exchangeConfig = exchangeConfigService.getExchangeConfig();
    LOG.info(() -> "Fetched Exchange config from repository: 从存储库获取 Exchange 配置：" + exchangeConfig);

    final ExchangeAdapter adapter =
        ConfigurableComponentFactory.createComponent(exchangeConfig.getAdapter());
    LOG.info(() -> "Trading Engine will use Exchange Adapter for: 交易引擎将使用 Exchange Adapter 进行：" + adapter.getImplName());

    final ExchangeConfigImpl exchangeApiConfig =
        ExchangeApiConfigBuilder.buildConfig(exchangeConfig);
    adapter.init(exchangeApiConfig);
    return adapter;
  }

  private EngineConfig loadEngineConfig() {
    final EngineConfig loadedEngineConfig = engineConfigService.getEngineConfig();
    LOG.info(() -> "Fetched Engine config from repository: 从存储库中获取引擎配置：" + loadedEngineConfig);
    return loadedEngineConfig;
  }

  private List<TradingStrategy> loadTradingStrategies() {
    final List<StrategyConfig> strategies = strategyConfigService.getAllStrategyConfig();
    LOG.info(() -> "Fetched Strategy config from repository: 从存储库中获取策略配置：" + strategies);
    final List<MarketConfig> markets = marketConfigService.getAllMarketConfig();
    LOG.info(() -> "Fetched Markets config from repository: 从存储库获取市场配置：" + markets);
    return tradingStrategiesBuilder.buildStrategies(strategies, markets, exchangeAdapter);
  }
}
