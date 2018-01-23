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

import com.gazbert.bxbot.core.config.exchange.AuthenticationConfigImpl;
import com.gazbert.bxbot.core.config.exchange.ExchangeConfigImpl;
import com.gazbert.bxbot.core.config.exchange.NetworkConfigImpl;
import com.gazbert.bxbot.core.config.exchange.OptionalConfigImpl;
import com.gazbert.bxbot.core.config.market.MarketImpl;
import com.gazbert.bxbot.core.config.strategy.StrategyConfigItems;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.core.util.ConfigurableComponentFactory;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.domain.exchange.AuthenticationConfig;
import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import com.gazbert.bxbot.domain.exchange.OptionalConfig;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.services.EngineConfigService;
import com.gazbert.bxbot.services.ExchangeConfigService;
import com.gazbert.bxbot.services.MarketConfigService;
import com.gazbert.bxbot.services.StrategyConfigService;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.TradingApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

/**
 * The main Trading Engine.
 * <p>
 * The engine has been coded to fail *hard and fast* whenever something unexpected happens. If Email
 * Alerts are enabled, a message will be sent with details of the problem before the bot is shutdown.
 * <p>
 * The only time the bot does not fail hard and fast is for network issues connecting to the exchange - it logs the error
 * and retries at next trade cycle.
 * <p>
 * To keep things simple:
 * - The engine is single threaded.
 * - The engine only supports trading on 1 exchange per instance of the bot, i.e. 1 Exchange Adapter per process.
 * - The engine only supports 1 Trading Strategy per Market.
 *
 * @author gazbert
 */
@Component
@ComponentScan(basePackages = {"com.gazbert.bxbot"})
public class TradingEngine {

    private static final Logger LOG = LogManager.getLogger();

    // Email Alert error message stuff
    private static final String CRITICAL_EMAIL_ALERT_SUBJECT = "CRITICAL Alert message from BX-bot";
    private static final String DETAILS_ERROR_MSG_LABEL = " Details: ";
    private static final String CAUSE_ERROR_MSG_LABEL = " Cause: ";
    private static final String NEWLINE = System.getProperty("line.separator");
    private static final String HORIZONTAL_RULE = "--------------------------------------------------" + NEWLINE;

    /*
     * Trade execution interval in secs. The time we wait/sleep in between trade cycles.
     */
    private static int tradeExecutionInterval;

    /*
     * Control flag decides if the Trading Engine lives or dies.
     */
    private volatile boolean keepAlive = true;

    /*
     * Is Trading Engine already running? Used to prevent multiple 'starts' of the engine.
     */
    private boolean isRunning = false;

    /*
     * Monitor to use when checking if Trading Engine is running.
     */
    private static final Object IS_RUNNING_MONITOR = new Object();

    /*
     * The thread the Trading Engine is running in.
     */
    private Thread engineThread;

    /*
     * Map of Trading Strategy descriptions from config.
     */
    private final Map<String, StrategyConfig> strategyDescriptions = new HashMap<>();

    /*
     * List of cached Trading Strategy implementations for the Trade Engine to execute.
     */
    private final List<TradingStrategy> tradingStrategiesToExecute = new ArrayList<>();

    /*
     * The emergency stop currency value is used to prevent a catastrophic loss on the exchange.
     * It is set to the currency short code, e.g. BTC, USD.
     * This is normally the currency you intend to hold a long position in.
     */
    private String emergencyStopCurrency;

    /*
     * The Emergency Stop balance.
     * It is used to prevent a catastrophic loss on the exchange.
     * The Trading Engine checks this value at the start of every trade cycle: if the balance on
     * the exchange drops below this value, the Trading Engine will stop trading on all markets.
     * Manual intervention is then required to restart the bot.
     */
    private BigDecimal emergencyStopBalance;

    private String botId;
    private String botName;

    private final EmailAlerter emailAlerter;
    private ExchangeAdapter exchangeAdapter;

