package io.swagger.inflector.controllers;

import io.swagger.inflector.examples.ExampleBuilder;
import io.swagger.inflector.examples.models.ArrayExample;
import io.swagger.inflector.examples.models.Example;
import io.swagger.inflector.examples.models.ObjectExample;
import io.swagger.inflector.models.RequestContext;
import io.swagger.inflector.models.ResponseContext;
import io.swagger.inflector.processors.EntityProcessor;
import io.swagger.inflector.processors.EntityProcessorFactory;
import io.swagger.models.properties.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by nielinjie on 9/12/16.
 */
class ResponseCreate {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCreate.class);
    private SwaggerOperationController controller = null;

    ResponseCreate(SwaggerOperationController swaggerOperationController) {
        this.controller = swaggerOperationController;
    }

    Response getResponse(RequestContext requestContext, Map<String, io.swagger.models.Response> responses) {
        String[] keys = new String[responses.keySet().size()];
        Arrays.sort(responses.keySet().toArray(keys));
        int code = 0;
        String defaultKey = null;
        for (String key : keys) {
            if (key.startsWith("2")) {
                defaultKey = key;
                code = Integer.parseInt(key);
                break;
            }
            if ("default".equals(key)) {
                defaultKey = key;
                code = 200;
                break;
            }
            if (key.startsWith("3")) {
                // we use the 3xx responses as defaults
                defaultKey = key;
                code = Integer.parseInt(key);
            }
        }
        //NOTE 根据default生成response，在此引入随机生成？
        if (defaultKey != null) {
            Response.ResponseBuilder builder = Response.status(code);
            io.swagger.models.Response response = responses.get(defaultKey);

            if (response.getHeaders() != null && response.getHeaders().size() > 0) {
                for (String key : response.getHeaders().keySet()) {
                    Property headerProperty = response.getHeaders().get(key);
                    Object output = ExampleBuilder.fromProperty(headerProperty, controller.getDefinitions());
                    if (output instanceof ArrayExample) {
                        output = ((ArrayExample) output).asString();
                    } else if (output instanceof ObjectExample) {
                        LOGGER.debug("not serializing output example, only primitives or arrays of primitives are supported");
                    } else {
                        output = ((Example) output).asString();
                    }
                    builder.header(key, output);
                }
            }

            Map<String, Object> examples = response.getExamples();
            if (examples != null) {
                for (MediaType mediaType : requestContext.getAcceptableMediaTypes()) {
                    for (String key : examples.keySet()) {
                        if (MediaType.valueOf(key).isCompatible(mediaType)) {
                            builder.entity(examples.get(key))
                                    .type(mediaType);

                            return builder.build();
                        }
                    }
                }
            }

            Object output = ExampleBuilder.fromProperty(response.getSchema(), controller.getDefinitions());
            //NOTE 转配到response里面去，比如设置contentType之类的。
            if (output != null) {
                ResponseContext resp = new ResponseContext().entity(output);
                //NOTE 从request找到一个可能的content type，set到responseContext里面。
                controller.setContentType(requestContext, resp, controller.getOperation());
                //builder的entity和responseContext的entity是何区别？
                //NOTE 删掉一遍，后面还有一遍，没明白，试一试。
//                builder.entity(output);
                if (resp.getContentType() != null) {
                    // this comes from the operation itself
                    builder.type(resp.getContentType());
                } else {
                    // get acceptable content types
                    List<EntityProcessor> processors = EntityProcessorFactory.getProcessors();

                    // take first compatible one
                    for (EntityProcessor processor : processors) {
                        for (MediaType mt : requestContext.getAcceptableMediaTypes()) {
                            LOGGER.debug("checking type " + mt.toString() + " against " + processor.getClass().getName());
                            if (processor.supports(mt)) {
                                builder.type(mt);
                                break;
                            }
                        }
                    }
                }

                //NOTE 为何要entity两遍？
                builder.entity(output);
            }
            return builder.build();
        } else {
            LOGGER.debug("no response type to map to, assume 200");
            code = 200;
        }
        return Response.status(code).build();
    }
}
