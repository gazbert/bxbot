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

import com.gazbert.bxbot.rest.api.v1.RestApiConfiguration;
import com.gazbert.bxbot.services.runtime.BotLogfileService;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for directing Bot Logfile requests.
 *
 * <p>GET /runtime/logfile/download [bxbot.restapi.maxLogfileDownloadSize]
 *
 * <p>GET /runtime/logfile [bxbot.restapi.maxLogfileLines]
 *
 * <p>GET /runtime/logfile?head=1000 [bxbot.restapi.maxLogfileLines]
 *
 * <p>GET /runtime/logfile?tail=2000 [bxbot.restapi.maxLogfileLines]
 *
 * @author gazbert
 * @since 1.0
 */
@RestController
@RequestMapping(RUNTIME_ENDPOINT_BASE_URI)
public class BotLogfileController {

  private static final Logger LOG = LogManager.getLogger();
  private static final String LOGFILE_RESOURCE_PATH = "/logfile";
  private static final String LOGFILE_DOWNLOAD_RESOURCE_PATH = "/logfile/download";

  private final RestApiConfiguration restApiConfiguration;
  private final BotLogfileService botLogfileService;

  @Autowired
  public BotLogfileController(
      RestApiConfiguration restApiConfiguration, BotLogfileService botLogfileService) {
    this.restApiConfiguration = restApiConfiguration;
    this.botLogfileService = botLogfileService;
  }


  /**
   * TODO: Does stuff.
   *
   * @param user the user.
   * @param request the request.
   * @return the logfile.
   */
  @GetMapping(value = LOGFILE_DOWNLOAD_RESOURCE_PATH)
  public ResponseEntity<Resource> downloadLogfile(
      @AuthenticationPrincipal User user, HttpServletRequest request) {

    LOG.info(
        () -> "GET " + LOGFILE_RESOURCE_PATH + " - getLogfile() - caller: " + user.getUsername());

    Resource logfile;
    try {
      logfile = botLogfileService.getLogfileAsResource(restApiConfiguration.getMaxLogfileLines());
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
   * <p>If the file has more lines than {@link RestApiConfiguration#getMaxLogfileLines()}, the
   * content will be truncated accordingly:
   *
   * <ul>
   *   <li>For a head request, the end of the file will be truncated.
   *   <li>For a tail request, the start of the file will be truncated.
   *   <li>If head or tail param not specified, the start of the file will be truncated
   * </ul>
   *
   * @param user the authenticated user making the request.
   * @return the logfile.
   */
  @GetMapping(value = LOGFILE_RESOURCE_PATH)
  public ResponseEntity<String> getLogfile(@AuthenticationPrincipal User user) {

    LOG.info(
        () -> "GET " + LOGFILE_RESOURCE_PATH + " - getLogfile() - caller: " + user.getUsername());

    String logfile;
    try {
      logfile = botLogfileService.getLogfile(restApiConfiguration.getMaxLogfileLines());
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(logfile, null, HttpStatus.OK);
  }
}
