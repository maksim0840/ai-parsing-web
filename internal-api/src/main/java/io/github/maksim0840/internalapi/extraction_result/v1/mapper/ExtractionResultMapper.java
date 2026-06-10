package io.github.maksim0840.internalapi.extraction_result.v1.mapper;

import io.github.maksim0840.extraction_result.v1.ExtractionResultProto;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;

public class ExtractionResultMapper {

    public static ExtractionResultProto dtoToProto(ExtractionResultDTO dto) {
        return ExtractionResultProto.newBuilder()
                .setId(dto.id())
                .setUrl(dto.url())
                .setUserId(dto.userId())
                .setJsonResult(ProtoJsonMapper.mapToStruct(dto.jsonResult()))
                .setCreatedAt(ProtoTimeMapper.instantToTimestamp(dto.createdAt()))
                .build();
    }

    public static ExtractionResultDTO protoToDto(ExtractionResultProto proto) {
        return ExtractionResultDTO.builder()
                .id(proto.getId())
                .url(proto.getUrl())
                .userId(proto.getUserId())
                .jsonResult(ProtoJsonMapper.structToMap(proto.getJsonResult()))
                .createdAt(ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()))
                .build();
    }
}
