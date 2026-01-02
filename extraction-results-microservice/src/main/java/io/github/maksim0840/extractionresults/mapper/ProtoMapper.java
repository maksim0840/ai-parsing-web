package io.github.maksim0840.extractionresults.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import io.github.maksim0840.extraction_result.v1.ExtractionResultProto;
import io.github.maksim0840.extractionresults.domain.ExtractionResult;

import java.time.Instant;
import java.util.Map;

public class ProtoMapper {

    private static final ObjectMapper OM = new ObjectMapper();

    public static ExtractionResultProto docToProto(ExtractionResult doc) {
        return ExtractionResultProto.newBuilder()
                .setId(doc.getId())
                .setUrl(doc.getUrl())
                .setUserId(doc.getUserId())
                .setJsonResult(mapToStruct(doc.getJsonResult()))
                .setCreatedAt(instantToTimestamp(doc.getCreatedAt()))
                .build();
    }

    public static Timestamp instantToTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public static Instant timestampToInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

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
