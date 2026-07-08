package io.github.maksim0840.internalapi.extraction_result.v1.mapper;

import io.github.maksim0840.extraction_result.v1.ExtractionResultProto;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.dto.ExtractionResultDTO;

import java.util.Map;

public class ExtractionResultProtoMapper {

    public static ExtractionResultProto dtoToProto(ExtractionResultDTO dto) {
        return ExtractionResultProto.newBuilder()
                .setId(dto.id() != null ? dto.id() : "")
                .setUrl(dto.url() != null ? dto.url() : "")
                .setUserId(dto.userId() != null ? dto.userId() : "")
                .setResultFormat(ResultFormatProtoMapper.enumToProto(dto.resultFormat()))
                .setResult(dto.result() != null ? dto.result() : "")
                .setCreatedAt(ProtoTimeMapper.instantToTimestamp(dto.createdAt()))
                .build();
    }

    public static ExtractionResultDTO protoToDto(ExtractionResultProto proto) {
        return ExtractionResultDTO.builder()
                .id(proto.getId())
                .url(proto.getUrl())
                .userId(proto.getUserId())
                .resultFormat(ResultFormatProtoMapper.protoToEnum(proto.getResultFormat()))
                .result(proto.getResult())
                .createdAt(ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()))
                .build();
    }
}
