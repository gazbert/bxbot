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

package com.gazbert.bxbot.services.runtime;

import java.io.IOException;
import org.springframework.core.io.Resource;

/**
 * The Bot logfile service.
 *
 * @author gazbert
 */
public interface BotLogfileService {

  /**
   * Returns entire logfile as a Resource. The beginning of the file is truncated if the file size
   * exceeds maxFileSize.
   *
   * @param maxFileSize the max size of the file to return.
   * @return the logfile as a Resource.
   * @throws IOException if an error occurs fetching the logfile.
   */
  Resource getLogfileAsResource(int maxFileSize) throws IOException;

  /**
   * Returns entire logfile as a String. The beginning of the file is truncated if the file line
   * count exceeds maxLines.
   *
   * @param maxLines the max number of lines to return.
   * @return the logfile lines as a String.
   * @throws IOException if an error occurs fetching the logfile.
   */
  String getLogfile(int maxLines) throws IOException;

  /**
   * Returns specified tail of the logfile as a String. The beginning of the file is truncated if
   * the requested lineCount exceeds the actual logfile line count.
   *
   * @param lineCount the requested line count.
   * @return the logfile lines as a String.
   * @throws IOException if an error occurs fetching the logfile.
   */
  String getLogfileTail(int lineCount) throws IOException;

  /**
   * Returns specified head of the logfile as a String. The end of the file is truncated if the
   * requested lineCount exceeds the actual logfile line count.
   *
   * @param lineCount the requested line count.
   * @return the logfile lines as a String.
   * @throws IOException if an error occurs fetching the logfile.
   */
  String getLogfileHead(int lineCount) throws IOException;
}
