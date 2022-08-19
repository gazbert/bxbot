/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
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

package com.gazbert.bxbot.exchanges;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.TradingApiException;
import com.google.common.base.MoreObjects;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class for common Exchange Adapter functionality.
 * 通用 Exchange 适配器功能的基类。
 *
 * <p>Exchange Adapters should extend this class.
 * <p>Exchange 适配器应该扩展这个类。
 *
 * @author gazbert
 * @since 1.0
 */
abstract class AbstractExchangeAdapter {

  private static final Logger LOG = LogManager.getLogger();
  private static final String EXCHANGE_CONFIG_FILE = "config/exchange.yaml";

  private static final String UNEXPECTED_IO_ERROR_MSG =
      "Failed to connect to Exchange due to unexpected IO error. 由于意外 IO 错误，无法连接到 Exchange。";
  private static final String IO_SOCKET_TIMEOUT_ERROR_MSG =
      "Failed to connect to Exchange due to socket timeout. 由于套接字超时，无法连接到 Exchange。";
  private static final String IO_5XX_TIMEOUT_ERROR_MSG =
      "Failed to connect to Exchange due to 5xx timeout. 由于 5xx 超时，无法连接到 Exchange。";
  private static final String AUTHENTICATION_CONFIG_MISSING =
      "authenticationConfig is missing in exchange.yaml file. exchange.yaml 文件中缺少 authenticationConfig。";
  private static final String NETWORK_CONFIG_MISSING =
      "networkConfig is missing in exchange.yaml file. exchange.yaml 文件中缺少 networkConfig。";
  private static final String OTHER_CONFIG_MISSING =
      "otherConfig is missing in exchange.yaml file. exchange.yaml 文件中缺少 otherConfig。";
  private static final String CONFIG_IS_NULL_OR_ZERO_LENGTH =
      " cannot be null or zero length! HINT: is the value set in the  不能为空或零长度！提示：是在";

  private static final String CONNECTION_TIMEOUT_PROPERTY_NAME = "connection-timeout";
  private static final String NON_FATAL_ERROR_CODES_PROPERTY_NAME = "non-fatal-error-codes";
  private static final String NON_FATAL_ERROR_MESSAGES_PROPERTY_NAME = "non-fatal-error-messages";

  private final Set<Integer> nonFatalNetworkErrorCodes;
  private final Set<String> nonFatalNetworkErrorMessages;

  private int connectionTimeout;
  private DecimalFormatSymbols decimalFormatSymbols;

  /**
   * Constructor sets some sensible defaults for the network config and specifies decimal point symbol.
   * 构造函数为网络配置设置一些合理的默认值并指定小数点符号。
   */
  AbstractExchangeAdapter() {
    connectionTimeout = 30;
    nonFatalNetworkErrorCodes = new HashSet<>();
    nonFatalNetworkErrorMessages = new HashSet<>();

    // Some locales (e.g. Czech Republic) default to ',' instead of '.' for decimal point. Exchanges
    // always require a '.'
    decimalFormatSymbols = new DecimalFormatSymbols(Locale.getDefault());
    decimalFormatSymbols.setDecimalSeparator('.');
  }

