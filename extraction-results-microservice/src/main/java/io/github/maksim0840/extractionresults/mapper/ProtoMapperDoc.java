package io.github.maksim0840.extractionresults.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.maksim0840.extraction_result.v1.ExtractionResultProto;
import io.github.maksim0840.extractionresults.domain.ExtractionResult;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoJsonMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoTimeMapper;

public class ProtoMapperDoc {

    private static final ObjectMapper OM = new ObjectMapper();

    public static ExtractionResultProto docToProto(ExtractionResult doc) {
        return ExtractionResultProto.newBuilder()
                .setId(doc.getId())
                .setUrl(doc.getUrl())
                .setUserId(doc.getUserId())
                .setJsonResult(ProtoJsonMapper.mapToStruct(doc.getJsonResult()))
                .setCreatedAt(ProtoTimeMapper.instantToTimestamp(doc.getCreatedAt()))
                .build();
    }

    public static ExtractionResult protoToDoc(ExtractionResultProto proto) {
        return ExtractionResult.builder()
                .id(proto.getId())
                .url(proto.getUrl())
                .userId(proto.getUserId())
                .jsonResult(ProtoJsonMapper.structToMap(proto.getJsonResult()))
                .createdAt(ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()))
                .build();
    }
}
