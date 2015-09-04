/**
 * <h2>Strategy API</h2>
 *
 * <p>
 * Every Trading Strategy must implement the {@link com.gazbert.bxbot.core.api.strategy.TradingStrategy} interface.
 * </p>
 *
 * <p>
 * The Trading Engine and Trading Strategies have a compile-time dependency on this API.
 * </p>
 *
 * TODO Consider moving this into a separate project - Aga and Trading Strategies projects would depend on it.
 *
 * @author gazbert
 */
package com.gazbert.bxbot.core.api.strategy;