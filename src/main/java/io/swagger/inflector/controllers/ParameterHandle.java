package io.swagger.inflector.controllers;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.io.Files;
import io.swagger.inflector.converters.ConversionException;
import io.swagger.inflector.converters.InputConverter;
import io.swagger.inflector.models.ApiError;
import io.swagger.inflector.models.RequestContext;
import io.swagger.inflector.processors.EntityProcessorFactory;
import io.swagger.inflector.schema.SchemaValidator;
import io.swagger.inflector.utils.ApiException;
import io.swagger.inflector.validators.ValidationException;
import io.swagger.inflector.validators.ValidationMessage;
import io.swagger.models.Model;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;

/**
 * Created by nielinjie on 9/12/16.
 */
class ParameterHandle {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterHandle.class);

    private SwaggerOperationController controller = null;

    ParameterHandle(SwaggerOperationController swaggerOperationController) {
        this.controller = swaggerOperationController;
    }

    void handleParameters(ContainerRequestContext ctx, List<Parameter> parameters, RequestContext requestContext, Map<String, Map<String, String>> formMap, Map<String, File> inputStreams, Object[] args) {
        int i = 0;

        args[i] = requestContext;
        i += 1;
        List<ValidationMessage> missingParams = new ArrayList<>();
        UriInfo uri = ctx.getUriInfo();
        String formDataString = null;
        String[] parts = null;
        Set<String> existingKeys = new HashSet<>();

        InputConverter validator = controller.getValidator();
        Map<String, Model> definitions = controller.getDefinitions();
        JavaType[] parameterClasses = controller.getParameterClasses();



        for (Iterator<String> x = uri.getQueryParameters().keySet().iterator(); x.hasNext(); ) {
            existingKeys.add(x.next() + ": qp");
        }
        for (Iterator<String> x = uri.getPathParameters().keySet().iterator(); x.hasNext(); ) {
            existingKeys.add(x.next() + ": pp");
        }
        for (Iterator<String> x = ctx.getHeaders().keySet().iterator(); x.hasNext(); ) {
            String key = x.next();
            //NOTE 为何被注释掉了，下面两行。Head里面不可能与QueryString、path重名？
            //existingKeys仅为发现多余的key存在。
            //必选由getQueryParameters,getPathParameters,getHeaders等是不是null判断。

//              if(!commonHeaders.contains(key))
//                existingKeys.add(key);
        }
        MediaType mt = requestContext.getMediaType();

        for (Parameter p : parameters) {
            Map<String, String> headers = new HashMap<String, String>();
            String name = null;

            if (p instanceof FormParameter) {
                if (formDataString == null) {
                    // can only read stream once
                    if (mt.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE))
                        handleMultipartDataType(ctx, formMap, inputStreams, mt, headers, name);
                    else {
                        try {
                            formDataString = IOUtils.toString(ctx.getEntityStream(), "UTF-8");
                            parts = formDataString.split("&");

                            for (String part : parts) {
                                String[] kv = part.split("=");
                                existingKeys.add(kv[0] + ": fp");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        for (Parameter parameter : parameters) {
            String in = parameter.getIn();
            Object o = null;

            try {

                if ("formData".equals(in))
                    o = handleFormData(formMap, inputStreams, i, missingParams, formDataString, parts, existingKeys, validator, definitions, parameterClasses, mt, parameter, o);
                else {
                    try {
                        String paramName = parameter.getName();
                        if ("query".equals(in)) {
                            existingKeys.remove(paramName + ": qp");
                        }
                        if ("path".equals(in)) {
                            existingKeys.remove(paramName + ": pp");
                        }
                        JavaType jt = parameterClasses[i];
                        Class<?> cls = jt.getRawClass();
                        if ("body".equals(in)) {
                            if (ctx.hasEntity()) {
                                BodyParameter body = (BodyParameter) parameter;
                                o = EntityProcessorFactory.readValue(ctx.getMediaType(), ctx.getEntityStream(), cls);
                                if (o != null) {
                                    controller.validate(o, body.getSchema(), SchemaValidator.Direction.INPUT);
                                }
                            } else if (parameter.getRequired()) {
                                ValidationException e = new ValidationException();
                                e.message(new ValidationMessage()
                                        .message("The input body `" + paramName + "` is required"));
                                throw e;
                            }
                        }
                        if ("query".equals(in)) {
                            o = validator.convertAndValidate(uri.getQueryParameters().get(parameter.getName()), parameter, cls, definitions);
                        } else if ("path".equals(in)) {
                            o = validator.convertAndValidate(uri.getPathParameters().get(parameter.getName()), parameter, cls, definitions);
                        } else if ("header".equals(in)) {
                            o = validator.convertAndValidate(ctx.getHeaders().get(parameter.getName()), parameter, cls, definitions);
                        }
                    } catch (ConversionException e) {
                        missingParams.add(e.getError());
                    } catch (ValidationException e) {
                        missingParams.add(e.getValidationMessage());
                    }
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Couldn't find " + parameter.getName() + " (" + in + ") to " + parameterClasses[i], e);
            }

            args[i] = o;
            i += 1;
        }
        if (existingKeys.size() > 0) {
            LOGGER.info("unexpected keys: " + existingKeys);
        }
        if (missingParams.size() > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append("Input error");
            if (missingParams.size() > 1) {
                builder.append("s");
            }
            builder.append(": ");
            int count = 0;
            for (ValidationMessage message : missingParams) {
                if (count > 0) {
                    builder.append(", ");
                }
                if (message != null && message.getMessage() != null) {
                    builder.append(message.getMessage());
                } else {
                    builder.append("no additional input");
                }
                count += 1;
            }
            int statusCode = controller.getConfig().getInvalidRequestStatusCode();
            ApiError error = new ApiError()
                    .code(statusCode)
                    .message(builder.toString());
            throw new ApiException(error);
        }
    }

    private Object handleFormData(Map<String, Map<String, String>> formMap, Map<String, File> inputStreams, int i, List<ValidationMessage> missingParams, String formDataString, String[] parts, Set<String> existingKeys, InputConverter validator, Map<String, Model> definitions, JavaType[] parameterClasses, MediaType mt, Parameter parameter, Object o) {
        LOGGER.info("formData found");
        SerializableParameter sp = (SerializableParameter) parameter;
        String name = parameter.getName();
        if (mt.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)) {
            // look in the form map
            Map<String, String> headers = formMap.get(name);
            if (headers != null && headers.size() > 0) {
                if ("file".equals(sp.getType())) {
                    o = inputStreams.get(name);
                } else {
                    Object obj = headers.get(parameter.getName());
                    if (obj != null) {
                        JavaType jt = parameterClasses[i];
                        Class<?> cls = jt.getRawClass();

                        List<String> os = Arrays.asList(obj.toString());
                        try {
                            o = validator.convertAndValidate(os, parameter, cls, definitions);
                        } catch (ConversionException e) {
                            missingParams.add(e.getError());
                        } catch (ValidationException e) {
                            missingParams.add(e.getValidationMessage());
                        }
                    }
                }
            }
        } else {
            if (formDataString != null) {
                for (String part : parts) {
                    String[] kv = part.split("=");
                    if (kv != null) {
                        if (kv.length > 0) {
                            existingKeys.remove(kv[0] + ": fp");
                        }
                        if (kv.length == 2) {
                            // TODO how to handle arrays here?
                            String key = kv[0];
                            try {
                                String value = URLDecoder.decode(kv[1], "utf-8");
                                if (parameter.getName().equals(key)) {
                                    JavaType jt = parameterClasses[i];
                                    Class<?> cls = jt.getRawClass();
                                    try {
                                        o = validator.convertAndValidate(Arrays.asList(value), parameter, cls, definitions);
                                    } catch (ConversionException e) {
                                        missingParams.add(e.getError());
                                    } catch (ValidationException e) {
                                        missingParams.add(e.getValidationMessage());
                                    }
                                }
                            } catch (UnsupportedEncodingException e) {
                                LOGGER.error("unable to decode value for " + key);
                            }
                        }
                    }
                }
            }
        }
        return o;
    }

    private void handleMultipartDataType(ContainerRequestContext ctx, Map<String, Map<String, String>> formMap, Map<String, File> inputStreams, MediaType mt, Map<String, String> headers, String name) {
        // get the boundary
        String boundary = mt.getParameters().get("boundary");

        if (boundary != null) {
            try {
                InputStream output = ctx.getEntityStream();

                MultipartStream multipartStream = new MultipartStream(output, boundary.getBytes());
                boolean nextPart = multipartStream.skipPreamble();
                while (nextPart) {
                    String header = multipartStream.readHeaders();
                    // process headers
                    if (header != null) {
                        CSVFormat format = CSVFormat.DEFAULT
                                .withDelimiter(';')
                                .withRecordSeparator("=");

                        Iterable<CSVRecord> records = format.parse(new StringReader(header));
                        for (CSVRecord r : records) {
                            for (int j = 0; j < r.size(); j++) {
                                String string = r.get(j);

                                Iterable<CSVRecord> outerString = CSVFormat.DEFAULT
                                        .withDelimiter('=')
                                        .parse(new StringReader(string));
                                for (CSVRecord outerKvPair : outerString) {
                                    if (outerKvPair.size() == 2) {
                                        String key = outerKvPair.get(0).trim();
                                        String value = outerKvPair.get(1).trim();
                                        if ("name".equals(key)) {
                                            name = value;
                                        }
                                        headers.put(key, value);
                                    } else {
                                        Iterable<CSVRecord> innerString = CSVFormat.DEFAULT
                                                .withDelimiter(':')
                                                .parse(new StringReader(string));
                                        for (CSVRecord innerKVPair : innerString) {
                                            if (innerKVPair.size() == 2) {
                                                String key = innerKVPair.get(0).trim();
                                                String value = innerKVPair.get(1).trim();
                                                if ("name".equals(key)) {
                                                    name = value;
                                                }
                                                headers.put(key, value);
                                            }
                                        }
                                    }
                                }
                                if (name != null) {
                                    formMap.put(name, headers);
                                }
                            }
                        }
                    }
                    String filename = SwaggerOperationController.extractFilenameFromHeaders(headers);
                    if (filename != null) {
                        try {
                            File file = new File(Files.createTempDir(), filename);
                            file.deleteOnExit();
                            file.getParentFile().deleteOnExit();
                            FileOutputStream fo = new FileOutputStream(file);
                            multipartStream.readBodyData(fo);
                            inputStreams.put(name, file);
                        } catch (Exception e) {
                            LOGGER.error("Failed to extract uploaded file", e);
                        }
                    } else {
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        multipartStream.readBodyData(bo);
                        String value = bo.toString();
                        headers.put(name, value);
                    }
                    if (name != null) {
                        formMap.put(name, headers);
                    }
                    headers = new HashMap<>();
                    name = null;
                    nextPart = multipartStream.readBoundary();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
