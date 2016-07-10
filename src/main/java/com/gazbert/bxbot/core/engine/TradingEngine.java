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

import com.gazbert.bxbot.core.api.exchange.ExchangeAdapter;
import com.gazbert.bxbot.core.api.trading.BalanceInfo;
import com.gazbert.bxbot.core.api.trading.ExchangeTimeoutException;
import com.gazbert.bxbot.core.api.trading.Market;
import com.gazbert.bxbot.core.api.trading.TradingApiException;
import com.gazbert.bxbot.core.api.strategy.StrategyException;
import com.gazbert.bxbot.core.api.strategy.TradingStrategy;
import com.gazbert.bxbot.core.config.ConfigurableComponentFactory;
import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.engine.generated.EngineType;
import com.gazbert.bxbot.core.config.exchange.AuthenticationConfigImpl;
import com.gazbert.bxbot.core.config.exchange.ExchangeConfigImpl;
import com.gazbert.bxbot.core.config.exchange.NetworkConfigImpl;
import com.gazbert.bxbot.core.config.exchange.OtherConfigImpl;
import com.gazbert.bxbot.core.config.exchange.generated.*;
import com.gazbert.bxbot.core.config.market.generated.MarketType;
import com.gazbert.bxbot.core.config.market.generated.MarketsType;
import com.gazbert.bxbot.core.config.strategy.StrategyConfigImpl;
import com.gazbert.bxbot.core.config.strategy.generated.ConfigItemType;
import com.gazbert.bxbot.core.config.strategy.generated.ConfigurationType;
import com.gazbert.bxbot.core.config.strategy.generated.StrategyType;
import com.gazbert.bxbot.core.config.strategy.generated.TradingStrategiesType;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.core.util.LogUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * The main Trading Engine.
 *
 * The engine has been coded to fail *hard and fast* whenever something unexpected happens. If Email
 * Alerts are enabled, a message will be sent with details of the problem before the bot is shutdown.
 *
 * The only time the bot does not fail hard and fast is for network issues connecting to the exchange - it logs the error
 * and retries at next trade cycle.
 *
 * To keep things simple:
 *
 * - The engine is single threaded; I'm working on a concurrent version.
 * - The engine only supports trading on 1 exchange per instance of the bot, i.e. 1 Exchange Adapter per process.
 * - The engine only supports 1 Trading Strategy per Market.
 *
 */
final public class TradingEngine {

    private static final Logger LOG = Logger.getLogger(TradingEngine.class);

    /*
     * Location of the config files (relative to project/installation root) used by the bot.
     */
    private static final String ENGINE_CONFIG_XML_FILENAME = "config/engine.xml";
    private static final String ENGINE_CONFIG_XSD_FILENAME = "config/schemas/engine.xsd";

    private static final String EXCHANGE_CONFIG_XML_FILENAME = "config/exchange.xml";
    private static final String EXCHANGE_CONFIG_XSD_FILENAME = "config/schemas/exchange.xsd";

    private static final String MARKETS_CONFIG_XML_FILENAME = "config/markets.xml";
    private static final String MARKETS_CONFIG_XSD_FILENAME = "config/schemas/markets.xsd";

    private static final String STRATEGIES_CONFIG_XML_FILENAME = "config/strategies.xml";
    private static final String STRATEGIES_CONFIG_XSD_FILENAME = "config/schemas/strategies.xsd";

    /*
     * Subject for Email Alerts sent by the engine.
     */
    private static final String CRITICAL_EMAIL_ALERT_SUBJECT = "CRITICAL Alert message from BX-bot";

    /*
     * Email Alert error message labels.
     */
    private static final String DETAILS_ERROR_MSG_LABEL =  " Details: ";
    private static final String CAUSE_ERROR_MSG_LABEL = " Cause: " ;

    /*
     * System Newline separator.
     */
    private static final String NEWLINE = System.getProperty("line.separator");

    /*
     * Horizontal rule divider for structuring the email message content.
     */
    private static final String HORIZONTAL_RULE = "--------------------------------------------------" + NEWLINE;

