/**
 * <h2>Trading Strategies</h2>
 *
 * <p>
 * This is the Trading Strategy subsystem.
 * </p>
 *
 * <p>
 * You can write your own Trading Strategies and keep them here. Alternatively, you can package them up in a
 * separate jar and place it on BX-bot's runtime classpath.
 * </p>
 *
 * Your Trading Strategy must:
 * <ol>
 * <li>implement the {@link com.gazbert.bxbot.core.api.strategy.TradingStrategy} interface.
 * <li>be placed on the Trading Engine's runtime classpath: keep it here, or in a separate jar file.</li>
 * <li>include a configuration entry in the ./config/strategies.xml file.</li>
 * </ol>
 *
 * <p>
 * You can pass configuration to your Strategy from the ./config/strategies.xml file - you access it from the
 * {@link com.gazbert.bxbot.core.api.strategy.TradingStrategy#init(com.gazbert.bxbot.core.api.trading.TradingApi, com.gazbert.bxbot.core.api.trading.Market, com.gazbert.bxbot.core.api.strategy.StrategyConfig)}
 * method via the StrategyConfig argument.
 * </p>
 *
 * <p>
 * The Trading Engine will only send 1 thread through your strategy code at a time - you do not have to code for
 * concurrency :-)
 * </p>
 *
 * <p>
 * See the project README "How do I write my own Trading Strategy?" section.
 * </p>
 * 
 * @author gazbert
 */
package com.gazbert.bxbot.core.strategies;