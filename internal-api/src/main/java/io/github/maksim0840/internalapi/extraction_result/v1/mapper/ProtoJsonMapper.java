package io.github.maksim0840.internalapi.extraction_result.v1.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;

import java.util.Map;

public class ProtoJsonMapper {

    private static final ObjectMapper OM = new ObjectMapper();

    public static Struct mapToStruct(Map<String, Object> map) {
        try {
            String json = OM.writeValueAsString(map);
            Struct.Builder structBuilder = Struct.newBuilder();
            JsonFormat.parser().merge(json, structBuilder);
            return structBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Object to Json", e);
        }
    }

    public static Map<String, Object> structToMap(Struct struct) {
        try {
            String json = JsonFormat.printer().print(struct);
            TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};
            return OM.readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Json to Object", e);
        }
    }
}