  /**
   * Makes a request to the Exchange.
   * 向交易所提出请求。
   *
   * @param url the URL to invoke.
   *            * @param url 要调用的 URL。
   *
   * @param postData optional post data to send. This can be null.
   *                 * @param postData 可选的发送数据。这可以为空。
   *
   * @param httpMethod the HTTP method to use, e.g. GET, POST, DELETE
   *                   * @param httpMethod 要使用的 HTTP 方法，例如 GET、POST、DELETE
   *
   * @param requestHeaders optional request headers to set on the {@link URLConnection} used to   invoke the Exchange.
   *                       * @param requestHeaders 在用于调用 Exchange 的 {@link URLConnection} 上设置的可选请求标头。
   *
   * @return the response from the Exchange.
   * * @return 来自交易所的响应。
   *
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *  如果尝试连接到 Exchange 时发生网络错误，则发生 ExchangeNetworkException。
   *
   *     This exception allows for recovery from temporary network issues.
   *     此例外允许从临时网络问题中恢复。
   *
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   * TradingApiException 如果 API 调用因网络错误以外的任何原因而失败。
   *
   *     This means something really bad as happened.
   *     * 这意味着发生了非常糟糕的事情。
   */
  ExchangeHttpResponse sendNetworkRequest(
      URL url, String httpMethod, String postData, Map<String, String> requestHeaders)
      throws TradingApiException, ExchangeNetworkException {

    HttpURLConnection exchangeConnection = null;
    final StringBuilder exchangeResponse = new StringBuilder();

    try {
      LOG.debug(() -> "Using following URL for API call (使用下面的URL进行API调用): " + url);

      exchangeConnection = (HttpURLConnection) url.openConnection();
      exchangeConnection.setUseCaches(false);
      exchangeConnection.setDoOutput(true);
      exchangeConnection.setRequestMethod(httpMethod); // GET|POST|DELETE
      exchangeConnection.setDoInput(true);

      setRequestHeaders(exchangeConnection, requestHeaders);

      // Add a timeout so we don't get blocked indefinitely; timeout on URLConnection is in millis.
      //添加一个超时，这样我们就不会被无限期地阻塞； URLConnection 上的超时以毫秒为单位。
      final int timeoutInMillis = connectionTimeout * 1000;
      exchangeConnection.setConnectTimeout(timeoutInMillis);
      exchangeConnection.setReadTimeout(timeoutInMillis);


      if (httpMethod.equalsIgnoreCase("POST") && postData != null) {
        LOG.debug(() -> "Doing POST with request body: " + postData);
        //TODO 手动设置安全策略
//        System.setSecurityManager(new SecurityManager(){
//          public void checkConnect (String host, int port) {}
//          public void checkConnect (String host, int port, Object context) {}
//        });
        // TODO 请求代理接口  仅在本地测试开启
        System.setProperty("proxyType", "4");
        System.setProperty("proxyPort", Integer.toString(10809));
        System.setProperty("proxyHost", "127.0.0.1");
        System.setProperty("proxySet", "true");
//
//        URL url2 = new URL("https://www.baidu.com/");
//        URL url2 = new URL("https://www.google.com/");
//        HttpURLConnection exchangeConnection2 = (HttpURLConnection) url2.openConnection();
//        exchangeConnection2.setRequestMethod("GET");
//        exchangeConnection2.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36");
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exchangeConnection2.getInputStream(), "UTF-8"));
//        String msg;
//        while ((msg=bufferedReader.readLine()) != null ){
//          System.out.println("==============="+msg);
//        }
//        if (true) return null;

        try (final OutputStreamWriter outputPostStream =
            new OutputStreamWriter(exchangeConnection.getOutputStream(), StandardCharsets.UTF_8)) {
          outputPostStream.write(postData);
        }
      }

