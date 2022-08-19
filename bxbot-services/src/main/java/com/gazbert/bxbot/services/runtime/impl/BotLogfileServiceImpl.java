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

package com.gazbert.bxbot.services.runtime.impl;

import com.gazbert.bxbot.services.runtime.BotLogfileService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Implementation of the Bot logfile service.
 *
 * @author gazbert
 */
@Service("botLogfileService")
public class BotLogfileServiceImpl implements BotLogfileService {

  private static final Logger LOG = LogManager.getLogger();
  private static final String NEWLINE = System.getProperty("line.separator");
  private LogFileWebEndpoint logFileWebEndpoint;

  @Autowired
  public BotLogfileServiceImpl(LogFileWebEndpoint logFileWebEndpoint) {
    this.logFileWebEndpoint = logFileWebEndpoint;
  }

  @Override
  public Resource getLogfileAsResource(int maxFileSize) throws IOException {
    final Resource logfile = logFileWebEndpoint.logFile();
    final long logfileLength = logfile.contentLength();
    try {
      if (logfileLength <= maxFileSize) {
        return logfile;
      } else {
        LOG.warn(
            () ->
                "Logfile exceeds MaxFileSize. Truncating end of file. MaxFileSize: 日志文件超过 MaxFileSize。截断文件结尾。最大文件大小："
                    + maxFileSize
                    + " LogfileSize: 日志文件大小："
                    + logfileLength);
        final InputStream inputStream = logfile.getInputStream();
        final byte[] truncatedLogfile = new byte[maxFileSize];
        inputStream.readNBytes(truncatedLogfile, 0, maxFileSize);
        return new ByteArrayResource(truncatedLogfile);
      }
    } catch (IOException e) {
      final String errorMsg = "Failed to load logfile. Details: 无法加载日志文件。细节：" + e.getMessage();
      LOG.error(() -> errorMsg);
      throw e;
    }
  }

  @Override
  public String getLogfile(int maxLines) throws IOException {
    return getLogfileTail(maxLines);
  }

  @Override
  public String getLogfileHead(int lineCount) throws IOException {
    final Resource resource = logFileWebEndpoint.logFile();
    final Path logfilePath = Paths.get(resource.getURI());
    final List<String> fileLines = headFile(logfilePath, lineCount);
    final StringBuilder truncatedFile = new StringBuilder();
    fileLines.forEach(line -> truncatedFile.append(line).append(NEWLINE));
    return truncatedFile.toString();
  }

  @Override
  public String getLogfileTail(int lineCount) throws IOException {
    final Resource resource = logFileWebEndpoint.logFile();
    final Path logfilePath = Paths.get(resource.getURI());
    final List<String> fileLines = tailFile(logfilePath, lineCount);
    final StringBuilder truncatedFile = new StringBuilder();
    fileLines.forEach(line -> truncatedFile.append(line).append(NEWLINE));
    return truncatedFile.toString();
  }

  private static List<String> tailFile(final Path source, final int lineCount) throws IOException {
    try (Stream<String> stream = Files.lines(source)) {
      final FileBuffer fileBuffer = new FileBuffer(lineCount);
      stream.forEach(fileBuffer::collectTailLine);
      return fileBuffer.getTailLines();
    }
  }

  private static List<String> headFile(final Path source, final int lineCount) throws IOException {
    try (Stream<String> stream = Files.lines(source)) {
      final FileBuffer fileBuffer = new FileBuffer(lineCount);
      stream.forEach(fileBuffer::collectHeadLine);
      return fileBuffer.getHeadLines();
    }
  }

  /** Util class for reading lines of a logfile.
   * 用于读取日志文件行的实用程序类。 */
  private static final class FileBuffer {
    private int offset = 0;
    private final int maxLines;
    private final String[] lines;

    FileBuffer(int maxLines) {
      this.maxLines = maxLines;
      this.lines = new String[maxLines];
    }

    void collectTailLine(String line) {
      lines[offset++ % maxLines] = line;
    }

    void collectHeadLine(String line) {
      if (offset + 1 <= maxLines) {
        lines[offset++] = line;
      }
    }

    List<String> getTailLines() {
      // Truncates the file head if file line count > maxLines
      // 如果文件行数 > maxLines，则截断文件头
      return IntStream.range(offset < maxLines ? 0 : offset - maxLines, offset)
          .mapToObj(idx -> lines[idx % maxLines])
          .collect(Collectors.toList());
    }

    List<String> getHeadLines() {
      // Truncates the file tail if file line count > maxLines
      // 如果文件行数 > maxLines，则截断文件尾部
      return IntStream.range(0, maxLines > offset ? offset : maxLines)
          .mapToObj(idx -> lines[idx % maxLines])
          .collect(Collectors.toList());
    }
  }
}
