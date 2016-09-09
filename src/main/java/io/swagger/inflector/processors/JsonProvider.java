package io.swagger.inflector.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.util.Json;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonProvider implements ContextResolver<ObjectMapper> {
    private final ObjectMapper objectMapper;

    public JsonProvider() {
        objectMapper = Json.mapper();
    };

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}