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

/**
 * The main Trading Engine.
 *
 * <p>The engine has been coded to fail *hard and fast* whenever something unexpected happens. If
 * Email Alerts are enabled, a message will be sent with details of the problem before the bot is
 * shutdown.
 *
 * <p>The only time the bot does not fail hard and fast is for network issues connecting to the
 * exchange - it logs the error and retries at next trade cycle.
 *
 * <p>To keep things simple:
 *
 * <ul>
 *   <li>The engine is single threaded.
 *   <li>The engine only supports trading on 1 exchange per instance of the bot, i.e. 1 Exchange
 *       Adapter per process.
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

  /** Creates the Trading Engine. */
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

  /** Starts the bot. */
  public void start() {
    synchronized (IS_RUNNING_MONITOR) {
      if (isRunning) {
        final String errorMsg = "Cannot start Trading Engine because it is already running!";
        LOG.error(() -> errorMsg);
        throw new IllegalStateException(errorMsg);
      }
      isRunning = true;
    }

    // store this so we can shutdown the engine later
    engineThread = Thread.currentThread();

    init();
    runMainControlLoop();
  }

  private void init() {
    LOG.info(() -> "Initialising Trading Engine...");
    // the sequence order of these methods is significant - don't change it.
    exchangeAdapter = loadExchangeAdapter();
    engineConfig = loadEngineConfig();
    tradingStrategies = loadTradingStrategies();
  }

  /*
   * The main control loop.
   * We loop infinitely unless an unexpected exception occurs.
   * The code fails hard and fast if an unexpected occurs. Network exceptions *should* recover.
   */
  private void runMainControlLoop() {
    LOG.info(() -> "Starting Trading Engine for " + engineConfig.getBotId() + " ...");
    while (keepAlive) {
      try {
        LOG.info(() -> "*** Starting next trade cycle... ***");

        // Emergency Stop Check MUST run at start of every trade cycle.
        if (isEmergencyStopLimitBreached()) {
          break;
        }

        for (final TradingStrategy tradingStrategy : tradingStrategies) {
          LOG.info(
              () ->
                  "Executing Trading Strategy ---> " + tradingStrategy.getClass().getSimpleName());
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

    // We've broken out of the control loop due to error or admin shutdown request
    LOG.fatal(() -> "BX-bot " + engineConfig.getBotId() + " is shutting down NOW!");
    synchronized (IS_RUNNING_MONITOR) {
      isRunning = false;
    }
  }

  /*
   * Shutdown the Trading Engine.
   * Might be called from a different thread.
   * Currently not used, but will eventually be called from BX-bot UI.
   */
  void shutdown() {
    LOG.info(() -> "Shutdown request received!");
    LOG.info(() -> "Engine originally started in thread: " + engineThread);
    keepAlive = false;
    engineThread.interrupt(); // poke it in case bot is sleeping
  }

  synchronized boolean isRunning() {
    LOG.info(() -> "isRunning: " + isRunning);
    return isRunning;
  }

  private void sleepUntilNextTradingCycle() {
    LOG.info(
        () ->
            "*** Sleeping "
                + engineConfig.getTradeCycleInterval()
                + "s til next trade cycle... ***");
    try {
      Thread.sleep(engineConfig.getTradeCycleInterval() * 1000L);
    } catch (InterruptedException e) {
      LOG.warn(() -> "Control Loop thread interrupted when sleeping before next trade cycle");
      Thread.currentThread().interrupt();
    }
  }

  /*
   * We have a network connection issue reported by Exchange Adapter when called directly from
   * Trading Engine. Current policy is to log it and sleep until next trade cycle.
   */
  private void handleExchangeNetworkException(ExchangeNetworkException e) {
    final String errorMessage =
        "A network error has occurred in Exchange Adapter! "
            + "BX-bot will try again in "
            + engineConfig.getTradeCycleInterval()
            + "s...";
    LOG.error(() -> errorMessage, e);

    try {
      Thread.sleep(engineConfig.getTradeCycleInterval() * 1000L);
    } catch (InterruptedException e1) {
      LOG.warn(() -> "Control Loop thread interrupted when sleeping before next trade cycle");
      Thread.currentThread().interrupt();
    }
  }

  /*
   * A serious issue has occurred in the Exchange Adapter.
   * Current policy is to log it, send email alert if required, and shutdown bot.
   */
  private void handleTradingApiException(TradingApiException e) {
    final String fatalErrorMessage = "A FATAL error has occurred in Exchange Adapter!";
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

  /*
   * A serious issue has occurred in the Trading Strategy.
   * Current policy is to log it, send email alert if required, and shutdown bot.
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

  /*
   * A serious and *unexpected* issue has occurred in the Exchange Adapter or Trading
   * Strategy. Current policy is to log it, send email alert if required, and shutdown bot.
   */
  private void handleUnexpectedException(Exception e) {
    final String fatalErrorMsg =
        "An unexpected FATAL error has occurred in Exchange Adapter or " + "Trading Strategy!";
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
      return false; // by-pass the emergency stop check
    }
    return EmergencyStopChecker.isEmergencyStopLimitBreached(
        exchangeAdapter, engineConfig, emailAlerter);
  }

  private ExchangeAdapter loadExchangeAdapter() {
    final ExchangeConfig exchangeConfig = exchangeConfigService.getExchangeConfig();
    LOG.info(() -> "Fetched Exchange config from repository: " + exchangeConfig);

    final ExchangeAdapter adapter =
        ConfigurableComponentFactory.createComponent(exchangeConfig.getAdapter());
    LOG.info(() -> "Trading Engine will use Exchange Adapter for: " + adapter.getImplName());

    final ExchangeConfigImpl exchangeApiConfig =
        ExchangeApiConfigBuilder.buildConfig(exchangeConfig);
    adapter.init(exchangeApiConfig);
    return adapter;
  }

  private EngineConfig loadEngineConfig() {
    final EngineConfig loadedEngineConfig = engineConfigService.getEngineConfig();
    LOG.info(() -> "Fetched Engine config from repository: " + loadedEngineConfig);
    return loadedEngineConfig;
  }

  private List<TradingStrategy> loadTradingStrategies() {
    final List<StrategyConfig> strategies = strategyConfigService.getAllStrategyConfig();
    LOG.info(() -> "Fetched Strategy config from repository: " + strategies);
    final List<MarketConfig> markets = marketConfigService.getAllMarketConfig();
    LOG.info(() -> "Fetched Markets config from repository: " + markets);
    return tradingStrategiesBuilder.buildStrategies(strategies, markets, exchangeAdapter);
  }
}
