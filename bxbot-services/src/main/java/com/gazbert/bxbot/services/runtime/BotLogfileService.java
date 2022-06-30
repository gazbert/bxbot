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
 * Bot 日志文件服务。
 *
 * @author gazbert
 */
public interface BotLogfileService {

  /**
   * Returns entire logfile as a Resource. The beginning of the file is truncated if the file size exceeds maxFileSize.
   * * 将整个日志文件作为资源返回。如果文件大小超过最大文件大小，文件的开头将被截断。
   *
   * @param maxFileSize the max size of the file to return. 要返回的文件的最大大小。
   * @return the logfile as a Resource. 日志文件作为资源。
   * @throws IOException if an error occurs fetching the logfile. 如果在获取日志文件时发生错误。
   */
  Resource getLogfileAsResource(int maxFileSize) throws IOException;

  /**
   * Returns entire logfile as a String.
   * 将整个日志文件作为字符串返回。
   * The beginning of the file is truncated if the file line count exceeds maxLines.
   * 如果文件行数超过 maxLines，文件的开头将被截断。
   *
   * @param maxLines the max number of lines to return.  返回的最大行数。
   * @return the logfile lines as a String. 日志文件行作为字符串。
   * @throws IOException if an error occurs fetching the logfile.   如果在获取日志文件时发生错误。
   */
  String getLogfile(int maxLines) throws IOException;

  /**
   * Returns specified tail of the logfile as a String. The beginning of the file is truncated if the requested lineCount exceeds the actual logfile line count.
   * * 将日志文件的指定尾部作为字符串返回。如果请求的行数超过实际的日志文件行数，文件的开头将被截断。
   *
   * @param lineCount the requested line count.  请求的行数。
   * @return the logfile lines as a String. 日志文件行作为字符串。
   * @throws IOException if an error occurs fetching the logfile. 如果在获取日志文件时发生错误。
   */
  String getLogfileTail(int lineCount) throws IOException;

  /**
   * Returns specified head of the logfile as a String. The end of the file is truncated if the requested lineCount exceeds the actual logfile line count.
   * 将指定的日志文件头作为字符串返回。如果请求的行数超过实际的日志文件行数，文件的结尾将被截断。
   *
   * @param lineCount the requested line count. 请求的行数。
   * @return the logfile lines as a String. 日志文件行作为字符串。
   * @throws IOException if an error occurs fetching the logfile.  如果在获取日志文件时发生错误。
   */
  String getLogfileHead(int lineCount) throws IOException;
}
