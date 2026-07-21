package io.github.maksim0840.internalapi.parsing_param.v1.mapper;

import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.parsing_param.v1.dto.ParsingParamDTO;
import io.github.maksim0840.parsing_param.v1.ParsingParamProto;

public class ParsingParamProtoMapper {

    public static ParsingParamProto dtoToProto(ParsingParamDTO dto) {
        return ParsingParamProto.newBuilder()
                .setId(dto.id())
                .setUserId(dto.userId())
                .setName(dto.name() != null ? dto.name() : "")
                .setCreatedAt(ProtoTimeMapper.instantToTimestamp(dto.createdAt()))
                .setHtmlParserParams(HtmlParserParamsProtoMapper.dtoToProto(dto.htmlParserParams()))
                .setHtmlPreprocessingParams(HtmlPreprocessingParamsProtoMapper.dtoToProto(dto.htmlPreprocessingParams()))
                .setLlmParams(LLMParamsProtoMapper.dtoToProto(dto.llmParams()))
                .build();
    }

    public static ParsingParamDTO protoToDto(ParsingParamProto proto) {
        return ParsingParamDTO.builder()
                .id(proto.getId())
                .userId(proto.getUserId())
                .name(proto.getName())
                .createdAt(ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()))
                .htmlParserParams(HtmlParserParamsProtoMapper.protoToDto(proto.getHtmlParserParams()))
                .htmlPreprocessingParams(HtmlPreprocessingParamsProtoMapper.protoToDto(proto.getHtmlPreprocessingParams()))
                .llmParams(LLMParamsProtoMapper.protoToDto(proto.getLlmParams()))
                .build();
    }
}


