# BX-bot

[![Build Status](https://travis-ci.com/gazbert/bxbot.svg?branch=master)](https://travis-ci.com/gazbert/bxbot)
[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=gazbert_bxbot&metric=alert_status)](https://sonarcloud.io/dashboard?id=gazbert_bxbot)
[![Join the chat at https://gitter.im/BX-bot/Lobby](https://badges.gitter.im/BX-bot/Lobby.svg)](https://gitter.im/BX-bot/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)		 	 
 
## What is BX-bot?

<img src="./docs/bxbot-cropped.png" align="right" width="25%" />

BX-bot (_Bex_) is a simple [Bitcoin](https://bitcoin.org) trading bot written in Java for trading on cryptocurrency 
[exchanges](https://bitcoin.org/en/exchanges).

The project contains the basic infrastructure to trade on a [cryptocurrency](http://coinmarketcap.com/) exchange...
except for the trading strategies - you'll need to write those yourself! A simple 
[example](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java) of a 
[scalping](http://www.investopedia.com/articles/trading/02/081902.asp) strategy is included to get you started with the
Trading API - take a look [here](https://github.com/ta4j/ta4j) for more ideas.

Exchange Adapters for using [Bitstamp](https://www.bitstamp.net), [Bitfinex](https://www.bitfinex.com),
[itBit](https://www.itbit.com/), [Kraken](https://www.kraken.com), [Gemini](https://gemini.com/),
and [Coinbase Pro](https://pro.coinbase.com/) are included.
Feel free to improve these or contribute new adapters to the project; that would be 
[shiny!](https://en.wikipedia.org/wiki/Firefly_(TV_series))

The Trading API provides support for [limit orders](http://www.investopedia.com/terms/l/limitorder.asp)
traded at the [spot price](http://www.investopedia.com/terms/s/spotprice.asp).
If you're looking for something more sophisticated with a much richer Trading API, take a look at
[XChange](https://github.com/knowm/XChange).
 
**Warning:** Trading Bitcoin carries significant financial risk; you could lose money. This software is provided 'as is'
and released under the [MIT license](http://opensource.org/licenses/MIT).

## Architecture

![bxbot-core-architecture.png](./docs/bxbot-core-architecture.png)

- **Trading Engine** - the execution unit. It provides a framework for integrating Exchange Adapters and executing 
                       Trading Strategies.
- **Exchange Adapters** - the data stream unit. They provide access to a given exchange.
- **Trading Strategies** - the decision or strategy unit. This is where the trading decisions happen.
- **Trading API** - Trading Strategies use this API to make trades. Exchange Adapters implement this to provide access
  to a given exchange.
- **Strategy API** - Trading Strategies implement this so the Trading Engine can execute them.
 
Trading Strategies and Exchange Adapters are injected by the Trading Engine on startup. The bot uses a simple 
[YAML](https://en.wikipedia.org/wiki/YAML) backed dependency injection framework to achieve this; the long term goal is
to convert it into a fully configurable [Spring Boot](http://projects.spring.io/spring-boot/) app.

The bot was designed to fail hard and fast if any unexpected errors occur in the Exchange Adapters or Trading Strategies:
it will log the error, send an email alert (if configured), and then shut down.

## Installation Guide
  
The bot runs on Linux, macOS, and Windows.

BX-bot requires a Java 11+ JDK (e.g. [openjdk-11-jdk](http://openjdk.java.net/projects/jdk/11/) or 
[Oracle JDK 11](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html))
to be installed on the machine you are going to use to build and run the bot.

BXBot uses a version of PowerMock that's relies on reflective access in a way that's no longer permitted in Java 11+. 
As workaround, set the following parameter for the JDK compiler: --illegal-access=permit. 

Be mindful of Oracle's recent [licensing changes](https://www.oracle.com/technetwork/java/javase/overview/oracle-jdk-faqs.html)
and how you intend to use the bot.

You can use [Maven](https://maven.apache.org) or [Gradle](https://gradle.org/) to build the bot.
The instructions below are for Linux/macOS, but equivalent Windows scripts are included.

Download the latest [Release](https://github.com/gazbert/bxbot/releases) and unzip the bot.

#### Maven
1. If you plan on using your own Trading Strategies/Exchange Adapters packaged in separate jar files, you'll need to add
   the dependency in the [bxbot-app/pom.xml](./bxbot-app/pom.xml) - see the commented out dependency examples inside it.
1. From the project root, run `./mvnw clean assembly:assembly` to produce the distribution 
   artifacts `bxbot-app-<version>-dist.tar.gz` and `bxbot-app-<version>-dist.zip` in the `./target` folder.
1. Copy either the `bxbot-app-<version>-dist.tar.gz` or the `bxbot-app-<version>-dist.zip` onto the machine you 
   want to run the bot and unzip it someplace.
1. Configure the bot as required - see the main _[Configuration](#configuration)_ section.
   The bot's default configuration uses the 
   [`ExampleScalpingStrategy`](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java), 
   but you'll probably want to [code your own](#how-do-i-write-my-own-trading-strategy)! The 
   [`TestExchangeAdapter`](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges/TestExchangeAdapter.java) is
   configured by default - it makes public API calls to [Bitstamp](https://www.bitstamp.net), but stubs out the private
   API (order management) calls; it's good for testing your initial setup without actually sending orders to the
   exchange.   
1. Usage: `./bxbot.sh [start|stop|status]`   
    
#### Gradle    
1. If you plan on using your own Trading Strategies/Exchange Adapters packaged in separate jar files, you'll need to add
   the dependency in the [bxbot-app/build.gradle](bxbot-app/build.gradle) - see the commented out dependency examples 
   inside it.
1. From the project root, run `./gradlew clean build` to build the bot.   
1. Then run `./gradlew buildTarGzipDist` or `./gradlew buildZipDist` to build the distribution 
   artifact: either `bxbot-app-<version>.tar.gz` or `bxbot-app-<version>.zip` respectively. 
   It will be placed in the `./build/distributions` folder.
1. Copy the artifact onto the machine you want to run the bot and unzip it someplace.
1. Configure the bot as described in step 4 of the previous [Maven](#maven) section.
1. Usage: `./bxbot.sh [start|stop|status]`

### Docker
If you want to just play around with the 
[`ExampleScalpingStrategy`](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java) 
and evaluate the bot, Docker is the way to go.

1. Install [Docker](https://docs.docker.com/engine/installation/) on the machine you want to run the bot.
1. Fetch the BX-bot image from [Docker Hub](https://hub.docker.com/r/gazbert/bxbot/): `docker pull gazbert/bxbot:1.2.0
1. Run the Docker container: `docker container run --publish=8080:8080 --name bxbot-1.2.0 -it gazbert/bxbot:1.2.0 bash`
1. Change into the bot's directory: `cd bxbot*`
1. Configure the bot as described in step 4 of the previous [Maven](#maven) section.
1. Usage: `./bxbot.sh [start|stop|status]`
1. You can detach from the container and leave the bot running using the `CTRL-p` `CTRL-q` key sequence.
1. To re-attach to the Docker container, run `docker container ls` to get the CONTAINER ID. 
   Then run: `docker container attach <CONTAINER ID>`   
   
## Build Guide
If you plan on developing the bot, you'll need JDK 11+ installed on your dev box.

You can use Maven or Gradle to build the bot and pull down the 
dependencies. BX-bot depends on [Spring Boot](http://projects.spring.io/spring-boot/), 
[log4j](http://logging.apache.org/log4j), [JavaMail](https://java.net/projects/javamail/pages/Home), 
[Google Gson](https://code.google.com/p/google-gson/), [Google Guava](https://github.com/google/guava), 
[Snake YAML](https://bitbucket.org/asomov/snakeyaml), [Java JWT](https://github.com/jwtk/jjwt),
[H2](https://www.h2database.com/html/main.html), [JAXB](https://javaee.github.io/jaxb-v2/),
[Jakarta Bean Validation](https://beanvalidation.org/), [Springfox](https://github.com/springfox/springfox),
and [Swagger](https://github.com/swagger-api/swagger-core).

The instructions below are for Linux/macOS, but equivalent Windows scripts are included.

Clone the repo locally (master branch).

### Maven
1. From the project root, run `./mvnw clean install`.
   If you want to run the exchange integration tests, use `./mvnw clean install -Pint`. 
   To execute both unit and integration tests, use `./mvnw clean install -Pall`.
1. Take a look at the Javadoc in the `./target/apidocs` folders of the bxbot-trading-api, bxbot-strategy-api, 
   and bxbot-exchange-api modules after the build completes.
   
### Gradle
1. From the project root, run `./gradlew build`.
   If you want to run the exchange integration tests, use `./gradlew integrationTests`.
   To execute both unit and integration tests, use `./gradlew build integrationTests`.
1. To generate the Javadoc, run `./gradlew javadoc` and look in the `./build/docs/javadoc` folders of the 
   bxbot-trading-api, bxbot-strategy-api, and bxbot-exchange-api modules.
   
## Issue & Change Management

Issues and new features are managed using the project [Issue Tracker](https://github.com/gazbert/bxbot/issues) -
submit bugs here.
 
You are welcome to take on new features or fix bugs! See [here](CONTRIBUTING.md) for how to get involved. 

For help and general questions about BX-bot, check out the [Gitter](https://gitter.im/BX-bot/Lobby) channel.

## Testing
The bot has undergone basic unit testing on a _best-effort_ basis. 

There is a continuous integration build running on [Travis CI](https://travis-ci.com/github/gazbert/bxbot/branches).

The latest stable build can always be found on the [Releases](https://github.com/gazbert/bxbot/releases) page. 
The SNAPSHOT builds on master are active development builds, but the tests should always pass and the bot should always 
be deployable.

## User Guide
_"Change your opinions, keep to your principles; change your leaves, keep intact your roots."_ - Victor Hugo

### Configuration
The bot provides a simple plugin framework for:

* Exchanges to integrate with.
* Markets to trade on.
* Trading Strategies to execute.

It uses [YAML](https://en.wikipedia.org/wiki/YAML) configuration files. These live in the [`config`](./config) folder.
Any config changes require a restart of the bot to take effect.

Sample configurations for running on different exchanges can be found in the 
[`config/samples`](./config/samples)folder.

##### Engine
The [`engine.yaml`](./config/engine.yaml) file is used to configure the Trading Engine.

```yaml
engine:
  botId: my-bitstamp-bot_1
  botName: Bitstamp Bot
  emergencyStopCurrency: BTC
  emergencyStopBalance: 1.0
  tradeCycleInterval: 20
```

All fields are mandatory.

* The `botId` value is a unique identifier for the bot. Value must be an alphanumeric string. 
  Underscores and dashes are also permitted.

* The `botName` is a friendly name for the bot. Value must be an alphanumeric string. Spaces are allowed.
      
* The `emergencyStopCurrency` value must be set to prevent catastrophic loss on the exchange. 
  This is normally the currency you intend to hold a long position in. It should be set to the currency short code for 
  the wallet, e.g. BTC, LTC, USD. This value can be case sensitive for some exchanges - check the Exchange Adapter
  documentation.

* The `emergencyStopBalance` value must be set to prevent catastrophic loss on the exchange. 
  The Trading Engine checks this value at the start of every trade cycle: if your `emergencyStopCurrency` wallet
  balance on the exchange drops below this value, the Trading Engine will log it, send an Email Alert (if configured)
  and then shut down. If you set this value to 0, the bot will bypass the check - be careful.

* The `tradeCycleInterval` value is the interval in _seconds_ that the Trading Engine will wait/sleep before executing
  each trade cycle. The minimum value is 1 second. Some exchanges allow you to hit them harder than others. However, 
  while their API documentation might say one thing, the reality is you might get socket timeouts and 5xx responses if 
  you hit it too hard. You'll need to experiment with the trade cycle interval for different exchanges.

##### Exchange Adapters
You specify the Exchange Adapter you want BX-bot to use in the 
[`exchange.yaml`](./config/exchange.yaml) file. 

BX-bot supports 1 exchange per bot. 
This keeps things simple and helps minimise risk: problems on one exchange should not impact trading on another.

```yaml
exchange:
  name: Bitstamp
  adapter: com.gazbert.bxbot.exchanges.BitstampExchangeAdapter
  
  authenticationConfig:
    clientId: your-client-id
    key: your-api-key
    secret: your-secret-key
           
  networkConfig:
    connectionTimeout: 15
    nonFatalErrorCodes: [502, 503, 520, 522, 525]            
    nonFatalErrorMessages:
      - Connection reset
      - Connection refused
      - Remote host closed connection during handshake
      - Unexpected end of file from server
      
  otherConfig:
    not-needed-on-bitstamp-1: here for illustration purposes only
    not-needed-on-bitstamp-2: here for illustration purposes again
```

All fields are mandatory unless stated otherwise.

* The `name` value is a friendly name for the Exchange. It is used in log statements to display the Exchange's name.
  Value must be an alphanumeric string. Spaces are allowed.

* For the `adapter` value, you must specify the fully qualified name of the Exchange Adapter class for the Trading
  Engine to inject on startup. The class must be on the runtime classpath. See the 
  _[How do I write my own Exchange Adapter?](#how-do-i-write-my-own-exchange-adapter)_ section for more details.

* The `authenticationConfig` section is used by the inbuilt Exchange Adapters to configure their exchange trading
  API credentials - see the sample `exchange.yaml` config files for details.

* The `networkConfig` section is optional. It is used by the inbuilt Exchange Adapters to set their network
  configuration as detailed below:

    * The `connectionTimeout` field is optional. This is the timeout value that the exchange adapter will wait on socket
      connect/socket read when communicating with the exchange. Once this threshold has been breached,
      the exchange adapter will give up and throw an
      [`ExchangeNetworkException`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/ExchangeNetworkException.java).
      The sample Exchange Adapters are single threaded: if a request gets blocked, it will block all subsequent
      requests from getting to the exchange. This timeout value prevents an indefinite block. If not set, it defaults 
      to 30 seconds.

    * The `nonFatalErrorCodes` field is optional. It contains a list of HTTP status codes that will trigger the
      adapter to throw a non-fatal `ExchangeNetworkException`. This allows the bot to recover from temporary network
      issues. See the sample `exchange.yaml` config files for status codes to use.

    * The `nonFatalErrorMessages` field is optional. It contains a list of `java.io` Exception message content that will
      trigger the adapter to throw a non-fatal `ExchangeNetworkException`. This allows the bot to recover from
      temporary network issues. See the sample `exchange.yaml` config files for messages to use.

* The `otherConfig` section is optional. It is not needed for Bitstamp, but shown above for illustration purposes.
  If present, at least 1 item must be set - these are repeating key/value String pairs.
  This section is used by the inbuilt Exchange Adapters to set any additional config, e.g. buy/sell fees.

##### Markets
You specify which markets you want to trade on in the 
[`markets.yaml`](./config/markets.yaml) file.

```yaml
  markets:            
    - id: btcusd    
      name: BTC/USD        
      baseCurrency: BTC
      counterCurrency: USD
      enabled: true
      tradingStrategyId: scalping-strategy
  
    - id: ltcusd
      name: LTC/BTC
      baseCurrency: LTC
      counterCurrency: BTC
      enabled: false
      tradingStrategyId: scalping-strategy
```

All fields are mandatory unless stated otherwise.

* The `id` value is the market id as defined on the exchange. E.g. the BTC/USD market id is `btcusd` on 
  [Bitstamp](https://www.bitstamp.net/api/) - see `currency_pair` values.

* The `name` value is a friendly name for the market. The is used in the logs to display the market's name.
  Value must be an alphanumeric string.

* The `baseCurrency` value is the currency short code for the base currency in the currency pair. When you buy or 
  sell a currency pair, you are performing that action on the base currency. The base currency is the commodity you 
  are buying or selling. E.g. in a BTC/USD market, the first currency (BTC) is the base currency and the second
  currency (USD) is the counter currency.

* The `counterCurrency` value is the currency short code for the counter currency in the currency pair. This is also
  known as the _quote_ currency.

* The `enabled` value allows you to toggle trading on the market. Remember, config changes are only applied on startup.

* The `tradingStrategyId` value _must_ match a strategy `id` defined in your `strategies.yaml` config.
  Currently, BX-bot only supports 1 `strategy` per `market`.

##### Strategies #####
You specify the Trading Strategies you wish to use in the 
[`strategies.yaml`](./config/strategies.yaml) file.

```yaml
strategies:
  - id: scalping-strategy
    name: Basic Scalping Strat
    description: >
      A simple scalper that buys at the current BID price, holds until current market price has 
      reached a configurable minimum percentage gain, then sells at current ASK price, thereby
      taking profit from the spread.       
    # This strategy is injected using the bot's custom injection framework using its className
    className: com.gazbert.bxbot.strategies.ExampleScalpingStrategy
    configItems:
      counter-currency-buy-order-amount: 20                        
      minimum-percentage-gain: 2
            
  - id: macd-strategy
    name: MACD Based Strat
    description: Strat uses MACD data to take long position in USD.    
    # This strategy is injected using a Spring beanName
    beanName: yourMacdStrategyBean
    configItems:
      counter-currency-buy-order-amount: 20      
      shortEmaInterval: 12            
      longEmaInterval: 26            
```

All fields are mandatory unless stated otherwise.

* The `id` value is a unique identifier for the strategy. The `markets.yaml` `tradingStrategyId` entries 
  cross-reference this. Value must be an alphanumeric string. Underscores and dashes are also permitted.

* The `name` value is a friendly name for the strategy. The is used in the logs to display the strategy's name.
  Value must be an alphanumeric string. Spaces are allowed.

* The `description` value is optional.

You configure the loading of your strategy using either a `className` _or_ a `beanName`; you cannot specify both. 

* For the `className` value, you must specify the fully qualified name of your Strategy class for the Trading Engine
  to load and execute. This will use the bot's custom injection framework. The class must be on the runtime classpath.
  If you set this value to load your strategy, you cannot set the `beanName` value.
  
* For the `beanName` value, you must specify the Spring bean name of you Strategy component class for the Trading Engine
  to load and execute. You will also need to annotate your strategy class with `@Component("yourMacdStrategyBean")` - 
  see the [example strategy](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java).
  This results in Spring injecting the bean.
  If you set this value to load your strategy, you cannot set the `className` value.        

* The `configItems` section is optional. It allows you to set key/value pair config items. This config is passed
  to your Trading Strategy when the bot starts up; see the 
  _[How do I write my own Trading Strategy?](#how-do-i-write-my-own-trading-strategy)_ section.

##### Email Alerts
You specify the Email Alerts config in the 
[`email-alerts.yaml`](./config/email-alerts.yaml) file.

This config is used to send email alerts when the bot is forced to shut down due to an unexpected error occurring in the 
Trading Strategies or Exchange Adapters. The email is sent to the SMTP host using TLS.

```yaml
emailAlerts:
  enabled: false
  smtpConfig:
    host: smtp.gmail.com
    tlsPort: 587
    accountUsername: your.account.username@gmail.com
    accountPassword: your.account.password
    fromAddress: from.addr@gmail.com
    toAddress: to.addr@gmail.com
```

All fields are mandatory unless stated otherwise.

* If `enabled` is set to true, the bot will send email alerts to the `toAddress` if it needs to shut down due to a
  critical error. 

* The `smtpConfig` config is optional and only required if `enabled` is set to true. 
  Sample SMTP config for using a Gmail account is shown above - all elements within `smtpConfig` are mandatory. 

### How do I write my own Trading Strategy?
_"I was seldom able to see an opportunity until it had ceased to be one."_ - Mark Twain

The best place to start is with the
[`ExampleScalpingStrategy`](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java) -
more ideas can be found in the excellent [ta4j](https://github.com/ta4j/ta4j) project.
There is also a Trading Strategy specific channel on [Gitter](https://gitter.im/BX-bot/trading-strategies).
  
Your strategy must implement the 
[`TradingStrategy`](./bxbot-strategy-api/src/main/java/com/gazbert/bxbot/strategy/api/TradingStrategy.java)
interface. This allows the Trading Engine to:

* Inject your strategy on startup.
* Pass any configuration (set in the `strategies.yaml`) to your strategy.
* Invoke your strategy at each trade cycle.

You load your strategy using either `className` _or_ `beanName` in the `strategies.yaml` file - see the 
_[Strategies Configuration](#strategies)_ section for full details. The choice is yours, but `beanName` is the way to
go if you want to use other Spring features in your strategy, e.g. a 
[Repository](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Repository.html) 
to store your trade data.   

The Trading Engine will only send 1 thread through your Trading Strategy; you do not have to code for concurrency.

##### Making Trades
You use the [`TradingApi`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/TradingApi.java)
to make trades etc. The API is passed to your Trading Strategy implementation `init` method when the bot starts up. 
See the Javadoc for full details of the API.

##### Error Handling
Your Trading Strategy implementation should throw a 
[`StrategyException`](./bxbot-strategy-api/src/main/java/com/gazbert/bxbot/strategy/api/StrategyException.java)
whenever it 'breaks'. BX-bot's error handling policy is designed to fail hard and fast; it will log the error, send an
Email Alert (if configured), and shut down.

Note that the inbuilt Exchange Adapters will (some more often than others!) throw an
[`ExchangeNetworkException`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/ExchangeNetworkException.java)
if they encounter network issues connecting with the exchange. Your strategy should always catch this exception and
choose what to do next, e.g. retry the previous Trading API call, or 'swallow' the exception and wait until the Trading
Engine invokes the strategy again at the next trade cycle.

##### Configuration
You specify the Trading Strategies you wish to use in the `strategies.yaml` file - see the
_[Strategies Configuration](#strategies)_ section for full details.

The `configItems` section in the `strategies.yaml` allows you to set key/value pair config items to pass to your
Trading Strategy implementation. On startup, the Trading Engine will pass the config to your Trading Strategy's 
`init(TradingApi tradingApi, Market market, StrategyConfig config)` method. 

##### Dependencies
Your Trading Strategy implementation has a compile-time dependency on the [Strategy API](./bxbot-strategy-api)
and the [Trading API](./bxbot-trading-api).

The inbuilt
[`ExampleScalpingStrategy`](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java)
also has a compile-time dependency on log4j and Google Guava.

##### Packaging & Deployment #####
To get going fast, you can code your Trading Strategy and place it in the
[bxbot-strategies](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies)
module alongside the example strategy. When you build the project, your Trading Strategy will be included in the
BX-bot jar. You can also create your own jar for your strats, e.g. `my-strats.jar`, and include it on BX-bot's 
runtime classpath - see the _[Installation Guide](#the-manual-way)_ for how to do this.

### How do I write my own Exchange Adapter?
_"Battle not with monsters, lest ye become a monster, and if you gaze into the abyss, the abyss gazes also into you."_ -
Friedrich Nietzsche

It's not easy, and can be frustrating at times, but a good place to start is with one of the inbuilt Exchange Adapters - see the latest 
[`BitstampExchangeAdapter`](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges/BitstampExchangeAdapter.java)
for example. There is also an Exchange Adapter specific channel on [Gitter](https://gitter.im/BX-bot/exchange-adapters).

Your adapter must implement the 
[`TradingApi`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/TradingApi.java)
and the [`ExchangeAdapter`](./bxbot-exchange-api/src/main/java/com/gazbert/bxbot/exchange/api/ExchangeAdapter.java)
interfaces. This allows the:
            
* Trading Engine to inject your adapter on startup.
* Trading Engine to pass any configuration (set in the `exchange.yaml`) to your adapter.
* Trading Strategies to invoke your adapter's implementation of the `TradingApi` at each trade cycle.

[`AbstractExchangeAdapter`](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges/AbstractExchangeAdapter.java)
is a handy base class that all the inbuilt Exchange Adapters extend - it could be useful.

The Trading Engine will only send 1 thread through your Exchange Adapter; you do not have to code for concurrency.

##### Error Handling
Your Exchange Adapter implementation should throw a
[`TradingApiException`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/TradingApiException.java)
whenever it breaks; the Trading Strategies should catch this and decide how they want to proceed.

The Trading API provides an
[`ExchangeNetworkException`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/ExchangeNetworkException.java)
for adapters to throw when they cannot connect to the exchange to make Trading API calls. This allows for
Trading Strategies to recover from temporary network failures. The `exchange.yaml` config file has an 
optional `networkConfig` section, which contains `nonFatalErrorCodes` and `nonFatalErrorMessages` elements - 
these can be used to tell the adapter when to throw the exception.

The first release of the bot is _single-threaded_ for simplicity. The downside to this is that if an API call to the 
exchange gets blocked on IO, BX-bot will get stuck until your Exchange Adapter frees the block. The Trading API provides
an `ExchangeNetworkException` for your adapter to throw if it times-out connecting to the exchange. It is your 
responsibility to free up any blocked connections - see the 
[`AbstractExchangeAdapter`](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges/AbstractExchangeAdapter.java)
for an example how to do this.

The Trading Engine will also call your adapter directly when performing the _Emergency Stop_ check to see if the 
`emergencyStopCurrency` wallet balance on the exchange drops below the configured `emergencyStopBalance` value.
If this call to the [`TradingApi`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/TradingApi.java)
`getBalanceInfo()` fails and is not due to a `ExchangeNetworkException`, the Trading Engine will log the error, send an 
Email Alert (if configured), and shut down. If the API call failed due to an `ExchangeNetworkException`, the 
Trading Engine will log the error and sleep until the next trade cycle.

##### Configuration
You provide your Exchange Adapter details in the `exchange.yaml` file - see the 
_[Exchange Adapters Configuration](#exchange-adapters)_ section for full details.

The `otherConfig` section in the `exchange.yaml` allows you to set key/value pair config items to pass to your
Exchange Adapter implementation. On startup, the Trading Engine will pass the config to your Exchange Adapter's 
`init(ExchangeConfig config)` method. 

##### Dependencies
Your Exchange Adapter implementation has a compile-time dependency on the [Trading API](./bxbot-trading-api).

The inbuilt Exchange Adapters also have compile-time dependencies on log4j, Google Gson, and Google Guava.

##### Packaging & Deployment
To get going fast, you can code your Exchange Adapter and place it in the 
[bxbot-exchanges](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges) module alongside the other inbuilt
adapters. When you build the project, your Exchange Adapter will be included in the BX-bot jar. You can also create 
your own jar for your adapters, e.g. `my-adapters.jar`, and include it on BX-bot's runtime classpath -
see the _[Installation Guide](#the-manual-way)_ for how to do this.

### Logging
Logging for the bot is provided by [log4j](http://logging.apache.org/log4j). The log file is written to `logs/bxbot.log` 
using a rolling policy. When a log file size reaches 100 MB or a new day is started, it is archived and a new log file 
is created. BX-bot will create up to 7 archives on the same day; these are stored in a directory based on the current 
year and month. Only the last 90 archives are kept. Each archive is compressed using gzip. The logging level is set 
at `info`. You can change this default logging configuration in the [`config/log4j2.xml`](./config/log4j2.xml) file.

We recommend running at `info` level, as `debug` level logging will produce a *lot* of
output from the Exchange Adapters; it's very handy for debugging, but not so good for your disk space!
 
### REST API
_"Enlightenment means taking full responsibility for your life."_ - William Blake

The bot has a REST API that allows you to remotely:

* View and update Engine, Exchange, Markets, Strategy, and Email Alerts config.
* View and download the log file.
* Restart the bot - this is necessary for any config changes to take effect.

It has role based access control 
([RBAC](https://en.wikipedia.org/wiki/Role-based_access_control)): Users can view config and the
logs, but only administrators can update config and restart the bot.

It is secured using [JWT](https://jwt.io/) and has [TLS](https://en.wikipedia.org/wiki/Transport_Layer_Security)
support for Production environments. 

You can view the [Swagger](https://swagger.io/tools/swagger-ui/) docs at: 
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) once you've configured
and started the bot.

#### Configuration
The REST API listens for plain HTTP traffic on port `8080` by default - you can change the 
`server.port` in the [./config/application.properties](./config/application.properties) file.
 
**Warning:** The bot must be configured to use TLS if you plan on accessing the REST API over a
public network - see the _[TLS](#tls)_ section below.

You _must_ also change the `bxbot.restapi.jwt.secret` value in the 
[./config/application.properties](./config/application.properties) before using the REST API over a public network.
This is the key that is used to sign your web tokens - the JWTs are signed using the HS512 algorithm.
  
Other interesting configuration in the [./config/application.properties](./config/application.properties) includes:

* `bxbot.restapi.maxLogfileLines` - the maximum number of lines to be returned in a view log file request. 
(For a head request, the end of the file is truncated; for a tail request the start of the file is truncated).

* `bxbot.restapi.maxLogfileDownloadSize` - the maximum size of the logfile to download. 
If the size of the logfile exceeds this limit, the end of the file will be truncated.

* `bxbot.restapi.jwt.expiration` - the expires time of the JWT. Set to 10 mins. Be sure you know the
risks if you decide to extend the expiry time.

#### Users
You _must_ change the `PASSWORD` values in the 
[./bxbot-rest-api/src/main/resources/import.sql](./bxbot-rest-api/src/main/resources/import.sql)
before using the REST API over a public network - see instructions in the file on how to 
[bcrypt](https://en.wikipedia.org/wiki/Bcrypt) your passwords.

2 users have been set up out of the box: `user` and `admin`. These users have `user` and `admin`
roles respectively. Passwords are the same as the usernames - remember to change these :-)

When the bot starts up, Spring Boot will load the `import.sql` file and store the users and their 
access rights in its [H2](https://www.h2database.com/html/main.html) in-memory database.

#### Authentication
The REST API endpoints require a valid JWT to be passed in the `Authorization` header of any requests.

To obtain a JWT, your REST client needs to call the `/api/token` endpoint with a valid username/password 
contained in the `import.sql` file. See the 
[Authentication](http://localhost:8080/swagger-ui.html#/Authentication/getTokenUsingPOST) 
Swagger docs for how to do this.

The returned JWT expires after 10 mins. Your client should call the `/api/refresh` endpoint with the
JWT before it expires in order to get a new one. Alternatively, you can re-authenticate using the
`/api/token` endpoint.

#### TLS
The REST API _must_ be configured to use TLS before accessing it over a public network.

You will need to 
[create a keystore](https://docs.oracle.com/en/java/javase/11/tools/keytool.html) - the command to
create a [PKCS12](https://en.wikipedia.org/wiki/PKCS_12) self-signed certificate is shown below:

``` bash
keytool -genkeypair -alias rest-api-keystore -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650
```
 
The keystore must be on the app's classpath - you can put it in
the [./bxbot-rest-api/src/main/resources](./bxbot-rest-api/src/main/resources) and re-build the app to get up and running fast.
For a Production system, you'll want to replace the self-signed certificate with a 
CA signed certificate.

The 'TLS Configuration' section in the [./config/application.properties](./config/application.properties) 
file needs the following properties set:

``` properties
# Spring Boot profile for REST API.
# Must use https profile in Production environment.
spring.profiles.active=https

# SSL (TLS) configuration to secure the REST API.
# Must be enabled in Production environment.
server.port=8443
security.require-ssl=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=secret
server.ssl.key-store-type=PKCS12
```

## Coming Soon... (Definitely Maybe)

A UI built with [React](https://reactjs.org/) - it will consume the REST API. 

See the [Project Board](https://github.com/gazbert/bxbot/projects/2) for timescales and progress.
