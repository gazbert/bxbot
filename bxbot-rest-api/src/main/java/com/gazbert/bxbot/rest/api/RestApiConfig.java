/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
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

package com.gazbert.bxbot.rest.api;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Encapsulates the configuration for the REST API.
 * 封装 REST API 的配置。
 *
 * <p>Values are loaded from the application.properties file on startup.
 * <p>值在启动时从 application.properties 文件加载。
 *
 * @author gazbert
 */
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "bxbot.restapi")
public class RestApiConfig {

  public static final int DEFAULT_MAX_LINES = 1000;
  public static final int DEFAULT_MAX_DOWNLOAD_SIZE = 1024 * 1024;
  private static final Logger LOG = LogManager.getLogger();

  @NotNull
  @Min(1)
  private int maxLogfileLines;

  @NotNull
  @Min(1)
  private int maxLogfileDownloadSize;

  /**
   * Returns the max logfile size (in bytes) to be returned by the REST API.
   * 返回 REST API 返回的最大日志文件大小（以字节为单位）。
   *
   * <p>It is specified in the application.properties file: bxbot.restapi.maxLogfileLines
   * * <p>在application.properties文件中指定：bxbot.restapi.maxLogfileLines
   *
   * @return the max logfile size in bytes.  @return 以字节为单位的最大日志文件大小。
   */
  public int getMaxLogfileLines() {
    if (maxLogfileLines == 0) {
      LOG.warn(
          () ->
              "bxbot.restapi.maxLogfileLines not set in application.properties file. bxbot.restapi.maxLogfileLines 未在 application.properties 文件中设置。"
                  + "Defaulting to: 默认为："
                  + DEFAULT_MAX_LINES
                  + " lines.线。”");
      maxLogfileLines = DEFAULT_MAX_LINES;
    }
    return maxLogfileLines;
  }

  public void setMaxLogfileLines(int maxLogfileLines) {
    this.maxLogfileLines = maxLogfileLines;
  }

  /**
   * Returns the max download size for a logfile (in bytes) returned by the REST API.
   *  返回 REST API 返回的日志文件的最大下载大小（以字节为单位）。
   *
   * <p>It is specified in the application.properties file: bxbot.restapi.maxLogfileDownloadSize
   *  * <p>在application.properties文件中指定：bxbot.restapi.maxLogfileDownloadSize
   *
   * @return max download size of the logfile in bytes.
   * @return 日志文件的最大下载大小（以字节为单位）。
   */
  public int getLogfileDownloadSize() {
    if (maxLogfileDownloadSize == 0) {
      LOG.warn(
          () ->
              "bxbot.restapi.maxLogfileDownloadSize not set in application.properties file.  bxbot.restapi.maxLogfileDownloadSize 未在 application.properties 文件中设置。"
                  + "Defaulting to: 默认为："
                  + DEFAULT_MAX_DOWNLOAD_SIZE
                  + " bytes 字节");
      maxLogfileDownloadSize = DEFAULT_MAX_DOWNLOAD_SIZE;
    }
    return maxLogfileDownloadSize;
  }

  public void setMaxLogfileDownloadSize(int maxLogfileDownloadSize) {
    this.maxLogfileDownloadSize = maxLogfileDownloadSize;
  }
}
