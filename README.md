
# BX-bot

## What is BX-bot?
BX-bot (_Bex_) is a simple Java algorithmic trading bot for trading [Bitcoin](https://bitcoin.org) on 
cryptocurrency [exchanges](https://bitcoinwisdom.com/).

The project contains the basic infrastructure to trade on a [cryptocurrency](http://coinmarketcap.com/) exchange...
except for the trading strategies - you'll need to write these yourself. A simple example
[scalping strategy](http://www.investopedia.com/articles/trading/02/081902.asp) is included to get you started with the
Trading API - take a look [here](http://www.investopedia.com/articles/active-trading/101014/basics-algorithmic-trading-concepts-and-examples.asp)
for more ideas.

Exchange Adapters for using [BTC-e](https://btc-e.com), [Bitstamp](https://www.bitstamp.net), 
[Bitfinex](https://www.bitfinex.com), [OKCoin](https://www.okcoin.com/), [Huobi](https://www.huobi.com/), 
[GDAX](https://www.gdax.com/), [itBit](https://www.itbit.com/), [Kraken](https://www.kraken.com), and [Gemini](https://gemini.com/) 
are included. Feel free to improve these or contribute new adapters to the project, that would be shiny.

The Trading API provides support for [limit orders](http://www.investopedia.com/terms/l/limitorder.asp)
traded at the [spot price](http://www.investopedia.com/terms/s/spotprice.asp);
it does not support [futures](http://www.investopedia.com/university/beginners-guide-to-trading-futures/) or 
[margin](http://www.investopedia.com/university/margin/) trading... yet.
 
**Warning:** Trading Bitcoin carries significant financial risk; you could lose money. This software is provided 'as is'
and released under the [MIT license](http://opensource.org/licenses/MIT).

## Architecture
![bxbot-core-architecture.png](https://github.com/gazbert/BX-bot/blob/master/docs/bxbot-core-architecture.png)

- Trading Engine - the execution unit. It provides a framework for integrating and executing Exchange Adapters and Trading Strategies.
- Exchange Adapters - the data stream unit. They provide access to a given exchange.
- Trading Strategies - the decision or strategy unit. This is where the trading decisions happen.
- Trading API - Trading Strategies use this API to make trades. Exchange Adapters implement this to provide access
  to a given exchange.
- Strategy API - Trading Strategies implement this for the Trading Engine to execute them.
 
Trading Strategies and Exchange Adapters are injected by the Trading Engine on startup. The bot uses a crude XML based
dependency injection framework to achieve this; the long term goal is to run it as a [Spring Boot](http://projects.spring.io/spring-boot/)
app in a [microservice](http://martinfowler.com/articles/microservices.html) system.

The bot was designed to fail hard and fast if any unexpected errors occur in the Exchange Adapters or Trading Strategies:
it will log the error, send an email alert (if configured), and then shutdown.

The first release of BX-bot is single-threaded for simplicity; I am working on a concurrent version.

## Dependencies
BX-bot requires [Oracle JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html) for the
development and runtime environment.

You'll also need [Maven 3](https://maven.apache.org) installed in order to build the bot and pull in the dependencies;
BX-bot depends on [log4j](http://logging.apache.org/log4j), [JavaMail](https://java.net/projects/javamail/pages/Home),
[Google Gson](https://code.google.com/p/google-gson/), [Google Guava](https://github.com/google/guava), and 
[Spring Boot](http://projects.spring.io/spring-boot/).
See the [`pom.xml`](https://github.com/gazbert/BX-bot/blob/master/pom.xml) for details.

## Testing
[![Build Status](https://travis-ci.org/gazbert/BX-bot.svg?branch=master)](https://travis-ci.org/gazbert/BX-bot)

The bot has undergone basic unit testing on a _best-effort_ basis - there is a continuous integration build 
running on [Travis CI](https://travis-ci.org/).

The latest stable build can always be found on the [Releases](https://github.com/gazbert/BX-bot/releases) page. The SNAPSHOT builds are
active development builds and very much ["sid"](https://www.debian.org/releases/sid/) - the tests should always pass,
but you might not have a working bot!

## Issue & Change Management
Issues and new features will be managed using the project [Issue Tracker](https://github.com/gazbert/BX-bot/issues) -
submit bugs here.
 
You are welcome to take on new features or fix bugs!

## User Guide

### Configuration
The bot provides a simple plugin framework for:

* Exchanges to integrate with.
* Markets to trade on.
* Trading Strategies to execute.

It uses XML configuration files. These live in the [`config`](https://github.com/gazbert/BX-bot/tree/master/config) folder.

Config changes are only applied at startup; they are _not_ hot.

All configuration elements are mandatory unless specified otherwise.

Sample configurations for running on different exchanges can be found in the 
[`config/samples`](https://github.com/gazbert/BX-bot/tree/master/config/samples)folder.

##### Exchange Adapters
You specify the Exchange Adapter you want BX-bot to use in the 
[`exchange.xml`](https://github.com/gazbert/BX-bot/blob/master/config/exchange.xml) file. 

```xml
<exchange>
    <name>BTC-e</name>
    <adapter>com.gazbert.bxbot.core.exchanges.BtceExchangeAdapter</adapter>
    <authentication-config>
        <config-item>
            <name>key</name>
            <value>your-api-key</value>
        </config-item>
        <config-item>
            <name>secret</name>
            <value>your-secret-key</value>
        </config-item>
    </authentication-config>
    <network-config>
        <connection-timeout>30</connection-timeout>
        <non-fatal-error-codes>
            <code>502</code>
            <code>503</code>
        </non-fatal-error-codes>
        <non-fatal-error-messages>
            <message>Connection reset</message>
            <message>Connection refused</message>
        </non-fatal-error-messages>
    </network-config>
    <other-config>
        <config-item>
            <name>buy-fee</name>
            <value>0.5</value>
        </config-item>
        <config-item>
            <name>sell-fee</name>
            <value>0.5</value>
        </config-item>
    </other-config>
</exchange>
```

All elements are mandatory unless stated otherwise.

The `<name>` value is for descriptive use only. It is used in the log statements.

For the `<adapter>` value, you must specify the fully qualified name of the Exchange Adapter class for the Trading Engine
to inject on startup. The class must be on the runtime classpath. See the _How do I write my own Exchange Adapter?_ 
section for more details.

The `<authentication-config>` section is optional. If present, at least 1 `<config-item>` must be set - these are repeating
key/value String pairs. This section is used by the inbuilt Exchange Adapters to configure their exchange trading API credentials - see
the sample `exchange.xml` config files for details.

The `<network-config>` section is optional. If present, the `<connection-timeout>`, `<non-fatal-error-codes>`, and
`<non-fatal-error-messages>` sections must be set. This section is used by the inbuilt Exchange Adapters to set
their network configuration as detailed below:

* The `<connection-timeout>` is the timeout value that the exchange adapter will wait on socket connect/socket read when
communicating with the exchange. Once this threshold has been breached, the exchange adapter will give up and throw an
[`ExchangeNetworkException`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/ExchangeNetworkException.java).
The sample Exchange Adapters are single threaded: if a request gets blocked, it will block all subsequent requests from
getting to the exchange. This timeout value prevents an indefinite block.

* The `<non-fatal-error-codes>` section contains a list of HTTP status codes that will trigger the adapter to throw a
non-fatal 'ExchangeNetworkException'.
This allows the bot to recover from temporary network issues. See the sample `exchange.xml` config files for status codes to use.

* The `<non-fatal-error-messages>` section contains a list of java.io exception messages that will trigger the adapter to
throw a non-fatal `ExchangeNetworkException`.
This allows the bot to recover from temporary network issues. See the sample `exchange.xml` config files for messages to use.

The `<other-config>` section is optional. If present, at least 1 `<config-item>` must be set - these are repeating
key/value String pairs. This section is used by the inbuilt Exchange Adapters to configure any additional config,
e.g. buy/sell fees.

BX-bot only supports 1 Exchange Adapter for each instance of the bot; you will need to create multiple (runtime) 
instances of the bot to run against different exchanges.

##### Markets
You specify which markets you want to trade on in the 
[`markets.xml`](https://github.com/gazbert/BX-bot/blob/master/config/markets.xml) file.

```xml
<markets>      
    <market>
        <label>BTC/USD</label>
        <id>btc_usd</id>
        <base-currency>BTC</base-currency>
        <counter-currency>USD</counter-currency>
        <enabled>true</enabled>
        <trading-strategy>scalping-strategy</trading-strategy>
    </market>
    <market>
        <label>LTC/BTC</label>
        <id>ltc_usd</id>
        <base-currency>LTC</base-currency>
        <counter-currency>BTC</counter-currency>
        <enabled>false</enabled>
        <trading-strategy>scalping-strategy</trading-strategy>
    </market>        
</markets>
```

All elements are mandatory unless stated otherwise.

The `<label>` value is for descriptive use only. It is used in the log statements.

The `<id>` value is the market id as defined on the exchange. E.g the BTC-e BTC/USD market id is btc_usd - see https://btc-e.com/api/3/docs

The `<base-currency>` value is the currency short code for the base currency in the currency pair. When you buy or sell a
currency pair, you are performing that action on the base currency. The base currency is the commodity you are buying or
selling. E.g. in a BTC/USD market, the first currency (BTC) is the base currency and the second currency (USD) is the
counter currency.

The `<counter-currency>` value is the currency short code for the counter currency in the currency pair. This is also known
as the quote currency.

The `<enabled>` value allows you to toggle trading on the market. Remember, config changes are only applied on startup.

The `<trading-strategy>` value _must_ match a strategy `<id>` defined in your `strategies.xml` config.
Currently, BX-bot only supports 1 `<trading-strategy>` per `<market>`.

##### Strategies #####
You specify the Trading Strategies you wish to use in the 
[`strategies.xml`](https://github.com/gazbert/BX-bot/blob/master/config/strategies.xml) file.

```xml
<trading-strategies>
    <strategy>
        <id>scalping-strategy</id>
        <label>Basic Scalping Strat</label>
        <description>A simple trend following scalper that buys at current BID price and sells at current
         ASK price, taking profit from the spread. The exchange fees are factored in.</description>
        <class-name>com.gazbert.bxbot.core.strategies.ExampleScalpingStrategy</class-name>
        <configuration>
            <config-item>
                <name>btc-buy-order-amount</name>
                <value>0.5</value>
            </config-item>
            <config-item>
                <name>minimumPercentageGain</name>
                <value>1</value>
            </config-item>
        </configuration>
    </strategy>
    <strategy>
        <id>macd-strategy</id>
        <label>MACD Based Strat</label>
        <description>Strat uses MACD data to take long position in USD.</description>
        <class-name>com.gazbert.bxbot.core.strategies.YourMacdStrategy</class-name>
        <configuration>
            <config-item>
                <name>btc-buy-order-amount</name>
                <value>0.5</value>
            </config-item>
            <config-item>
                <name>shortEmaInterval</name>
                <value>12</value>
            </config-item>
            <config-item>
                <name>longEmaInterval</name>
                <value>26</value>
            </config-item>
        </configuration>
    </strategy>
</trading-strategies>
```

All elements are mandatory unless stated otherwise.

The `<id>` value must be unique. The markets.xml `<market><trading-strategy>` entries cross-reference this.

The `<label>` value is for descriptive use only. It is used in the log statements.

The `<description>` value is optional and not used anywhere yet; a new Web UI will in the future.

For the `<class-name>` value, you must specify the fully qualified name of your Trading Strategy class for the
Trading Engine to inject on startup. The class _must_ be on the runtime classpath.

The `<configuration>` section is optional. It allows you to set custom key/value pair String config - this is passed
to your Trading Strategy when the bot starts up; see the _How do I write my own Trading Strategy?_ section.

##### Engine
The [`engine.xml`](https://github.com/gazbert/BX-bot/blob/master/config/engine.xml) file is used to configure the Trading Engine.

```xml
<engine>
    <emergency-stop-currency>BTC</emergency-stop-currency>
    <emergency-stop-balance>1.0</emergency-stop-balance>
    <trade-cycle-interval>60</trade-cycle-interval>
</engine>
```

All elements are mandatory.

The `<emergency-stop-currency>` value must be set to prevent catastrophic loss on the exchange. 
This is normally the currency you intend to hold a long position in. It should be set to the currency short code for the
wallet, e.g. BTC, LTC, USD. This value can be case sensitive for some exchanges - check the Exchange Adapter documentation.

The `<emergency-stop-balance>` value must be set to prevent catastrophic loss on the exchange. 
The Trading Engine checks this value at the start of every trade cycle: if your `<emergency-stop-currency>` wallet balance on
the exchange drops below this value, the Trading Engine will log it, send an Email Alert (if configured) and then shutdown.

The `<trade-cycle-interval>` value is the interval in _seconds_ that the Trading Engine will wait/sleep before executing
each trade cycle. The minimum value is 1 second. Some exchanges allow you to hit them harder than others. However, while
their API documentation might say one thing, the reality is you might get socket timeouts and 5xx responses if you hit it
too hard - you cannot perform [low latency]("https://en.wikipedia.org/wiki/Low_latency_(capital_markets)") trading over 
the public internet ;-) I might have EMA/MACD strats running every 5 mins and 'scalping'
strats running every 60s on BTC-e. You'll need to experiment with the trade cycle interval for different exchanges.

##### Email Alerts
You specify the Email Alerts config in the 
[`email-alerts.xml`](https://github.com/gazbert/BX-bot/blob/master/config/email-alerts.xml) file.

```xml
<email-alerts>
    <enabled>false</enabled>
    <smtp-config>
        <smtp-host>smtp.gmail.com</smtp-host>
        <smtp-tls-port>587</smtp-tls-port>
        <account-username>your.account.username@gmail.com</account-username>
        <account-password>your.account.password</account-password>
        <from-addr>from.addr@gmail.com</from-addr>
        <to-addr>to.addr@gmail.com</to-addr>
    </smtp-config>
</email-alerts>
```

This config is used to send email alerts when the bot is forced to shutdown due to an unexpected error occurring in the 
Trading Strategies or Exchange Adapters.

All elements are mandatory unless stated otherwise.

If `<enabled>` is set to 'true', the bot will load the optional `<smtp-config>` config. If `<enabled>` is set to 'false',
you can omit the `<smtp-config>` config.

If enabled, the bot will send email alerts to the `<to-addr>` if it needs to shutdown due to a critical error.

Sample SMTP config for using a Gmail account is shown above.

The email is sent using TLS.

#### Logging
Logging for the bot is provided by [log4j](http://logging.apache.org/log4j). The log file is written to `logs/bxbot.log` 
uses a rolling policy. It will create up to 7 archives on the same day (1-7) that are stored in a directory based on 
the current year and month, and will compress each archive using gzip. Once a file reaches 100 MB or a new day is started,
it is archived and a new log file is created. Only the last 90 archives are kept. The logging level is set at `info`. 
You can change this default logging configuration in the [`resources/log4j2.xml`](https://github.com/gazbert/BX-bot/blob/master/resources/log4j2.xml) file.

I recommend running at `info` level, as `debug` level logging will produce a *lot* of
output from the Exchange Adapters; it's very handy for debugging, but not so good for your disk space!

### How do I write my own Trading Strategy?
_"Battle not with monsters, lest ye become a monster, and if you gaze into the abyss, the abyss gazes also into you."_ - Friedrich Nietzsche

The best place to start is with the sample Trading Strategy provided - see the latest 
[`BasicScalpingExampleStrategy`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/strategies/ExampleScalpingStrategy.java).
More information can be found
[here](http://www.investopedia.com/articles/active-trading/101014/basics-algorithmic-trading-concepts-and-examples.asp).

Your strategy must implement the [`TradingStrategy`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/strategy/TradingStrategy.java)
interface. This allows the Trading Engine to:

* inject your strategy on startup of the bot.
* pass any configuration (you set up in the `strategies.xml`) to your strategy.
* invoke your strategy during each trade cycle.

The Trading Engine will only send 1 thread through your Trading Strategy; you do not have to code for concurrency.

The project Javadoc will be useful too. It can be found in the `./target/apidocs` folder after running the Maven build -
see the _Build Guide_ section.

##### Making Trades
You use the [`TradingApi`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApi.java)
to make trades etc. The API is passed to your Trading Strategy implementation `init` method when the bot starts up. 
See the Javadoc for full details of the API.

##### Error Handling
Your Trading Strategy implementation should throw a [`StrategyException`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/strategy/StrategyException.java)
whenever it 'breaks'. BX-bot's error handling policy is designed to fail hard and fast; it will log the error, send an
Email Alert (if configured), and shutdown.

Note that the inbuilt Exchange Adapters will (some more often than others!) throw an
[`ExchangeNetworkException`](httpflog4s://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/ExchangeNetworkException.java)
if they encounter network issues connecting with the exchange. Your strategy should always catch this exception and
choose what to do next, e.g. retry the previous Trading API call, or 'swallow' the exception and wait until the Trading
Engine invokes the strategy again at the next trade cycle.

##### Configuration
You specify the Trading Strategies you wish to use in the `strategies.xml` file - see the main _Configuration_ section 
for full details.

The optional `<configuration>` section in the `strategies.xml` allows you to set key/value pair String config to pass to
your Trading Strategy implementation.

On startup, the Trading Engine will pass the config to your Trading Strategy's `init` method in the `StrategyConfig` arg. 

##### Dependencies
Your Trading Strategy implementation has a compile-time dependency on the [Strategy API](https://github.com/gazbert/BX-bot/tree/master/src/main/java/com/gazbert/bxbot/core/api/strategy)
and the [Trading API](https://github.com/gazbert/BX-bot/tree/master/src/main/java/com/gazbert/bxbot/core/api/trading).

The inbuilt [`BasicScalpingExampleStrategy`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/strategies/ExampleScalpingStrategy.java)
also has a compile-time dependency on log4j and Google Guava.

##### Packaging & Deployment #####
To get going fast, you can code your Trading Strategy and place it in the [com.gazbert.bxbot.core.strategies](https://github.com/gazbert/BX-bot/tree/master/src/main/java/com/gazbert/bxbot/core/strategies)
package alongside the example strategy. When you build the project, your Trading Strategy will be included in the BX-bot jar. 
You can also create your own jar for your strats, e.g. `my-strats.jar`, and include it on BX-bot's runtime classpath -
see the _Installation Guide_ for how to do this.

### How do I write my own Exchange Adapter?
The best place to start is with one of the sample Exchange Adapters provided - see the latest [`BitstampExchangeAdapter`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/exchanges/BitstampExchangeAdapter.java)
for example.

Your adapter must implement the [`TradingApi`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApi.java)
and the [`ExchangeAdapter`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/exchange/ExchangeAdapter.java)
interfaces. This allows for:

* the main Trading Engine to inject your adapter on startup and initialise it with config from the `exchange.xml` file.
* the Trading Strategies to invoke your adapter's implementation of the `TradingApi` during each trade cycle.

[`AbstractExchangeAdapter`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/exchanges/AbstractExchangeAdapter.java)
is a handy base class that all the inbuilt Exchange Adapters extend - it could be useful.

The Trading Engine will only send 1 thread through your Exchange Adapter; you do not have to code for concurrency.

The project Javadoc will be useful too. It can be found in the `./target/apidocs` folder after running the Maven build -
see the _Build Guide_ section.

##### Error Handling
Your Exchange Adapter implementation should throw a [`TradingApiException`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApiException.java)
whenever it breaks; the Trading Strategies should catch this and decide how they want to proceed.

The Trading API provides an [`ExchangeNetworkException`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/ExchangeNetworkException.java)
for adapters to throw when they cannot connect to the exchange to make Trading API calls. This allows for
Trading Strategies to recover from temporary network failures. The `exchange.xml` config file has an optional `<network-config>`
section, which contains `<non-fatal-error-codes>` and `<non-fatal-error-messages>` elements - these can be used to tell the
adapter when to throw the exception.

The first release of the bot is _single-threaded_ for simplicity. The downside to this is that if an API call to the 
exchange gets blocked on IO, BX-bot will get stuck until your Exchange Adapter frees the block. The Trading API provides
an `ExchangeNetworkException` for your adapter to throw if it times-out connecting to the exchange. It is your responsibility to free up any blocked
connections - see the [`AbstractExchangeAdapter`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/exchanges/AbstractExchangeAdapter.java)
for an example how to do this.

The Trading Engine will also call your adapter directly when performing the _Emergency Stop_ check to see if your 
`<emergency-stop-currency>` wallet balance on the exchange drops below the configured `<emergency-stop-value>` value. If this call to the
[`TradingApi`](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApi.java)
`getBalanceInfo()` fails and is not due to a `TradingApiException`, the Trading Engine will log the error, send an 
Email Alert (if configured), and shutdown. If the API call failed due to an `ExchangeNetworkException`, the 
Trading Engine will log the error and sleep until the next trade cycle.

##### Configuration
You provide your Exchange Adapter details in the `exchange.xml` file - see the main _Configuration_ section for full details.

##### Dependencies
Your Exchange Adapter implementation has a compile-time dependency on the [Trading API](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApi.java).

The inbuilt Exchange Adapters also have compile-time dependencies on log4j, Google Gson, and Google Guava.

##### Packaging & Deployment
To get going fast, you can code your Exchange Adapter and place it in the [com.gazbert.bxbot.core.exchanges](https://github.com/gazbert/BX-bot/tree/master/src/main/java/com/gazbert/bxbot/core/exchanges)
package alongside the other inbuilt adapters. When you build the project, your Exchange Adapter will be included in the BX-bot jar. 
You can also create your own jar for your adapters, e.g. `my-adapters.jar`, and include it on BX-bot's runtime classpath -
see the _Installation Guide_ for how to do this.

## Build Guide

A Maven `pom.xml` is included for building the bot.

1. Clone the BX-bot repo locally - the [Releases](https://github.com/gazbert/BX-bot/releases) page has the stable builds.
1. Open the `./config` XML files and configure them as required.
1. If you plan on using Trading Strategies or Exchange Adapters that are packaged in separate jar files, you'll need to add
   the dependency in the `pom.xml` file - see the commented out dependency examples inside it.
1. Run `mvn assembly:assembly` to build the bot and produce the distribution artifacts `bxbot-core-<version>-dist.tar.gz`
   and `bxbot-core-<version>-dist.zip`. Take a look at the Javadoc in the `./target/apidocs` folder after running this.

## Installation Guide

1. Prerequisite: [Oracle JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html) needs to be installed
   on the machine you want to run the bot.  
1. Copy either the `bxbot-core-<version>-dist.tar.gz` or the `bxbot-core-<version>-dist.zip` onto the machine you 
   want to run the bot. Unzip it into a folder of your choice.
1. To start the bot: `./bxbot.sh start` ; to stop the bot: `./bxbot.sh stop`
 
## Coming Soon...
The following features are in the pipeline:

- REST API for configuring the bot.
- Web UI for configuring the bot.
- Admin app - a microservice for administering multiple bots in the cloud.
- Trade Analysis app - a microservice that will feed off trading events sent by the bots.
- Android app for administering bots.

See the main project [Issue Tracker](https://github.com/gazbert/BX-bot/issues) for timescales and progress.


