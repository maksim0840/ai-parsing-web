package io.github.maksim0840.extractionresults.mapper;

import io.github.maksim0840.extraction_result.v1.ExtractionResultProto;
import io.github.maksim0840.extractionresults.domain.ExtractionResult;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoJsonMapper;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;

public class ProtoDomainExtractionResultMapper {

    public static ExtractionResultProto domainToProto(ExtractionResult domain) {
        return ExtractionResultProto.newBuilder()
                .setId(domain.getId())
                .setUrl(domain.getUrl())
                .setUserId(domain.getUserId())
                .setJsonResult(ProtoJsonMapper.mapToStruct(domain.getJsonResult()))
                .setCreatedAt(ProtoTimeMapper.instantToTimestamp(domain.getCreatedAt()))
                .build();
    }

    public static ExtractionResult protoToDomain(ExtractionResultProto proto) {
        return ExtractionResult.builder()
                .id(proto.getId())
                .url(proto.getUrl())
                .userId(proto.getUserId())
                .jsonResult(ProtoJsonMapper.structToMap(proto.getJsonResult()))
                .createdAt(ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()))
                .build();
    }
}
