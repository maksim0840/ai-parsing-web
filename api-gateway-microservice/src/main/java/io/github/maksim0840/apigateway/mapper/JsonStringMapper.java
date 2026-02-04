package io.github.maksim0840.apigateway.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JsonStringMapper {
    private static final ObjectMapper OM = new ObjectMapper();

    public static Map<String, Object> stringToMap(String json) {
        try {
            return OM.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON string", e);
        }
    }
}
