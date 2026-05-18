package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;


import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMRequestDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.HtmlParserRequestProto;
import io.github.maksim0840.parsing_task_orchestrator.v1.LLMRequestProto;

import java.util.List;
import java.util.Map;

public class LLMRequestMapper {
    public static LLMRequestProto dtoToProto(LLMRequestDTO dto) {
        LLMRequestProto.Builder protoBuilder = LLMRequestProto.newBuilder();
        protoBuilder.setTaskId(dto.taskId() != null ? dto.taskId() : "");
        protoBuilder.setModelName(dto.modelName() != null ? dto.modelName() : "");
        protoBuilder.setSystemMessage(dto.systemMessage() != null ? dto.systemMessage() : "");
        protoBuilder.setUserMessage(dto.userMessage() != null ? dto.userMessage() : "");
        if (dto.temperature() != null) protoBuilder.setTemperature(dto.temperature());
        if (dto.maxOutputTokens() != null) protoBuilder.setMaxOutputTokens(dto.maxOutputTokens());
        protoBuilder.addAllHtmlDocs(dto.htmlDocs() != null ? FileInfoMapper.dtoToProtoList(dto.htmlDocs()) : List.of());
        protoBuilder.addAllImages(dto.images() != null ? FileInfoMapper.dtoToProtoList(dto.images()) : List.of());
        return protoBuilder.build();
    }

    public static LLMRequestDTO protoToDto(LLMRequestProto proto) {
        return LLMRequestDTO.builder()
                .taskId(proto.getTaskId())
                .modelName(proto.getModelName())
                .systemMessage(proto.getSystemMessage())
                .userMessage(proto.getUserMessage())
                .temperature(proto.hasTemperature() ? proto.getTemperature() : null)
                .maxOutputTokens(proto.hasMaxOutputTokens() ? proto.getMaxOutputTokens() : null)
                .htmlDocs(FileInfoMapper.protoToDtoList(proto.getHtmlDocsList()))
                .images(FileInfoMapper.protoToDtoList(proto.getImagesList()))
                .build();
    }
}
