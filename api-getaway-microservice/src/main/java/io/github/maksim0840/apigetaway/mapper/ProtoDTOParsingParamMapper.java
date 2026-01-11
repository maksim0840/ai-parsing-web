package io.github.maksim0840.apigetaway.mapper;

import io.github.maksim0840.apigetaway.dto.ParsingParamDTO;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.parsing_param.v1.ParsingParamProto;

public class ProtoDTOParsingParamMapper {

    public static ParsingParamProto dtoToProto(ParsingParamDTO dto) {
        return ParsingParamProto.newBuilder()
                .setId(dto.id())
                .setUserId(dto.userId())
                .setName(dto.name())
                .setDescription(dto.description())
                .setCreatedAt(ProtoTimeMapper.instantToTimestamp(dto.createdAt()))
                .build();
    }

    public static ParsingParamDTO protoToDto(ParsingParamProto proto) {
        return ParsingParamDTO.builder()
                .id(proto.getId())
                .userId(proto.getUserId())
                .name(proto.getName())
                .description(proto.getDescription())
                .createdAt(ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()))
                .build();
    }
}
