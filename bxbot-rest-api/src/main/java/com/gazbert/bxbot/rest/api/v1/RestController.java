/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 gazbert
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

package com.gazbert.bxbot.rest.api.v1;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.context.annotation.PropertySource;

/**
 * Controller for all REST config and runtime operations.
 * 所有 REST 配置和运行时操作的控制器。
 *
 * @author gazbert
 * @since 1.0
 */
@PropertySource({"classpath:swagger.properties"})
@OpenAPIDefinition(
    info =
        @Info(
            title = "${swagger.info.title}",
            version = "${application.version}",
            description = "${swagger.info.description}",
            license =
                @License(
                    name = "MIT",
                    url = "https://github.com/gazbert/bxbot/blob/master/LICENSE"),
            termsOfService = "https://github.com/gazbert/bxbot"))
@SecurityRequirement(name = "Authorization")
public interface RestController {
}
