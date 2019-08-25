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
            "Logfile exceeds MaxFileSize. Truncating beginning of file. MaxFileSize: "
                + maxFileSize
                + " LogfileSize: "
                + logfileLength);
        final InputStream inputStream = logfile.getInputStream();
        final byte[] truncatedLogfile = new byte[maxFileSize];
        inputStream.readNBytes(truncatedLogfile, ((int) logfileLength) - maxFileSize, maxFileSize);
        return new ByteArrayResource(truncatedLogfile);
      }
    } catch (IOException e) {
      final String errorMsg = "Failed to load logfile. Details: " + e.getMessage();
      LOG.error(() -> errorMsg);
      throw e;
    }
  }

  @Override
  public String getLogfile(int maxLines) throws IOException {
    return getLogfileTail(maxLines);
  }

  @Override
  public String getLogfileHead(int lineCount) throws IOException  {
    final Resource resource = logFileWebEndpoint.logFile();
    final Path logfilePath = Paths.get(resource.getURI());
    final List<String> fileLines = headFile(logfilePath, lineCount);
    final StringBuilder truncatedFile = new StringBuilder();
    fileLines.forEach((line) -> truncatedFile.append(line).append(NEWLINE));
    return truncatedFile.toString();
  }

  @Override
  public String getLogfileTail(int lineCount) throws IOException {
    final Resource resource = logFileWebEndpoint.logFile();
    final Path logfilePath = Paths.get(resource.getURI());
    final List<String> fileLines = tailFile(logfilePath, lineCount);
    final StringBuilder truncatedFile = new StringBuilder();
    fileLines.forEach((line) -> truncatedFile.append(line).append(NEWLINE));
    return truncatedFile.toString();
  }

  private static List<String> tailFile(final Path source, final int lineCount) throws IOException {
    try (Stream<String> stream = Files.lines(source)) {
      if (stream.count() > lineCount) {
        LOG.warn(
            "Logfile line count exceeds requested tail line count. Truncating beginning of file. "
                + "RequestedLineCount: "
                + lineCount
                + " LogfileLineCount: "
                + stream.count());
      }
      final FileBuffer fileBuffer = new FileBuffer(lineCount);
      stream.forEach(fileBuffer::collect);
      return fileBuffer.getTailLines();
    }
  }

  private static List<String> headFile(final Path source, final int lineCount) throws IOException {
    try (Stream<String> stream = Files.lines(source)) {
      if (stream.count() > lineCount) {
        LOG.warn(
            "Logfile line count exceeds requested head line count. Truncating end of file. "
                + "RequestedLineCount: "
                + lineCount
                + " LogfileLineCount: "
                + stream.count());
      }
      final FileBuffer fileBuffer = new FileBuffer(lineCount);
      stream.forEach(fileBuffer::collect);
      return fileBuffer.getHeadLines();
    }
  }

  /**
   * Util class for reading lines of a logfile.
   */
  private static final class FileBuffer {
    private int offset = 0;
    private final int maxLines;
    private final String[] lines;

    FileBuffer(int maxLines) {
      this.maxLines = maxLines;
      this.lines = new String[maxLines];
    }

    void collect(String line) {
      lines[offset++ % maxLines] = line;
    }

    List<String> getTailLines() {
      return IntStream.range(offset < maxLines ? 0 : offset - maxLines, offset)
          .mapToObj(idx -> lines[idx % maxLines])
          .collect(Collectors.toList());
    }

    List<String> getHeadLines() {
      return IntStream.range(offset, offset < maxLines ? 0 : offset - maxLines)
          .mapToObj(idx -> lines[idx % maxLines])
          .collect(Collectors.toList());
    }
  }
}
