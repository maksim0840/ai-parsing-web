package io.github.maksim0840.internalapi.parsing_param.v1.mapper;


import io.github.maksim0840.internalapi.parsing_param.v1.dto.LLMParamsDTO;
import io.github.maksim0840.parsing_param.v1.LLMParamsProto;

public class LLMParamsProtoMapper {
    public static LLMParamsProto dtoToProto(LLMParamsDTO dto) {
        LLMParamsProto.Builder protoBuilder = LLMParamsProto.newBuilder();
        protoBuilder.setModelName(dto.modelName() != null ? dto.modelName() : "");
        protoBuilder.setSystemMessage(dto.systemMessage() != null ? dto.systemMessage() : "");
        protoBuilder.setUserMessage(dto.userMessage() != null ? dto.userMessage() : "");
        if (dto.temperature() != null) protoBuilder.setTemperature(dto.temperature());
        if (dto.maxOutputTokens() != null) protoBuilder.setMaxOutputTokens(dto.maxOutputTokens());
        return protoBuilder.build();
    }

    public static LLMParamsDTO protoToDto(LLMParamsProto proto) {
        return LLMParamsDTO.builder()
                .modelName(proto.getModelName())
                .systemMessage(proto.getSystemMessage())
                .userMessage(proto.getUserMessage())
                .temperature(proto.hasTemperature() ? proto.getTemperature() : null)
                .maxOutputTokens(proto.hasMaxOutputTokens() ? proto.getMaxOutputTokens() : null)
                .build();
    }
}