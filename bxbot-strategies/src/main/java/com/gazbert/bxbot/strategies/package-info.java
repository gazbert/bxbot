/**
 *
 *
 * <h2>Trading Strategies</h2>
 * <h2>交易策略</h2>
 *
 * <p>You can write your own Trading Strategies and keep them here. Alternatively, you can package
  them up in a separate jar and place it on BX-bot's runtime classpath. Your Trading Strategy must:
 <p>您可以编写自己的交易策略并将其保留在此处。或者，您可以打包
 它们放在一个单独的 jar 中，并将其放在 BX-bot 的运行时类路径中。您的交易策略必须：
 *
 * <ol>
 *   <li>implement the {@link com.gazbert.bxbot.strategy.api.TradingStrategy} interface.
 *   <li>实现 {@link com.gazbert.bxbot.strategy.api.TradingStrategy} 接口。
 *   <li>be placed on the Trading Engine's runtime classpath: keep it here, or in a separate jar  file.
 *   * <li>放置在交易引擎的运行时类路径中：将其保存在此处，或保存在单独的 jar 文件中。
 *
 *   <li>include a configuration entry in the strategies.yaml file.
 *   <li>在 strategy.yaml 文件中包含一个配置条目。
 * </ol>
 *
 * <p>You can pass configuration to your Strategy from the strategies.yaml file - you access it from the {@link
  com.gazbert.bxbot.strategy.api.TradingStrategy#init(com.gazbert.bxbot.trading.api.TradingApi,
  com.gazbert.bxbot.trading.api.Market, com.gazbert.bxbot.strategy.api.StrategyConfig)} method via the StrategyConfigImpl argument.
 <p>您可以从 strategy.yaml 文件将配置传递给您的策略 - 您可以从 {@link 访问它
com.gazbert.bxbot.strategy.api.TradingStrategy#init(com.gazbert.bxbot.trading.api.TradingApi,
com.gazbert.bxbot.trading.api.Market, com.gazbert.bxbot.strategy.api.StrategyConfig)} 方法通过 StrategyConfigImpl 参数。
 *
 * <p>The Trading Engine will only send 1 thread through your strategy code at a time - you do not have to code for concurrency.
 * * <p>交易引擎一次只会通过您的策略代码发送 1 个线程 - 您不必为并发编写代码。
 *
 * <p>See the project README "How do I write my own Trading Strategy?" section.
 * <p>请参阅项目自述文件“我如何编写自己的交易策略？”部分。
 *
 * @author gazbert
 */
package com.gazbert.bxbot.strategies;
