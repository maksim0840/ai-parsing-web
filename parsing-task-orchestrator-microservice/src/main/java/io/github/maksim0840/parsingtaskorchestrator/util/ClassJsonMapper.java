package io.github.maksim0840.parsingtaskorchestrator.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ClassJsonMapper {
    private static final ObjectMapper OM = new ObjectMapper();

    public static Map<String, Object> classToMap(Object classObject) {
        return OM.convertValue(
                classObject,
                new TypeReference<Map<String, Object>>() {}
        );
    }
}
