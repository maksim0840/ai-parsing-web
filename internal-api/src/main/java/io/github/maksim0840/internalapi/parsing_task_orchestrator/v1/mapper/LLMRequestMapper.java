package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;


import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMRequestDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.HtmlParserRequestProto;
import io.github.maksim0840.parsing_task_orchestrator.v1.LLMRequestProto;

public class LLMRequestMapper {
    public static LLMRequestProto dtoToProto(LLMRequestDTO dto) {
        LLMRequestProto.Builder protoBuilder = LLMRequestProto.newBuilder();
        protoBuilder.setTaskId(dto.taskId());
        protoBuilder.setModelName(dto.modelName());
        protoBuilder.setSystemMessage(dto.systemMessage());
        protoBuilder.setUserMessage(dto.userMessage());
        if (dto.temperature() != null) protoBuilder.setTemperature(dto.temperature());
        if (dto.maxOutputTokens() != null) protoBuilder.setMaxOutputTokens(dto.maxOutputTokens());
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
                .build();
    }
}
