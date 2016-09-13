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

package io.swagger.inflector.controllers;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.inflector.config.Configuration;
import io.swagger.inflector.config.ControllerFactory;
import io.swagger.inflector.converters.InputConverter;
import io.swagger.inflector.models.ApiError;
import io.swagger.inflector.models.RequestContext;
import io.swagger.inflector.models.ResponseContext;
import io.swagger.inflector.schema.SchemaValidator;
import io.swagger.inflector.utils.ApiErrorUtils;
import io.swagger.inflector.utils.ApiException;
import io.swagger.inflector.utils.ContentTypeSelector;
import io.swagger.inflector.utils.ReflectionUtils;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import io.swagger.util.Json;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.process.Inflector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


public class SwaggerOperationController extends ReflectionUtils implements Inflector<ContainerRequestContext, Response> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerOperationController.class);

    private static Set<String> commonHeaders = new HashSet<String>();

    static {
        commonHeaders.add("Host");
        commonHeaders.add("User-Agent");
        commonHeaders.add("Accept");
        commonHeaders.add("Content-Type");
        commonHeaders.add("Content-Length");
    }

    private String path;
    private String httpMethod;
    private Operation operation;
    private Object controller = null;
    private Method method = null;


    private JavaType[] parameterClasses = null;


    private Map<String, Model> definitions;
    private InputConverter validator;
    private String controllerName;
    private String methodName;
    private String operationSignature;
    @Inject
    private Provider<Providers> providersProvider;
    @Inject
    private Provider<HttpServletRequest> requestProvider;
    private ControllerFactory controllerFactoryCache = null;


    private ResponseCreate responseCreate = null;
    private ParameterHandle parameterHandle = null;

    public SwaggerOperationController(Configuration config, String path, String httpMethod, Operation operation, Map<String, Model> definitions) {
        this.setConfiguration(config);
        this.path = path;
        this.httpMethod = httpMethod;
        this.operation = operation;
        this.definitions = definitions;
        this.validator = InputConverter.getInstance();
        this.method = detectMethod(operation);
        if (method == null) {
            LOGGER.debug("no method `" + methodName + "` in `" + controllerName + "` to map to, using mock response");
        }
        this.responseCreate = new ResponseCreate(this);
        this.parameterHandle = new ParameterHandle(this);
    }

    public Method detectMethod(Operation operation) {
        controllerName = getControllerName(operation);
        methodName = getMethodName(path, httpMethod, operation);
        JavaType[] args = getOperationParameterClasses(operation, this.definitions);

        StringBuilder builder = new StringBuilder();

        builder.append(getMethodName(path, httpMethod, operation))
                .append("(");

        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                builder.append(RequestContext.class.getCanonicalName()).append(" request");
            } else {
                builder.append(", ");
                if (args[i] == null) {
                    LOGGER.error("didn't expect a null class for " + operation.getParameters().get(i - 1).getName());
                } else if (args[i].getRawClass() != null) {
                    String className = args[i].getRawClass().getName();
                    if (className.startsWith("java.lang.")) {
                        className = className.substring("java.lang.".length());
                    }
                    builder.append(className);
                    builder.append(" ").append(operation.getParameters().get(i - 1).getName());
                }
            }
        }
        builder.append(")");

        operationSignature = "public io.swagger.inflector.models.ResponseContext " + builder.toString();

        LOGGER.info("looking for method: `" + operationSignature + "` in class `" + controllerName + "`");
        this.parameterClasses = args;

        if (controllerName != null && methodName != null) {
            try {
                Class<?> cls;
                try {
                    cls = Class.forName(controllerName);
                } catch (ClassNotFoundException e) {
                    controllerName = controllerName + "Controller";
                    cls = Class.forName(controllerName);
                }

                Method[] methods = cls.getMethods();
                for (Method method : methods) {
                    if (methodName.equals(method.getName())) {
                        Class<?>[] methodArgs = method.getParameterTypes();
                        if (methodArgs.length == args.length) {
                            int i = 0;
                            boolean matched = true;
                            if (!args[i].getRawClass().equals(methodArgs[i])) {
                                LOGGER.debug("failed to match " + args[i] + ", " + methodArgs[i]);
                                matched = false;
                            }
                            if (matched) {
                                this.parameterClasses = args;
                                this.controller = getControllerFactory().instantiateController(cls, operation);
                                LOGGER.debug("found class `" + controllerName + "`");
                                return method;
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                LOGGER.debug("didn't find class " + controller);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    //为了把parameterHandle和ResponseCreate移出去产生的getter

    Operation getOperation() {
        return operation;
    }

    Map<String, Model> getDefinitions() {
        return definitions;
    }

    Configuration getConfig() {
        return this.config;
    }

    InputConverter getValidator() {
        return validator;
    }

    JavaType[] getParameterClasses() {
        return parameterClasses;
    }
    //end

    @Override
    public Response apply(ContainerRequestContext ctx) {
        List<Parameter> parameters = operation.getParameters();
        final RequestContext requestContext = createContext(ctx);

        String path = ctx.getUriInfo().getPath();
        Map<String, Map<String, String>> formMap = new HashMap<String, Map<String, String>>();
        Map<String, File> inputStreams = new HashMap<String, File>();

        Object[] args = new Object[parameters.size() + 1];
        if (parameters != null)
            parameterHandle.handleParameters(ctx, parameters, requestContext, formMap, inputStreams, args);
        try {
            if (method != null)
                return getResponseByUserSuppliedMethod(ctx, args);
            Map<String, io.swagger.models.Response> responses = operation.getResponses();
            if (responses != null)
                return responseCreate.getResponse(requestContext, responses);
            return Response.ok().build();
        } finally {
            for (String key : inputStreams.keySet()) {
                File file = inputStreams.get(key);
                if (file != null) {
                    LOGGER.debug("deleting file " + file.getPath());

                    file.delete();
                }
            }
        }
    }

    //NOTE 现在应该不太需要这个，这是是在存在controller，覆盖了默认的时候用的。
    private Response getResponseByUserSuppliedMethod(ContainerRequestContext ctx, Object[] args) {
        LOGGER.info("calling method " + method + " on controller " + this.controller + " with args " + Arrays.toString(args));
        try {
            Object response = method.invoke(controller, args);
            if (response instanceof ResponseContext) {
                ResponseContext wrapper = (ResponseContext) response;
                ResponseBuilder builder = Response.status(wrapper.getStatus());

                // response headers
                for (String key : wrapper.getHeaders().keySet()) {
                    List<String> v = wrapper.getHeaders().get(key);
                    if (v.size() == 1) {
                        builder.header(key, v.get(0));
                    } else {
                        builder.header(key, v);
                    }
                }

                // entity
                if (wrapper.getEntity() != null) {
                    builder.entity(wrapper.getEntity());
                    // content type
                    if (wrapper.getContentType() != null) {
                        builder.type(wrapper.getContentType());
                    } else {
                        final ContextResolver<ContentTypeSelector> selector = providersProvider
                                .get().getContextResolver(ContentTypeSelector.class,
                                        MediaType.WILDCARD_TYPE);
                        if (selector != null) {
                            selector.getContext(getClass()).apply(ctx.getAcceptableMediaTypes(),
                                    builder);
                        }
                    }

                    if (operation.getResponses() != null) {
                        String responseCode = String.valueOf(wrapper.getStatus());
                        io.swagger.models.Response responseSchema = operation.getResponses().get(responseCode);
                        if (responseSchema == null) {
                            // try default response schema
                            responseSchema = operation.getResponses().get("default");
                        }
                        if (responseSchema != null && responseSchema.getSchema() != null) {
                            validate(wrapper.getEntity(), responseSchema.getSchema(), SchemaValidator.Direction.OUTPUT);
                        } else {
                            LOGGER.debug("no response schema for code " + responseCode + " to validate against");
                        }
                    }
                }

                return builder.build();
            }
            return Response.ok().entity(response).build();
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            for (Throwable cause = e.getCause(); cause != null; ) {
                if (cause instanceof ApiException) {
                    throw (ApiException) cause;
                }
                final Throwable next = cause.getCause();
                cause = next == cause || next == null ? null : next;
            }
            throw new ApiException(ApiErrorUtils.createInternalError(), e);
        }
    }

    static String extractFilenameFromHeaders(Map<String, String> headers) {
        String filename = headers.get("filename");
        if (StringUtils.isBlank(filename)) {
            return null;
        }

        filename = filename.trim();

        int ix = filename.lastIndexOf(File.separatorChar);
        if (ix != -1) {
            filename = filename.substring(ix + 1).trim();
            if (StringUtils.isBlank(filename)) {
                return null;
            }
        }

        return filename;
    }

    public void validate(Object o, Property property, SchemaValidator.Direction direction) throws ApiException {
        doValidation(o, property, direction);
    }

    public void validate(Object o, Model model, SchemaValidator.Direction direction) throws ApiException {
        doValidation(o, model, direction);
    }

    public void setContentType(RequestContext res, ResponseContext resp, Operation operation) {
        // honor what has been set, it may be determined by business logic in the controller
        if (resp.getContentType() != null) {
            return;
        }
        List<String> available = operation.getProduces();
        if (available != null) {
            for (String a : available) {
                MediaType mt = MediaType.valueOf(a);
                for (MediaType acceptable : res.getAcceptableMediaTypes()) {
                    if (mt.isCompatible(acceptable)) {
                        resp.setContentType(mt);
                        return;
                    }
                }
            }
            if (available.size() > 0) {
                resp.setContentType(MediaType.valueOf(available.get(0)));
            }
        }
    }

    public String getOperationSignature() {
        return operationSignature;
    }

    public void setOperationSignature(String operationSignature) {
        this.operationSignature = operationSignature;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    private RequestContext createContext(ContainerRequestContext from) {
        final RequestContext result = new RequestContext(from);
        if (requestProvider != null) {
            final HttpServletRequest request = requestProvider.get();
            if (request != null) {
                result.setRemoteAddr(request.getRemoteAddr());
            }
        }
        return result;
    }

    private void doValidation(Object value, Object schema, SchemaValidator.Direction direction) throws ApiException {
        if (config.getValidatePayloads().isEmpty()) {
            return;
        }
        switch (direction) {
            case INPUT:
                if (config.getValidatePayloads().contains(Configuration.Direction.IN)
                        //NOTE schema被转换为json，所有validate都是针对Schema么？有些是否应该针对一些key、value对？比如in queryString和in path的parameter。
                        && !SchemaValidator.validate(value, Json.pretty(schema), direction)) {
                    throw new ApiException(new ApiError()
                            .code(config.getInvalidRequestStatusCode())
                            .message("Input does not match the expected structure"));
                }
                break;
            case OUTPUT:
                if (config.getValidatePayloads().contains(Configuration.Direction.OUT)
                        && !SchemaValidator.validate(value, Json.pretty(schema), direction)) {
                    throw new ApiException(new ApiError()
                            .code(config.getInvalidRequestStatusCode())
                            .message("The server generated an invalid response"));
                }
                break;
        }
    }

    private ControllerFactory getControllerFactory() {
        if (controllerFactoryCache == null) {
            controllerFactoryCache = config.getControllerFactory();
        }
        return controllerFactoryCache;
    }
}
