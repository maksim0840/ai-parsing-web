package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMResponseDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.LLMResponseProto;

public class LLMResponseProtoMapper {

    public static LLMResponseProto dtoToProto(LLMResponseDTO dto) {
        return LLMResponseProto.newBuilder()
                .setTaskId(dto.taskId() != null ? dto.taskId() : "")
                .setSuccess(dto.success())
                .setMessage(dto.message() != null ? dto.message() : "")
                .setLlmOutput(dto.llmOutput() != null ? dto.llmOutput() : "")
                .build();
    }

    public static LLMResponseDTO protoToDto(LLMResponseProto proto) {
        return LLMResponseDTO.builder()
                .taskId(proto.getTaskId())
                .success(proto.getSuccess())
                .message(proto.getMessage())
                .llmOutput(proto.getLlmOutput())
                .build();
    }
}