    // Services
    private final ExchangeConfigService exchangeConfigService;
    private final EngineConfigService engineConfigService;
    private final StrategyConfigService strategyConfigService;
    private final MarketConfigService marketConfigService;

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    public TradingEngine(ExchangeConfigService exchangeConfigService, EngineConfigService engineConfigService,
                         StrategyConfigService strategyConfigService, MarketConfigService marketConfigService,
                         EmailAlerter emailAlerter) {

        LOG.info(() -> "Initialising Trading Engine...");

        this.exchangeConfigService = exchangeConfigService;
        this.engineConfigService = engineConfigService;
        this.strategyConfigService = strategyConfigService;
        this.marketConfigService = marketConfigService;
        this.emailAlerter = emailAlerter;
    }

    public void start() throws IllegalStateException {

        synchronized (IS_RUNNING_MONITOR) {
            if (isRunning) {
                final String errorMsg = "Cannot start Trading Engine because it is already running!";
                LOG.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            // first time to start
            isRunning = true;
        }

        // store this so we can shutdown the engine later
        engineThread = Thread.currentThread();

        initConfig();
        runMainControlLoop();
    }

    private void initConfig() {

        LOG.info(() -> "Initialising BX-bot config...");

        // the sequence order of these methods is significant - don't change it.
        loadExchangeAdapterConfig();
        loadEngineConfig();
        loadTradingStrategyConfig();
        loadMarketConfigAndInitialiseTradingStrategies();
    }

    /*
     * The main control loop.
     * We loop infinitely unless an unexpected exception occurs.
     * The code fails hard and fast if an unexpected occurs. Network exceptions *should* recover.
     */
    private void runMainControlLoop() {

        LOG.info(() -> "Starting Trading Engine for " + botId + " ...");

        while (keepAlive) {

            try {

                LOG.info(() -> "*** Starting next trade cycle... ***");

                // Emergency Stop Check MUST run at start of every trade cycle.
                if (isEmergencyStopLimitBreached()) {
                    break;
                }

                // Execute the Trading Strategies
                for (final TradingStrategy tradingStrategy : tradingStrategiesToExecute) {
                    LOG.info(() -> "Executing Trading Strategy ---> " + tradingStrategy.getClass().getSimpleName());
                    tradingStrategy.execute();
                }

                LOG.info(() -> "*** Sleeping " + tradeExecutionInterval + "s til next trade cycle... ***");

                try {
                    Thread.sleep(tradeExecutionInterval * 1000);
                } catch (InterruptedException e) {
                    LOG.warn("Control Loop thread interrupted when sleeping before next trade cycle");
                    Thread.currentThread().interrupt();
                }

            } catch (ExchangeNetworkException e) {

                /*
                 * We have a network connection issue reported by Exchange Adapter when called directly from
                 * Trading Engine. Current policy is to log it and sleep until next trade cycle.
                 */
                final String WARNING_MSG = "A network error has occurred in Exchange Adapter! " +
                        "BX-bot will try again in " + tradeExecutionInterval + "s...";
                LOG.error(WARNING_MSG, e);

                try {
                    Thread.sleep(tradeExecutionInterval * 1000);
                } catch (InterruptedException e1) {
                    LOG.warn("Control Loop thread interrupted when sleeping before next trade cycle");
                    Thread.currentThread().interrupt();
                }

            } catch (TradingApiException e) {

                /*
                 * A serious issue has occurred in the Exchange Adapter.
                 * Current policy is to log it, send email alert if required, and shutdown bot.
                 */
                final String FATAL_ERROR_MSG = "A FATAL error has occurred in Exchange Adapter!";
                LOG.fatal(FATAL_ERROR_MSG, e);
                emailAlerter.sendMessage(CRITICAL_EMAIL_ALERT_SUBJECT,
                        buildCriticalEmailAlertMsgContent(FATAL_ERROR_MSG +
                                DETAILS_ERROR_MSG_LABEL + e.getMessage() +
                                CAUSE_ERROR_MSG_LABEL + e.getCause(), e));
                keepAlive = false;

            } catch (StrategyException e) {

                /*
                 * A serious issue has occurred in the Trading Strategy.
                 * Current policy is to log it, send email alert if required, and shutdown bot.
                 */
                final String FATAL_ERROR_MSG = "A FATAL error has occurred in Trading Strategy!";
                LOG.fatal(FATAL_ERROR_MSG, e);
                emailAlerter.sendMessage(CRITICAL_EMAIL_ALERT_SUBJECT,
                        buildCriticalEmailAlertMsgContent(FATAL_ERROR_MSG +
                                DETAILS_ERROR_MSG_LABEL + e.getMessage() +
                                CAUSE_ERROR_MSG_LABEL + e.getCause(), e));
                keepAlive = false;

            } catch (Exception e) {

                /*
                 * A serious and *unexpected* issue has occurred in the Exchange Adapter or Trading Strategy.
                 * Current policy is to log it, send email alert if required, and shutdown bot.
                 */
                final String FATAL_ERROR_MSG = "An unexpected FATAL error has occurred in Exchange Adapter or Trading Strategy!";
                LOG.fatal(FATAL_ERROR_MSG, e);
                emailAlerter.sendMessage(CRITICAL_EMAIL_ALERT_SUBJECT,
                        buildCriticalEmailAlertMsgContent(FATAL_ERROR_MSG +
                                DETAILS_ERROR_MSG_LABEL + e.getMessage() +
                                CAUSE_ERROR_MSG_LABEL + e.getCause(), e));
                keepAlive = false;
            }
        }

        LOG.fatal("BX-bot " + botId + " is shutting down NOW!");
        synchronized (IS_RUNNING_MONITOR) {
            isRunning = false;
        }
    }

    /*
     * Shutdown the Trading Engine.
     * Might be called from a different thread.
     * TODO currently not used - but will eventually be called from Admin Console.
     */
    public void shutdown() {

        LOG.info(() -> "Shutdown request received!");
        LOG.info(() -> "Engine originally started in thread: " + engineThread);

        keepAlive = false;
        engineThread.interrupt(); // poke it in case bot is sleeping
    }

    synchronized boolean isRunning() {
        LOG.info(() -> "isRunning: " + isRunning);
        return isRunning;
    }

    /*
     * Checks if the Emergency Stop Currency (e.g. USD, BTC) wallet balance on exchange has gone *below* configured limit.
     * If the balance cannot be obtained or has dropped below the configured limit, we notify the main control loop to
     * immediately shutdown the bot.
     *
     * This check is here to help protect runaway losses due to:
     * - 'buggy' Trading Strategies
     * - Unforeseen bugs in the Trading Engine and Exchange Adapter
     * - the exchange sending corrupt order book data and the Trading Strategy being misled... this has happened.
     */
    private boolean isEmergencyStopLimitBreached() throws TradingApiException, ExchangeNetworkException {

        boolean isEmergencyStopLimitBreached = true;

        if (emergencyStopBalance.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        LOG.info(() -> "Performing Emergency Stop check...");

        BalanceInfo balanceInfo;
        try {
            balanceInfo = exchangeAdapter.getBalanceInfo();
        } catch (TradingApiException e) {
            final String errorMsg = "Failed to get Balance info from exchange to perform Emergency Stop check - letting"
                    + " Trade Engine error policy decide what to do next...";
            LOG.error(errorMsg, e);
            // re-throw to main loop - might only be connection issue and it will retry...
            throw e;
        }

        final Map<String, BigDecimal> balancesAvailable = balanceInfo.getBalancesAvailable();
        final BigDecimal currentBalance = balancesAvailable.get(emergencyStopCurrency);
        if (currentBalance == null) {
            final String errorMsg =
                    "Emergency stop check: Failed to get current Emergency Stop Currency balance as '"
                            + emergencyStopCurrency + "' key into Balances map "
                            + "returned null. Balances returned: " + balancesAvailable;
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        } else {

            LOG.info(() -> "Emergency Stop Currency balance available on exchange is ["
                    + new DecimalFormat("#.########").format(currentBalance) + "] "
                    + emergencyStopCurrency);

            LOG.info(() -> "Balance that will stop ALL trading across ALL markets is ["
                    + new DecimalFormat("#.########").format(emergencyStopBalance) + "] " + emergencyStopCurrency);

            if (currentBalance.compareTo(emergencyStopBalance) < 0) {
                final String balanceBlownErrorMsg =
                        "EMERGENCY STOP triggered! - Current Emergency Stop Currency [" + emergencyStopCurrency + "] wallet balance ["
                                + new DecimalFormat("#.########").format(currentBalance) + "] on exchange "
                                + "is lower than configured Emergency Stop balance ["
                                + new DecimalFormat("#.########").format(emergencyStopBalance) + "] " + emergencyStopCurrency;

                LOG.fatal(balanceBlownErrorMsg);
                emailAlerter.sendMessage(CRITICAL_EMAIL_ALERT_SUBJECT,
                        buildCriticalEmailAlertMsgContent(balanceBlownErrorMsg, null));
            } else {

                isEmergencyStopLimitBreached = false;
                LOG.info(() -> "Emergency Stop check PASSED!");
            }
        }
        return isEmergencyStopLimitBreached;
    }

    private String buildCriticalEmailAlertMsgContent(String errorDetails, Throwable exception) {

        final StringBuilder msgContent = new StringBuilder("A CRITICAL error event has occurred on BX-bot.");
        msgContent.append(NEWLINE).append(NEWLINE);

        msgContent.append(HORIZONTAL_RULE);
        msgContent.append("Bot Id / Name:");
        msgContent.append(NEWLINE).append(NEWLINE);
        msgContent.append(botId);
        msgContent.append(" / ");
        msgContent.append(botName);
        msgContent.append(NEWLINE).append(NEWLINE);

        msgContent.append(HORIZONTAL_RULE);
        msgContent.append("Exchange Adapter:");
        msgContent.append(NEWLINE).append(NEWLINE);
        msgContent.append(exchangeAdapter.getClass().getName());
        msgContent.append(NEWLINE).append(NEWLINE);

        msgContent.append(HORIZONTAL_RULE);
        msgContent.append("Event Time:");
        msgContent.append(NEWLINE).append(NEWLINE);
        msgContent.append(new Date());
        msgContent.append(NEWLINE).append(NEWLINE);

        msgContent.append(HORIZONTAL_RULE);
        msgContent.append("Event Details:");
        msgContent.append(NEWLINE).append(NEWLINE);
        msgContent.append(errorDetails);
        msgContent.append(NEWLINE).append(NEWLINE);

        msgContent.append(HORIZONTAL_RULE);
        msgContent.append("Action Taken:");
        msgContent.append(NEWLINE).append(NEWLINE);
        msgContent.append("The bot will shut down NOW! Check the bot logs for more information.");
        msgContent.append(NEWLINE).append(NEWLINE);

        if (exception != null) {
            msgContent.append(HORIZONTAL_RULE);
            msgContent.append("Stacktrace:");
            msgContent.append(NEWLINE).append(NEWLINE);
            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);
            msgContent.append(stringWriter.toString());
        }

        return msgContent.toString();
    }

    // ------------------------------------------------------------------------
    // Config loading methods
    // ------------------------------------------------------------------------

    private void loadExchangeAdapterConfig() {

        final ExchangeConfig domainExchangeConfig = exchangeConfigService.getExchangeConfig();
        LOG.info(() -> "Fetched Exchange config from repository: " + domainExchangeConfig);

        exchangeAdapter = ConfigurableComponentFactory.createComponent(domainExchangeConfig.getExchangeAdapter());
        LOG.info(() -> "Trading Engine will use Exchange Adapter for: " + exchangeAdapter.getImplName());

        final ExchangeConfigImpl adapterExchangeConfig = new ExchangeConfigImpl();

        // Fetch optional network config
        final NetworkConfig networkConfig = domainExchangeConfig.getNetworkConfig();
        if (networkConfig != null) {

            final NetworkConfigImpl adapterNetworkConfig = new NetworkConfigImpl();
            adapterNetworkConfig.setConnectionTimeout(networkConfig.getConnectionTimeout());

            // Grab optional non-fatal error codes
            final List<Integer> nonFatalErrorCodes = networkConfig.getNonFatalErrorCodes();
            if (nonFatalErrorCodes != null) {
                adapterNetworkConfig.setNonFatalErrorCodes(nonFatalErrorCodes);
            } else {
                LOG.info(() ->
                        "No (optional) NetworkConfiguration NonFatalErrorCodes have been set for Exchange Adapter: "
                                + exchangeAdapter.getImplName());
            }

            // Grab optional non-fatal error messages
            final List<String> nonFatalErrorMessages = networkConfig.getNonFatalErrorMessages();
            if (nonFatalErrorMessages != null) {
                adapterNetworkConfig.setNonFatalErrorMessages(nonFatalErrorMessages);
            } else {
                LOG.info(() ->
                        "No (optional) NetworkConfiguration NonFatalErrorMessages have been set for Exchange Adapter: "
                                + exchangeAdapter.getImplName());
            }

            adapterExchangeConfig.setNetworkConfig(adapterNetworkConfig);
            LOG.info(() -> "NetworkConfiguration has been set: " + adapterNetworkConfig);

        } else {
            LOG.info(() -> "No (optional) NetworkConfiguration has been set for Exchange Adapter: " + exchangeAdapter.getImplName());
        }

        // Fetch optional authentication config
        final AuthenticationConfig authenticationConfig = domainExchangeConfig.getAuthenticationConfig();
        if (authenticationConfig != null) {

            final AuthenticationConfigImpl adapterAuthenticationConfig = new AuthenticationConfigImpl();
            adapterAuthenticationConfig.setItems(authenticationConfig.getItems());
            adapterExchangeConfig.setAuthenticationConfig(adapterAuthenticationConfig);

            // WARNING - careful when you log this
//            LOG.info(() ->
//                    "AuthenticationConfiguration has been set: " + adapterAuthenticationConfig);

        } else {
            LOG.info(() -> "No (optional) AuthenticationConfiguration has been set for Exchange Adapter: " + exchangeAdapter.getImplName());
        }

        // Fetch optional config
        final OptionalConfig optionalConfig = domainExchangeConfig.getOptionalConfig();
        if (optionalConfig != null) {

            final OptionalConfigImpl adapterOptionalConfig = new OptionalConfigImpl();
            adapterOptionalConfig.setItems(optionalConfig.getItems());
            adapterExchangeConfig.setOptionalConfig(adapterOptionalConfig);
            LOG.info(() -> "Optional Exchange Adapter config has been set: " + adapterOptionalConfig);

        } else {
            LOG.info(() -> "No Optional config has been set for Exchange Adapter: " + exchangeAdapter.getImplName());
        }

        exchangeAdapter.init(adapterExchangeConfig);
    }

    private void loadEngineConfig() {

        final EngineConfig engineConfig = engineConfigService.getEngineConfig();
        LOG.info(() -> "Fetched Engine config from repository: " + engineConfig);

        botId = engineConfig.getBotId();
        botName = engineConfig.getBotName();

        tradeExecutionInterval = engineConfig.getTradeCycleInterval();
        emergencyStopCurrency = engineConfig.getEmergencyStopCurrency();
        emergencyStopBalance = engineConfig.getEmergencyStopBalance();
    }

    private void loadTradingStrategyConfig() {

        final List<StrategyConfig> strategies = strategyConfigService.getAllStrategyConfig();
        LOG.debug(() -> "Fetched Strategy config from repository: " + strategies);

        for (final StrategyConfig strategy : strategies) {
            strategyDescriptions.put(strategy.getId(), strategy);
            LOG.info(() -> "Registered Trading Strategy with Trading Engine - ID: " + strategy.getId());
        }
    }

    private void loadMarketConfigAndInitialiseTradingStrategies() {

        final List<MarketConfig> markets = marketConfigService.getAllMarketConfig();
        LOG.info(() -> "Fetched Markets config from repository: " + markets);

        // used only as crude mechanism for checking for duplicate Markets
        final Set<Market> loadedMarkets = new HashSet<>();

        // Load em up and create the Strategies
        for (final MarketConfig market : markets) {

            final String marketName = market.getName();
            if (!market.isEnabled()) {
                LOG.info(() -> marketName + " market is NOT enabled for trading - skipping to next market...");
                continue;
            }

            final Market tradingMarket = new MarketImpl(marketName, market.getId(), market.getBaseCurrency(), market.getCounterCurrency());
            final boolean wasAdded = loadedMarkets.add(tradingMarket);
            if (!wasAdded) {
                final String errorMsg = "Found duplicate Market! Market details: " + market;
                LOG.fatal(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // Get the strategy to use for this Market
            final String strategyToUse = market.getTradingStrategyId();
            LOG.info(() -> "Market Trading Strategy Id: " + strategyToUse);

            if (strategyDescriptions.containsKey(strategyToUse)) {
                final StrategyConfig tradingStrategy = strategyDescriptions.get(strategyToUse);

                // Grab optional config for the Trading Strategy
                final StrategyConfigItems tradingStrategyConfig = new StrategyConfigItems();
                final Map<String, String> configItems = tradingStrategy.getConfigItems();
                if (configItems != null) {
                    tradingStrategyConfig.setItems(configItems);
                } else {
                    LOG.info(() -> "No (optional) configuration has been set for Trading Strategy: " + strategyToUse);
                }

                LOG.info(() -> "StrategyConfigImpl (optional): " + tradingStrategyConfig);

                /*
                 * Load the Trading Strategy impl, instantiate it, set its config, and store in the cached
                 * Trading Strategy execution list.
                 */
                TradingStrategy strategyImpl = obtainTradingStrategyInstance(tradingStrategy);;
                strategyImpl.init(exchangeAdapter, tradingMarket, tradingStrategyConfig);

                LOG.info(() -> "Initialized trading strategy successfully. Name: [" + tradingStrategy.getName()
                        + "] Class: " + tradingStrategy.getClassName());

                tradingStrategiesToExecute.add(strategyImpl);
            } else {

                // Game over. Config integrity blown - we can't find strat.
                final String errorMsg = "Failed to find matching Strategy for Market " + market
                        + " - The Strategy " + "[" + strategyToUse + "] cannot be found in the "
                        + " Strategy Descriptions map: " + strategyDescriptions;
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        }

        LOG.info(() -> "Loaded and set Market configuration successfully!");
    }

    private TradingStrategy obtainTradingStrategyInstance(StrategyConfig tradingStrategy) {
        final String tradingStrategyClassname = tradingStrategy.getClassName();
        final String tradingStrategyBeanName = tradingStrategy.getBeanName();

        TradingStrategy strategyImpl = null;
        if (tradingStrategyBeanName != null) {
            // if beanName is configured, try get the bean first
            try {
                strategyImpl = (TradingStrategy) springContext.getBean(tradingStrategyBeanName);

            } catch (NullPointerException e) {
                final String errorMsg = "Failed to obtain bean ["+tradingStrategyBeanName+"] from spring context";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        }
        if (strategyImpl == null) {
            // if beanName not configured use className
            strategyImpl = ConfigurableComponentFactory.createComponent(tradingStrategyClassname);
        }
        return strategyImpl;
    }
}