    /*
     * Trade execution interval in secs.
     * The time we wait/sleep in between trade cycles.
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
    private final Map<String, StrategyType> strategyDescriptions = new HashMap<>();

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

    /*
     * Email Alerter for sending messages when the bot is forced to shutdown.
     */
    private EmailAlerter emailAlerter;

    /*
     * The Exchange Adapter integrating with the exchange - it also provides the TradingApi impl.
     */
    private ExchangeAdapter exchangeAdapter;


    /*
     * Constructor loads the bot config and initialises the Trading Engine.
     */
    private TradingEngine() {

        // the sequence order of these methods is significant - don't change it.
        loadExchangeAdapterConfig();
        loadEngineConfig();
        loadTradingStrategyConfig();
        loadMarketConfigAndInitialiseTradingStrategies();

        emailAlerter = EmailAlerter.getInstance();
    }

    /*
     * Returns a new instance of the Trading Engine.
     */
    public static TradingEngine newInstance() {
        return new TradingEngine();
    }

    /*
     * Starts the Trading Engine.
     */
    public void start() throws IllegalStateException {

        synchronized(IS_RUNNING_MONITOR) {
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

        LogUtils.log(LOG, Level.INFO, () -> "Starting Trading Engine...");
        runMainControlLoop();
}

    /*
     * The main control loop.
     * We loop infinitely unless an unexpected exception occurs.
     * The code fails hard and fast if an unexpected occurs. Network exceptions *should* recover.
     */
    private void runMainControlLoop() {

        while (keepAlive) {

            try {

                LogUtils.log(LOG, Level.INFO, () -> "");
                LogUtils.log(LOG, Level.INFO, () -> "*** Starting next trade cycle... ***");

                // Emergency Stop Check MUST run at start of every trade cycle.
                if (isEmergencyStopLimitBreached()) {
                    break;
                }

                // Execute the Trading Strategies
                for (final TradingStrategy tradingStrategy : tradingStrategiesToExecute) {
                    LogUtils.log(LOG, Level.INFO, () -> "Executing Trading Strategy ---> " + tradingStrategy.getClass().getSimpleName());
                    tradingStrategy.execute();
                }

                // Sleep until next trade cycle...
                LogUtils.log(LOG, Level.INFO, () -> "*** Sleeping " + tradeExecutionInterval + "s til next trade cycle... ***");

                try {
                    Thread.sleep(tradeExecutionInterval * 1000);
                } catch (InterruptedException e) {
                    LOG.warn("Control Loop thread interrupted when sleeping before next trade cycle");
                    Thread.currentThread().interrupt();
                }

            } catch (ExchangeTimeoutException e) {

                /*
                 * We have a network connection issue reported by Exchange Adapter when called directly from
                 * Trading Engine.
                 * Current policy is to log it and sleep until next trade cycle.
                 */
                final String WARNING_MSG = "A network error has occurred in Exchange Adapter! " +
                        "BX-bot will attempt next trade in " + tradeExecutionInterval + "s...";
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

        LOG.fatal("BX-bot is shutting down NOW!");
        synchronized(IS_RUNNING_MONITOR) {
            isRunning = false;
        }
    }

    /*
     * Shutdown the Trading Engine.
     * Might be called from a different thread.
     * TODO currently not used - but will eventually be called from Admin Console.
     */
    public void shutdown() {

        LogUtils.log(LOG, Level.INFO, () -> "Shutdown request received!");
        LogUtils.log(LOG, Level.INFO, () -> "Engine originally started in thread: " + engineThread);

        keepAlive = false;
        engineThread.interrupt(); // poke it in case bot is sleeping
    }

    /*
     * Returns true if the Trading Engine is running, false otherwise.
     */
    public synchronized boolean isRunning() {
        LogUtils.log(LOG, Level.INFO, () -> "isRunning: " + isRunning);
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
    private boolean isEmergencyStopLimitBreached() throws TradingApiException, ExchangeTimeoutException {

        boolean isEmergencyStopLimitBreached = true;

        LogUtils.log(LOG, Level.INFO, () -> "Performing Emergency Stop check...");

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

            LogUtils.log(LOG, Level.INFO, () -> "Emergency Stop Currency balance available on exchange is ["
                        + new DecimalFormat("#.########").format(currentBalance) + "] "
                        + emergencyStopCurrency);

            LogUtils.log(LOG, Level.INFO, () -> "Balance that will stop ALL trading across ALL markets is ["
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
                LogUtils.log(LOG, Level.INFO, () -> "Emergency Stop check PASSED!");
            }
        }
        return isEmergencyStopLimitBreached;
    }

    /*
     * Builds and formats the Email Alert message content in plain text.
     */
    private String buildCriticalEmailAlertMsgContent(String errorDetails, Throwable exception) {

        final StringBuilder msgContent = new StringBuilder("A CRITICAL error event has occurred on BX-bot.");
        msgContent.append(NEWLINE);
        msgContent.append(NEWLINE);

        msgContent.append(HORIZONTAL_RULE);
        msgContent.append("Exchange Adapter:");
        msgContent.append(NEWLINE);
        msgContent.append(NEWLINE);
        msgContent.append(exchangeAdapter.getClass().getName());
        msgContent.append(NEWLINE);
        msgContent.append(NEWLINE);

        msgContent.append(HORIZONTAL_RULE);
        msgContent.append("Event Time:");
        msgContent.append(NEWLINE);
        msgContent.append(NEWLINE);
        msgContent.append(new Date());
        msgContent.append(NEWLINE);
        msgContent.append(NEWLINE);

        msgContent.append(HORIZONTAL_RULE);
        msgContent.append("Event Details:");
        msgContent.append(NEWLINE);
        msgContent.append(NEWLINE);
        msgContent.append(errorDetails);
        msgContent.append(NEWLINE);
        msgContent.append(NEWLINE);

        msgContent.append(HORIZONTAL_RULE);
        msgContent.append("Action Taken:");
        msgContent.append(NEWLINE);
        msgContent.append(NEWLINE);
        msgContent.append("The bot will shutdown NOW! Check the bot logs for more information.");
        msgContent.append(NEWLINE);
        msgContent.append(NEWLINE);

        if (exception != null) {
            msgContent.append(HORIZONTAL_RULE);
            msgContent.append("Stacktrace:");
            msgContent.append(NEWLINE);
            msgContent.append(NEWLINE);
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

    /*
     * Loads the Exchange Adapter config so the bot knows *where* to trade.
     */
    private void loadExchangeAdapterConfig() {

        final ExchangeType exchangeType = ConfigurationManager.loadConfig(ExchangeType.class,
                EXCHANGE_CONFIG_XML_FILENAME, EXCHANGE_CONFIG_XSD_FILENAME);

        exchangeAdapter = ConfigurableComponentFactory.createComponent(exchangeType.getAdapter());
        LogUtils.log(LOG, Level.INFO, () -> "Trading Engine will use Exchange Adapter for: " + exchangeAdapter.getImplName());

        final ExchangeConfigImpl exchangeConfig = new ExchangeConfigImpl();

        // Fetch optional network config
        final NetworkConfigType networkConfigType = exchangeType.getNetworkConfig();
        if (networkConfigType != null) {

            final NetworkConfigImpl networkConfig = new NetworkConfigImpl();
            networkConfig.setConnectionTimeout(networkConfigType.getConnectionTimeout());

            // Grab optional non-fatal error codes
            final NonFatalErrorCodesType nonFatalErrorCodesType = networkConfigType.getNonFatalErrorCodes();
            if (nonFatalErrorCodesType != null) {
                networkConfig.setNonFatalErrorCodes(nonFatalErrorCodesType.getCodes());
            } else {
                LogUtils.log(LOG, Level.INFO, () ->
                        "No (optional) NetworkConfiguration NonFatalErrorCodes have been set for Exchange Adapter: "
                                + exchangeAdapter.getImplName());
            }

            // Grab optional non-fatal error messages
            final NonFatalErrorMessagesType nonFatalErrorMessagesType = networkConfigType.getNonFatalErrorMessages();
            if (nonFatalErrorMessagesType != null) {
                networkConfig.setNonFatalErrorMessages(nonFatalErrorMessagesType.getMessages());
            } else {
                LogUtils.log(LOG, Level.INFO, () ->
                        "No (optional) NetworkConfiguration NonFatalErrorMessages have been set for Exchange Adapter: "
                                + exchangeAdapter.getImplName());
            }

            exchangeConfig.setNetworkConfig(networkConfig);
            LogUtils.log(LOG, Level.INFO, () ->
                    "NetworkConfiguration has been set: " + exchangeConfig.getNetworkConfig());

        } else {
            LogUtils.log(LOG, Level.INFO, () ->
                    "No (optional) NetworkConfiguration has been set for Exchange Adapter: " + exchangeAdapter.getImplName());
        }

        // Fetch optional authentication config
        final AuthenticationConfigType authenticationConfigType = exchangeType.getAuthenticationConfig();
        if (authenticationConfigType != null) {

            final AuthenticationConfigImpl authenticationConfig = new AuthenticationConfigImpl();

            final List<com.gazbert.bxbot.core.config.exchange.generated.ConfigItemType> configItems =
                    authenticationConfigType.getConfigItems();

            for (final com.gazbert.bxbot.core.config.exchange.generated.ConfigItemType configItem : configItems) {
                authenticationConfig.addItem(configItem.getName(), configItem.getValue());
            }

            exchangeConfig.setAuthenticationConfig(authenticationConfig);
            LogUtils.log(LOG, Level.INFO, () ->
                    "AuthenticationConfiguration has been set: " + exchangeConfig.getAuthenticationConfig());

        } else {
            LogUtils.log(LOG, Level.INFO, () ->
                    "No (optional) AuthenticationConfiguration has been set for Exchange Adapter: " + exchangeAdapter.getImplName());
        }

        // Fetch optional 'other' config
        final OtherConfigType otherConfigType = exchangeType.getOtherConfig();
        if (otherConfigType != null) {

            final OtherConfigImpl otherConfig = new OtherConfigImpl();

            final List<com.gazbert.bxbot.core.config.exchange.generated.ConfigItemType> configItems =
                    otherConfigType.getConfigItems();

            for (final com.gazbert.bxbot.core.config.exchange.generated.ConfigItemType configItem : configItems) {
                otherConfig.addItem(configItem.getName(), configItem.getValue());
            }

            exchangeConfig.setOtherConfig(otherConfig);
            LogUtils.log(LOG, Level.INFO, () ->
                    "OtherConfiguration has been set: " + exchangeConfig.getOtherConfig());

        } else {
            LogUtils.log(LOG, Level.INFO, () ->
                    "No (optional) OtherConfiguration has been set for Exchange Adapter: " + exchangeAdapter.getImplName());
        }

        // Finally initialise the adapter
        exchangeAdapter.init(exchangeConfig);
    }

    /*
     * Loads the Trading Engine config so the bot knows *when* to trade.
     * This config also sets up the Emergency Stop shutdown behaviour.
     */
    private void loadEngineConfig() {

        final EngineType engineConfig = ConfigurationManager.loadConfig(EngineType.class,
                ENGINE_CONFIG_XML_FILENAME, ENGINE_CONFIG_XSD_FILENAME);

        tradeExecutionInterval = engineConfig.getTradeCycleInterval();
        LogUtils.log(LOG, Level.INFO, () -> "Trade Execution Cycle Interval in secs: " + tradeExecutionInterval);

        emergencyStopCurrency = engineConfig.getEmergencyStopCurrency();
        LogUtils.log(LOG, Level.INFO, () -> "Emergency Stop Currency is: " + emergencyStopCurrency);

        emergencyStopBalance = engineConfig.getEmergencyStopBalance();
        LogUtils.log(LOG, Level.INFO, () -> "Emergency Stop Balance is: " + emergencyStopBalance);
    }

    /*
     * Loads Trading Strategy descriptions from config so the bot knows *how* to trade.
     */
    private void loadTradingStrategyConfig() {
        final TradingStrategiesType strategiesConfig = ConfigurationManager.loadConfig(TradingStrategiesType.class,
                STRATEGIES_CONFIG_XML_FILENAME, STRATEGIES_CONFIG_XSD_FILENAME);
        final List<StrategyType> strategies = strategiesConfig.getStrategies();

        // Load em all up
        for (final StrategyType strategy : strategies) {
            strategyDescriptions.put(strategy.getId(), strategy);
            LogUtils.log(LOG, Level.INFO, () -> "Registered Trading Strategy with Trading Engine - ID: " + strategy.getId());
        }
    }

    /*
     * Loads the Market configuration so the bot knows *what* to trade.
     * Initialises the Trading Strategies ready for the engine to execute them.
     */
    private void loadMarketConfigAndInitialiseTradingStrategies() {

        final MarketsType marketConfiguration = ConfigurationManager.loadConfig(MarketsType.class,
                MARKETS_CONFIG_XML_FILENAME, MARKETS_CONFIG_XSD_FILENAME);
        final List<MarketType> markets = marketConfiguration.getMarkets();

        LogUtils.log(LOG, Level.INFO, () -> "Processing Markets from config...");

        // used only as crude mechanism for checking for duplicate Markets
        final Set<Market> loadedMarkets = new HashSet<>();

        // Load em up and create the Strategies
        for (final MarketType marketType : markets) {
            final String marketName = marketType.getLabel();
            LogUtils.log(LOG, Level.INFO, () -> "Market Name: " + marketName);

            final String marketId = marketType.getId();
            LogUtils.log(LOG, Level.INFO, () -> "Market Id: " + marketId);

            final String baseCurrency = marketType.getBaseCurrency();
            LogUtils.log(LOG, Level.INFO, () -> "Market Base Currency code: " + baseCurrency);

            final String counterCurrency = marketType.getCounterCurrency();
            LogUtils.log(LOG, Level.INFO, () -> "Market Counter Currency code: " + counterCurrency);

            final boolean isMarketEnabled = marketType.isEnabled();
            LogUtils.log(LOG, Level.INFO, () -> "Is Market Enabled: " + isMarketEnabled);

            if (!isMarketEnabled) {
                LogUtils.log(LOG, Level.INFO, () -> marketName + " market is NOT enabled for trading - skipping to next market...");
                continue;
            }

            final Market market = new Market(marketName, marketId, baseCurrency, counterCurrency);
            final boolean wasAdded = loadedMarkets.add(market);
            if (!wasAdded) {
                final String errorMsg = "Found duplicate Market in " + MARKETS_CONFIG_XML_FILENAME + " config file. " +
                        "Market details: " + market;
                LOG.fatal(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // Get the strategy to use for this Market
            final String strategyToUse = marketType.getTradingStrategy();
            LogUtils.log(LOG, Level.INFO, () -> "Market Trading Strategy Id: " + strategyToUse);

            if (strategyDescriptions.containsKey(strategyToUse)) {
                final StrategyType tradingStrategy = strategyDescriptions.get(strategyToUse);
                final String tradingStrategyClassname = tradingStrategy.getClassName();

                // Grab optional config for the Trading Strategy
                final StrategyConfigImpl strategyConfig = new StrategyConfigImpl();
                final ConfigurationType config = tradingStrategy.getConfiguration();
                if (config != null) {
                    final List<ConfigItemType> configItems = config.getConfigItem();
                    for (final ConfigItemType configItem : configItems) {
                        strategyConfig.addConfigItem(configItem.getName(), configItem.getValue());
                    }
                } else {
                    LogUtils.log(LOG, Level.INFO, () -> "No (optional) configuration has been set for Trading Strategy: " + strategyToUse);
                }

                LogUtils.log(LOG, Level.INFO, () -> "StrategyConfig (optional): " + strategyConfig);

                /*
                 * Load the Trading Strategy impl, instantiate it, set its config, and store in the cached
                 * Trading Strategy execution list.
                 */
                final TradingStrategy strategyImpl = ConfigurableComponentFactory.createComponent(tradingStrategyClassname);
                strategyImpl.init(exchangeAdapter, market, strategyConfig);

                LogUtils.log(LOG, Level.INFO, () -> "Initialized trading strategy successfully. Name: [" + tradingStrategy.getLabel()
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

        LogUtils.log(LOG, Level.INFO, () -> "Loaded and set Market configuration successfully!");
    }
}