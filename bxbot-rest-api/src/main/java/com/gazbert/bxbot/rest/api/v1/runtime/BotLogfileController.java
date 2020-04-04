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

package com.gazbert.bxbot.rest.api.v1.runtime;

import static com.gazbert.bxbot.rest.api.v1.EndpointLocations.RUNTIME_ENDPOINT_BASE_URI;

import com.gazbert.bxbot.rest.api.RestApiConfig;
import com.gazbert.bxbot.services.runtime.BotLogfileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for directing Bot Logfile requests.
 *
 * @author gazbert
 * @since 1.0
 */
@Api(tags = {"Bot Logfile"})
@RestController
@RequestMapping(RUNTIME_ENDPOINT_BASE_URI)
public class BotLogfileController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String LOGFILE_RESOURCE_PATH = "/logfile";
  private static final String LOGFILE_DOWNLOAD_RESOURCE_PATH = "/logfile/download";

  private final RestApiConfig restApiConfig;
  private final BotLogfileService botLogfileService;

  @Autowired
  public BotLogfileController(RestApiConfig restApiConfig, BotLogfileService botLogfileService) {
    this.restApiConfig = restApiConfig;
    this.botLogfileService = botLogfileService;
  }

  /**
   * Returns the logfile as a download.
   *
   * <p>If the file is larger than {@link RestApiConfig#getLogfileDownloadSize()}, the end of the
   * logfile will be truncated.
   *
   * @param principal the authenticated user making the request.
   * @param request the request.
   * @return the logfile as a download.
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = LOGFILE_DOWNLOAD_RESOURCE_PATH)
  public ResponseEntity<Resource> downloadLogfile(
      @ApiIgnore Principal principal, HttpServletRequest request) {

    LOG.info(
        () ->
            "GET "
                + LOGFILE_RESOURCE_PATH
                + " - downloadLogfile() - caller: "
                + principal.getName());

    Resource logfile;
    try {
      logfile = botLogfileService.getLogfileAsResource(restApiConfig.getLogfileDownloadSize());
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    String contentType = null;
    try {
      contentType = request.getServletContext().getMimeType(logfile.getFile().getAbsolutePath());
    } catch (IOException ex) {
      LOG.info(() -> "Could not determine file type.");
    }
    // Fallback to the default content type if type could not be determined
    if (contentType == null) {
      contentType = "application/octet-stream";
    }

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + logfile.getFilename() + "\"")
        .body(logfile);
  }

  /**
   * Returns logfile content for the bot.
   *
   * <p>If the file has more lines than {@link RestApiConfig#getMaxLogfileLines()}, the content will
   * be truncated accordingly:
   *
   * <ul>
   *   <li>For a head request, the end of the file will be truncated.
   *   <li>For a tail request, the start of the file will be truncated.
   *   <li>If head or tail param is not specified, the start of the file will be truncated.
   *   <li>If both a head and tail param is present (just why?!!), a tail request will be actioned.
   * </ul>
   *
   * @param principal the authenticated user making the request.
   * @param head the number of lines to fetch from head of file.
   * @param tail the number of lines to fetch from tail of file.
   * @return the logfile.
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = LOGFILE_RESOURCE_PATH)
  public ResponseEntity<String> getLogfile(
      @ApiIgnore Principal principal,
      @ApiParam(value = "Number of lines to fetch from head of file.", example = "100")
          @RequestParam(required = false)
          Integer head,
      @ApiParam(value = "Number of lines to fetch from tail of file.", example = "100")
          @RequestParam(required = false)
          Integer tail) {

    LOG.info(
        () ->
            "GET "
                + LOGFILE_RESOURCE_PATH
                + " - getLogfile() - caller: "
                + principal.getName()
                + ", head="
                + head
                + ", tail="
                + tail);

    String logfile;
    final int maxLogfileLineCount = restApiConfig.getMaxLogfileLines();

    try {
      if (head != null && head > 0) {
        if (head > maxLogfileLineCount) {
          LOG.warn(
              () ->
                  "Requested head line count exceeds max line count. Using max line count: "
                      + maxLogfileLineCount);
          logfile = botLogfileService.getLogfileHead(maxLogfileLineCount);
        } else {
          logfile = botLogfileService.getLogfileHead(head);
        }

      } else if (tail != null && tail > 0) {
        if (tail > maxLogfileLineCount) {
          LOG.warn(
              () ->
                  "Requested tail line count exceeds max line count. Using max line count: "
                      + maxLogfileLineCount);
          logfile = botLogfileService.getLogfileTail(maxLogfileLineCount);
        } else {
          logfile = botLogfileService.getLogfileTail(tail);
        }

      } else {
        logfile = botLogfileService.getLogfile(restApiConfig.getMaxLogfileLines());
      }

      return new ResponseEntity<>(logfile, null, HttpStatus.OK);

    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