      // Grab the response - we just block here as per Connection API
      // 获取响应 - 我们只是按照连接 API 在这里阻塞
      try (final BufferedReader responseInputStream =
          new BufferedReader(
              new InputStreamReader(exchangeConnection.getInputStream(), StandardCharsets.UTF_8))) {

        // Read the JSON response lines into our response buffer
        // 将 JSON 响应行读入我们的响应缓冲区
        String responseLine;
        while ((responseLine = responseInputStream.readLine()) != null) {
          exchangeResponse.append(responseLine);
        }

        return new ExchangeHttpResponse(
            exchangeConnection.getResponseCode(),
            exchangeConnection.getResponseMessage(),
            exchangeResponse.toString());
      }

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);

    } catch (SocketTimeoutException e) {
      final String errorMsg = IO_SOCKET_TIMEOUT_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new ExchangeNetworkException(errorMsg, e);

//    } catch (FileNotFoundException | UnknownHostException e) {
    } catch (FileNotFoundException | UnknownHostException e) {
      // Huobi started throwing FileNotFoundException as of 8 Nov 2015. 火币从 2015 年 11 月 8 日开始抛出 FileNotFoundException。
      // EC2 started throwing UnknownHostException for BTC-e, GDAX, as of 14 July 2016 :-/ // 自 2016 年 7 月 14 日起，EC2 开始为 BTC-e、GDAX 抛出 UnknownHostException :-/
      final String errorMsg = "Failed to connect to Exchange. It's dead Jim! 无法连接到 Exchange。吉姆死了！";
      LOG.error(errorMsg, e);
      throw new ExchangeNetworkException(errorMsg, e);

    } catch (IOException e) {
      try {
        if (errorMessageIsRecoverableNetworkError(e)) {
          final String errorMsg =
              "Failed to connect to Exchange. SSL Connection was refused or reset by the server. 无法连接到 Exchange。 SSL 连接被服务器拒绝或重置。";
          LOG.error(errorMsg, e);
          throw new ExchangeNetworkException(errorMsg, e);

        } else if (errorCodeIsRecoverableNetworkError(exchangeConnection)) {
          final String errorMsg = IO_5XX_TIMEOUT_ERROR_MSG;
          LOG.error(errorMsg, e);
          throw new ExchangeNetworkException(errorMsg, e);

        } else {
          // Game over!
          String errorMsg = extractIoErrorMessage(exchangeConnection);
          LOG.error(errorMsg, e);
          throw new TradingApiException(errorMsg, e);
        }

      } catch (IOException e1) {
        final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
        LOG.error(errorMsg, e1);
        throw new TradingApiException(errorMsg, e1);
      }

    } finally {
      if (exchangeConnection != null) {
        exchangeConnection.disconnect();
      }
    }
  }

  /**
   * Sets the network config for the exchange adapter. This helper method expects the network config to be present.
   * * 设置交换适配器的网络配置。此辅助方法需要存在网络配置。
   *
   * @param exchangeConfig the exchange config.
   *                       交换配置。
   *
   * @throws IllegalArgumentException if the network config is not set.
   * @throws IllegalArgumentException 如果未设置网络配置。
   */
  void setNetworkConfig(ExchangeConfig exchangeConfig) {
    final NetworkConfig networkConfig = exchangeConfig.getNetworkConfig();
    if (networkConfig == null) {
      final String errorMsg = NETWORK_CONFIG_MISSING + exchangeConfig;
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }

    connectionTimeout = networkConfig.getConnectionTimeout();
    if (connectionTimeout == 0) {
      final String errorMsg =
          CONNECTION_TIMEOUT_PROPERTY_NAME + " cannot be 0 value. 不能为 0 值。" + exchangeConfig;
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }
    LOG.info(() -> CONNECTION_TIMEOUT_PROPERTY_NAME + ": " + connectionTimeout);

    final List<Integer> nonFatalErrorCodesFromConfig = networkConfig.getNonFatalErrorCodes();
    if (nonFatalErrorCodesFromConfig != null) {
      nonFatalNetworkErrorCodes.addAll(nonFatalErrorCodesFromConfig);
    }
    LOG.info(() -> NON_FATAL_ERROR_CODES_PROPERTY_NAME + ": " + nonFatalNetworkErrorCodes);

    final List<String> nonFatalErrorMessagesFromConfig = networkConfig.getNonFatalErrorMessages();
    if (nonFatalErrorMessagesFromConfig != null) {
      nonFatalNetworkErrorMessages.addAll(nonFatalErrorMessagesFromConfig);
    }
    LOG.info(() -> NON_FATAL_ERROR_MESSAGES_PROPERTY_NAME + ": " + nonFatalNetworkErrorMessages);
  }

  /**
   * Fetches the authentication config for the exchange adapter.
   * 获取交换适配器的身份验证配置。
   *
   * @param exchangeConfig the exchange adapter config.
   *                       * @param exchangeConfig 交换适配器配置。
   *
   * @return the authentication config for the adapter.
   * * @return 适配器的身份验证配置。
   *
   * @throws IllegalArgumentException if authentication config is not set in exchange adapter   config.
   * @throws IllegalArgumentException 如果身份验证配置未在交换适配器配置中设置。
   */
  AuthenticationConfig getAuthenticationConfig(ExchangeConfig exchangeConfig) {
    final AuthenticationConfig authenticationConfig = exchangeConfig.getAuthenticationConfig();
    if (authenticationConfig == null) {
      final String errorMsg = AUTHENTICATION_CONFIG_MISSING + exchangeConfig;
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }
    return authenticationConfig;
  }

  /**
   * Fetches other config for the exchange adapter.
   * 获取交换适配器的其他配置。
   *
   * @param exchangeConfig the exchange adapter config.
   *                       * @param exchangeConfig 交换适配器配置。
   *
   * @return the other config for the adapter.
   * * @return 适配器的其他配置。
   *
   * @throws IllegalArgumentException if optional config is not set.
   * @throws IllegalArgumentException 如果未设置可选配置。
   */
  OtherConfig getOtherConfig(ExchangeConfig exchangeConfig) {
    final OtherConfig otherConfig = exchangeConfig.getOtherConfig();
    if (otherConfig == null) {
      final String errorMsg = OTHER_CONFIG_MISSING + exchangeConfig;
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }
    return otherConfig;
  }

  /**
   * Fetches an authentication item value from the adapter config.
   * 从适配器配置中获取身份验证项值。
   *
   * @param authenticationConfig the authentication config for the adapter.
   *                             适配器的身份验证配置。
   *
   * @param itemName the config item name, e.g. key. secret, client-id
   *                 * @param itemName 配置项名称，例如钥匙。秘密，客户 ID
   * @return the config item value.
   *          * @return 配置项值。
   *
   * @throws IllegalArgumentException if authentication item is not set.
   *      @throws IllegalArgumentException 如果未设置身份验证项。
   */
  String getAuthenticationConfigItem(AuthenticationConfig authenticationConfig, String itemName) {
    final String itemValue = authenticationConfig.getItem(itemName);
    return assertItemExists(itemName, itemValue);
  }

  /**
   * Fetches an other config item value from the adapter config.
   *  从适配器配置中获取其他配置项值。
   *
   * @param otherConfig other config for the adapter.
   *                    * @param otherConfig 适配器的其他配置。
   *
   * @param itemName the config item name, e.g. buy-fee, sell-fee
   *                 * @param itemName 配置项名称，例如买入费，卖出费
   *
   * @return the config item value.
   *  * @return 配置项值。
   *
   * @throws IllegalArgumentException if authentication item is not set.
   *  * @throws IllegalArgumentException 如果未设置身份验证项。
   */
  String getOtherConfigItem(OtherConfig otherConfig, String itemName) {
    final String itemValue = otherConfig.getItem(itemName);
    LOG.info(() -> itemName + ": " + itemValue);
    return assertItemExists(itemName, itemValue);
  }

  /**
   * Sorts the request params alphabetically (uses natural ordering) and returns them as a query string.
   *    按字母顺序对请求参数进行排序（使用自然排序）并将它们作为查询字符串返回。
   *
   * @param params the request params to sort.
   *                * @param params 要排序的请求参数。
   * @return the query string containing the sorted request params.
   *  @return 包含已排序请求参数的查询字符串。
   */
  String createAlphabeticallySortedQueryString(Map<String, String> params) {
    final List<String> keys = new ArrayList<>(params.keySet());
    Collections.sort(keys); // use natural/alphabetical ordering of params   使用参数的自然/字母顺序

    final StringBuilder sortedQueryString = new StringBuilder();
    for (final String param : keys) {
      if (sortedQueryString.length() > 0) {
        sortedQueryString.append("&");
      }
      sortedQueryString.append(param);
      sortedQueryString.append("=");
      sortedQueryString.append(params.get(param));
    }
    return sortedQueryString.toString();
  }

  /**
   * Returns the decimal format symbols for using with BigDecimals with the exchanges. Specifically, the decimal point symbol is set to a '.'
   *  * 返回与 BigDecimals 一起使用的十进制格式符号。具体来说，小数点符号设置为“.”。
   *
   * @return the decimal format symbols.
   * @return 十进制格式符号。
   */
  DecimalFormatSymbols getDecimalFormatSymbols() {
    return decimalFormatSymbols;
  }

  /** Wrapper for holding Exchange HTTP response. 用于保存 Exchange HTTP 响应的包装器 */
  static class ExchangeHttpResponse {

    private final int statusCode;
    private final String reasonPhrase;
    private final String payload;

    ExchangeHttpResponse(int statusCode, String reasonPhrase, String payload) {
      this.statusCode = statusCode;
      this.reasonPhrase = reasonPhrase;
      this.payload = payload;
    }

    String getReasonPhrase() {
      return reasonPhrase;
    }

    int getStatusCode() {
      return statusCode;
    }

    String getPayload() {
      return payload;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("statusCode", statusCode)
          .add("reasonPhrase", reasonPhrase)
          .add("payload", payload)
          .toString();
    }
  }

  // --------------------------------------------------------------------------
  //  Util methods  // 实用方法
  // --------------------------------------------------------------------------

  private void setRequestHeaders(
      HttpURLConnection exchangeConnection, Map<String, String> requestHeaders) {
    // Er, perhaps, we need to be a bit more stealth here...  呃，也许，我们在这里需要更隐秘一点……
    // This was needed for some exchanges back in the day!  这在当时的一些交流中是必需的！
    exchangeConnection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/74.0.3729.169 Safari/537.36");

    if (requestHeaders != null) {
      for (final Map.Entry<String, String> requestHeader : requestHeaders.entrySet()) {
        exchangeConnection.setRequestProperty(requestHeader.getKey(), requestHeader.getValue());
        LOG.debug(() -> "Setting following request header:  设置以下请求标头：" + requestHeader);
      }
    }
  }

  private boolean errorMessageIsRecoverableNetworkError(Exception e) {
    return e.getMessage() != null && nonFatalNetworkErrorMessages.contains(e.getMessage());
  }

  private boolean errorCodeIsRecoverableNetworkError(HttpURLConnection exchangeConnection)
      throws IOException {
    return (exchangeConnection != null
        && nonFatalNetworkErrorCodes.contains(exchangeConnection.getResponseCode()));
  }

  private String extractIoErrorMessage(HttpURLConnection exchangeConnection) throws IOException {
    String errorMsg = UNEXPECTED_IO_ERROR_MSG;
    // Check for any clue in the response...  检查响应中的任何线索...
    if (exchangeConnection != null) {
      final InputStream rawErrorStream = exchangeConnection.getErrorStream();
      if (rawErrorStream != null) {
        try (final BufferedReader errorInputStream =
            new BufferedReader(new InputStreamReader(rawErrorStream, StandardCharsets.UTF_8))) {

          final StringBuilder errorResponse = new StringBuilder();
          String errorLine;
          while ((errorLine = errorInputStream.readLine()) != null) {
            errorResponse.append(errorLine);
          }
          errorMsg += " ErrorStream Response: 错误流响应：" + errorResponse;
        }
      }
    }
    return errorMsg;
  }

  private static String assertItemExists(String itemName, String itemValue) {
    if (itemValue == null || itemValue.length() == 0) {
      final String errorMsg =
          itemName + CONFIG_IS_NULL_OR_ZERO_LENGTH + EXCHANGE_CONFIG_FILE + " ?";
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }
    return itemValue;
  }
}
