/**
 *
 *
 * <h2>Exchange Adapters</h2>
 *
 * <p>Trading Strategies will use Exchange Adapters to execute trades on the exchange via the {@link
 * com.gazbert.bxbot.trading.api.TradingApi}.
 *
 * <p>The Trading Engine will initialise Exchange Adapters via the {@link
 * com.gazbert.bxbot.exchange.api.ExchangeAdapter}.
 *
 * <p>You can write your own Exchange Adapters and keep them here. Alternatively, you can package
 * them up in a separate jar and place it on BX-bot's runtime classpath. Your Exchange Adapter must:
 *
 * <ol>
 *   <li>implement the {@link com.gazbert.bxbot.exchange.api.ExchangeAdapter} and {@link
 *       com.gazbert.bxbot.trading.api.TradingApi} interfaces.
 *   <li>be placed on the Trading Engine's runtime classpath: keep it here, or in a separate jar
 *       file.
 *   <li>include a configuration entry in the exchanges.xml file.
 * </ol>
 *
 * <p>See the project README "How do I write my own Exchange Adapter?" section.
 *
 * @author gazbert
 * @since 1.0
 */
package com.gazbert.bxbot.exchanges;
