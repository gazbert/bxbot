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

import com.gazbert.bxbot.rest.api.security.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.text.MessageFormat;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The JWT Authentication Filter extracts the JWT from the Authorization header and validates it.
 * JWT 身份验证过滤器从 Authorization 标头中提取 JWT 并对其进行验证。
 *
 * <p>If no JWT is present, the next filter in the Spring Security filter chain is invoked.
 * <p>如果不存在 JWT，则调用 Spring Security 过滤器链中的下一个过滤器。
 *
 * <p>The filter is invoked once for every request to validate the JWT - we don't use sessions.
 * * <p>每个请求都会调用一次过滤器来验证 JWT - 我们不使用会话。
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by Stephan Zerhusen:
 * https://github.com/szerhusenBC/jwt-spring-security-demo
 * <p>代码源自 Stephan Zerhusen 的优秀 JWT 和 Spring Boot 示例：
 *  * https://github.com/szerhusenBC/jwt-spring-security-demo
 *
 * @author gazbert
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LogManager.getLogger();
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

  private JwtUtils jwtUtils;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException {

    try {
      // Extract token after Bearer prefix if present
      // 如果存在，则在 Bearer 前缀之后提取令牌
      String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
      if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
        authorizationHeader = authorizationHeader.substring(BEARER_PREFIX_LENGTH);
      }

      // Might be null if client does not have a token yet.
      // 如果客户端还没有令牌，则可能为 null。
      if (authorizationHeader != null) {
        final Claims claims = jwtUtils.validateTokenAndGetClaims(authorizationHeader);
        LOG.info(() -> "JWT is valid");
        final String username = jwtUtils.getUsernameFromTokenClaims(claims);
        LOG.info(() -> "Username in JWT: " + username);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
          // First time in - store user details in Spring's Security context
          // 第一次 - 在 Spring 的安全上下文中存储用户详细信息
          final UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  username, null, jwtUtils.getRolesFromTokenClaims(claims));

          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);

          LOG.info(
              () -> "Authenticated User: 认证用户：" + username + " has been set in Spring SecurityContext. 已在 Spring SecurityContext 中设置。");
        }
      }

      chain.doFilter(request, response);

    } catch (Exception e) {
      LOG.error(
          MessageFormat.format("JWT Authentication failure! Details:JWT 认证失败！细节： {0}", e.getMessage()), e);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
  }

  @Autowired
  public void setJwtUtils(JwtUtils jwtUtils) {
    this.jwtUtils = jwtUtils;
  }
}
