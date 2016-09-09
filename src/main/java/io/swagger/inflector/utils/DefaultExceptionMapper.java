/*
 *  Copyright 2016 SmartBear Software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.swagger.inflector.utils;

import io.swagger.inflector.models.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExceptionMapper.class);

    @Context
    Providers providers;

    @Context
    private HttpHeaders headers;

    public Response toResponse(Exception exception) {
        final ApiError error = createError(exception);
        final int code = error.getCode();
        if (code != Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(error.getMessage(), exception);
            }
        } else {
            LOGGER.error(error.getMessage(), exception);
        } final Response.ResponseBuilder builder = Response.status(code).entity(error);
        final ContextResolver<ContentTypeSelector> selector = providers.getContextResolver(
                ContentTypeSelector.class, MediaType.WILDCARD_TYPE);
        if (selector != null) {
            selector.getContext(getClass()).apply(headers.getAcceptableMediaTypes(), builder);
        }
        return builder.build();
    }

    private ApiError createError(Exception exception) {
        if (exception instanceof ApiException) {
            return ((ApiException) exception).getError();
        } else if (exception instanceof WebApplicationException) {
            final WebApplicationException e = (WebApplicationException) exception;
            return new ApiError().code(e.getResponse().getStatus()).message(e.getMessage());
        } else {
            return ApiErrorUtils.createInternalError();
        }
    }
}
