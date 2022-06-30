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
import com.gazbert.bxbot.rest.api.v1.RestController;
import com.gazbert.bxbot.services.runtime.BotLogfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Controller for directing Bot Logfile requests.
 * 用于引导 Bot 日志文件请求的控制器。
 *
 * @author gazbert
 * @since 1.0
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping(RUNTIME_ENDPOINT_BASE_URI)
@Tag(name = "Bot Logfile")
public class BotLogfileController implements RestController {

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
   * 将日志文件作为下载返回。
   *
   * <p>If the file is larger than {@link RestApiConfig#getLogfileDownloadSize()}, the end of the logfile will be truncated.
   * * <p>如果文件大于 {@link RestApiConfig#getLogfileDownloadSize()}，日志文件的末尾将被截断。
   *
   * @param principal the authenticated user making the request.
   *                  发出请求的经过身份验证的用户。
   *
   * @param request the request.  the request.
   * @return the logfile as a download.  * @return 日志文件作为下载文件。
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = LOGFILE_DOWNLOAD_RESOURCE_PATH)
  @Operation(summary = "Downloads the logfile")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<Resource> downloadLogfile(
      @Parameter(hidden = true) Principal principal, HttpServletRequest request) {

    LOG.info(
        () ->
            "GET "
                + LOGFILE_RESOURCE_PATH
                + " - downloadLogfile() - caller: - 下载日志文件（） - 调用者："
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
      LOG.info(() -> "Could not determine file type. 无法确定文件类型。");
    }
    // Fallback to the default content type if type could not be determined
    // 如果无法确定类型，则回退到默认内容类型
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
   * Returns logfile content for the bot.  返回机器人的日志文件内容。
   *
   * <p>If the file has more lines than {@link RestApiConfig#getMaxLogfileLines()}, the content will
    be truncated accordingly:
   <p>如果文件的行数多于 {@link RestApiConfig#getMaxLogfileLines()}，则内容将
   相应地被截断：
   *
   * <ul>
   *   <li>For a head request, the end of the file will be truncated.
   *   <li>对于头部请求，文件末尾将被截断。
   *
   *   <li>For a tail request, the start of the file will be truncated.
   *   <li>对于尾部请求，文件的开头将被截断。
   *
   *   <li>If head or tail param is not specified, the start of the file will be truncated.
   *   <li>如果没有指定 head 或 tail 参数，文件的开头将被截断。
   *
   *   <li>If both a head and tail param is present (just why?!!), a tail request will be actioned.
   *   <li>如果同时存在 head 和 tail 参数（为什么？！！），则会执行尾部请求。
   * </ul>
   *
   * @param principal the authenticated user making the request.  发出请求的经过身份验证的用户。
   * @param head the number of lines to fetch from head of file.  从文件头获取的行数。
   * @param tail the number of lines to fetch from tail of file.  从文件尾部获取的行数。
   * @return the logfile.  日志文件。
   */
  @PreAuthorize("hasRole('USER')")
  @GetMapping(value = LOGFILE_RESOURCE_PATH)
  @Operation(summary = "Fetches section of logfile")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<String> getLogfile(
      @Parameter(hidden = true) Principal principal,
      @Parameter(description = "Number of lines to fetch from head of file. 从文件头获取的行数。", example = "100")
          @RequestParam(required = false)
          Integer head,
      @Parameter(description = "Number of lines to fetch from tail of file. 从文件尾部获取的行数。", example = "100")
          @RequestParam(required = false)
          Integer tail) {

    LOG.info(
        () ->
            "GET "
                + LOGFILE_RESOURCE_PATH
                + " - getLogfile() - caller: - getLogfile() - 调用者："
                + principal.getName()
                + ", head= 头="
                + head
                + ", tail= 尾巴="
                + tail);

    String logfile;
    final int maxLogfileLineCount = restApiConfig.getMaxLogfileLines();

    try {
      if (head != null && head > 0) {
        if (head > maxLogfileLineCount) {
          LOG.warn(
              () ->
                  "Requested head line count exceeds max line count. Using max line count: 请求的标题行数超过最大行数。使用最大行数："
                      + maxLogfileLineCount);
          logfile = botLogfileService.getLogfileHead(maxLogfileLineCount);
        } else {
          logfile = botLogfileService.getLogfileHead(head);
        }

      } else if (tail != null && tail > 0) {
        if (tail > maxLogfileLineCount) {
          LOG.warn(
              () ->
                  "Requested tail line count exceeds max line count. Using max line count: 请求的尾行数超过最大行数。使用最大行数："
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
