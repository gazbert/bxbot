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

package com.gazbert.bxbot.rest.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Tests the REST API configuration can be set and loaded as expected.
 *
 * @author gazbert
 */
public class TestRestApiConfiguration {

  private static final int MAX_LOGFILE_LINES = 1000;
  private static final int MAX_LOGFILE_DOWNLOAD_SIZE = 2 * 1048;

  @Test
  public void testMaxLogfileLinesCanBeSetAndFetched() {
    final RestApiConfiguration restApiConfiguration = new RestApiConfiguration();
    restApiConfiguration.setMaxLogfileLines(MAX_LOGFILE_LINES);
    assertThat(restApiConfiguration.getMaxLogfileLines()).isEqualTo(MAX_LOGFILE_LINES);
  }

  @Test
  public void testMaxLogfileLinesDefaultFallback() {
    final RestApiConfiguration restApiConfiguration = new RestApiConfiguration();
    restApiConfiguration.setMaxLogfileLines(0);
    assertThat(restApiConfiguration.getMaxLogfileLines())
        .isEqualTo(RestApiConfiguration.DEFAULT_MAX_LINES);
  }

  @Test
  public void testMaxLogfileDownloadSizeCanBeSetAndFetched() {
    final RestApiConfiguration restApiConfiguration = new RestApiConfiguration();
    restApiConfiguration.setMaxLogfileDownloadSize(MAX_LOGFILE_DOWNLOAD_SIZE);
    assertThat(restApiConfiguration.getLogfileDownloadSize()).isEqualTo(MAX_LOGFILE_DOWNLOAD_SIZE);
  }

  @Test
  public void testMaxLogfileDownloadSizeDefaultFallback() {
    final RestApiConfiguration restApiConfiguration = new RestApiConfiguration();
    restApiConfiguration.setMaxLogfileDownloadSize(0);
    assertThat(restApiConfiguration.getLogfileDownloadSize())
        .isEqualTo(RestApiConfiguration.DEFAULT_MAX_DOWNLOAD_SIZE);
  }
}
