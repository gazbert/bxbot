/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Stephan Zerhusen
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

package com.gazbert.bxbot.rest.api.security.authentication;

import com.gazbert.bxbot.rest.api.security.jwt.JwtUser;
import com.gazbert.bxbot.rest.api.security.jwt.JwtUtils;
import com.gazbert.bxbot.rest.api.v1.RestController;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * REST Controller for handling initial authentication requests to obtain a JWT.
 * 用于处理初始身份验证请求以获取 JWT 的 REST 控制器。
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by Stephan Zerhusen:
 https://github.com/szerhusenBC/jwt-spring-security-demo
 <p>代码源自 Stephan Zerhusen 的优秀 JWT 和 Spring Boot 示例：
 https://github.com/szerhusenBC/jwt-spring-security-demo
 *
 * @author gazbert
 */
@org.springframework.web.bind.annotation.RestController
@Tag(name = "Authentication")
public class AuthenticationController implements RestController {

  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final JwtUtils jwtUtils;

  /**
   * Constructor for the controller.
   * 控制器的构造函数。
   *
   * @param authenticationManager the Spring Authentication Manager. Spring 身份验证管理器。
   * @param userDetailsService the user details service for looking up users. 用于查找用户的用户详细信息服务。
   * @param jwtUtils JWT utility functions.JWT 实用程序函数。
   */
  @Autowired
  public AuthenticationController(
      AuthenticationManager authenticationManager,
      UserDetailsService userDetailsService,
      JwtUtils jwtUtils) {

    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.jwtUtils = jwtUtils;
  }

  /**
   * Clients initially call this with their username/password in order to receive a JWT for use in
    future requests.
   客户最初使用他们的用户名/密码调用它，以便接收 JWT 以用于
   未来的请求。
   *
   * @param authenticationRequest the authentication request containing the client's  username/password. 包含客户端用户名/密码的身份验证请求。
   * @return a JWT if the client was authenticated successfully. * @return 如果客户端成功验证，则返回 JWT。
   * @throws AuthenticationException if the the client was not authenticated successfully.  @throws AuthenticationException 如果客户端没有成功认证。
   */
  @PostMapping(value = "/api/token")
  @Operation(summary = "Gets an API token")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = JwtAuthenticationResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<JwtAuthenticationResponse> getToken(
      @RequestBody JwtAuthenticationRequest authenticationRequest) {

    final Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                authenticationRequest.getUsername(), authenticationRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // Reload password post-security check, so we can generate the token...  // 在安全检查后重新加载密码，这样我们就可以生成令牌...
    final JwtUser userDetails =
        (JwtUser) userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
    final String token = jwtUtils.createToken(userDetails);

    return ResponseEntity.ok(new JwtAuthenticationResponse(token));
  }

  /**
   * Clients should call this in order to refresh a JWT. 客户端应该调用它来刷新 JWT。
   *
   * @param request the request from the client.  来自客户端的请求。
   * @return the JWT with an extended expiry time if the client was authenticated, a 400 Bad Request  otherwise.
   * * @return 如果客户端通过了身份验证，则返回具有延长到期时间的 JWT，否则返回 400 错误请求。
   */
  @GetMapping(value = "/api/token/refresh")
  @Operation(summary = "Refreshes an API token")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = JwtAuthenticationResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<JwtAuthenticationResponse> refreshToken(HttpServletRequest request) {

    final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    final Claims claims = jwtUtils.validateTokenAndGetClaims(authorizationHeader);
    final String username = jwtUtils.getUsernameFromTokenClaims(claims);
    final JwtUser user = (JwtUser) userDetailsService.loadUserByUsername(username);

    if (jwtUtils.canTokenBeRefreshed(claims, new Date(user.getLastPasswordResetDate()))) {
      final String refreshedToken = jwtUtils.refreshToken(authorizationHeader);
      return ResponseEntity.ok(new JwtAuthenticationResponse(refreshedToken));
    } else {
      return ResponseEntity.badRequest().body(null);
    }
  }
}
