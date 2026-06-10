package io.github.maksim0840.parsingtaskorchestrator.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JsonMapper {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> objectToMap (Object classObject) {
        return mapper.convertValue(
                classObject,
                new TypeReference<Map<String, Object>>() {}
        );
    }

    public static <T> T mapToObject(Map<String, Object> json, Class<T> clazz) {
        return mapper.convertValue(json, clazz);
    }

    public static String objectToString(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    public static <T> T stringToObject(String json, Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(json, clazz);
    }
}
