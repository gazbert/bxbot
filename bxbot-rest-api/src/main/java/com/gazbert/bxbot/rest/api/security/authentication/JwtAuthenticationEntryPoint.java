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

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * JWT 'login' entry point - Spring does not supply one for JWT Authentication.
 * * JWT 'login' 入口点 - Spring 没有为 JWT 身份验证提供一个入口点。
 *
 * <p>We just send a 401 response, to which the client should call the /auth endpoint to fetch a JWT for use in all subsequent requests.
 * * <p>我们只是发送一个 401 响应，客户端应该调用 /auth 端点来获取 JWT 以用于所有后续请求。
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by Stephan Zerhusen: https://github.com/szerhusenBC/jwt-spring-security-demo
 * * <p>代码源自 Stephan Zerhusen 的优秀 JWT 和 Spring Boot 示例：https://github.com/szerhusenBC/jwt-spring-security-demo
 *
 * @author gazbert
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  /**
   * This is invoked when a user tries to access a secured REST resource without supplying any
   credentials in the HTTP Authorization header.
   当用户尝试访问受保护的 REST 资源而不提供任何
   HTTP 授权标头中的凭据。
   *
   * <p>We just send a 401 Unauthorized response because there is no 'login page' to redirect to.
    The client should then post username/password to the /auth endpoint to obtain a JWT.
   <p>我们只是发送一个 401 Unauthorized 响应，因为没有“登录页面”可以重定向到。
   然后，客户端应将用户名/密码发布到 /auth 端点以获取 JWT。
   *
   * @param request the incoming request.  传入的请求。
   * @param response the outbound response.  出站响应。
   * @param authException the exception that got us here.  让我们来到这里的例外。
   */
  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
  }
}
