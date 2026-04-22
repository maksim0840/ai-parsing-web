package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionResponseDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.LLMRequestProto;
import io.github.maksim0840.parsing_task_orchestrator.v1.LLMResponseProto;
import io.github.maksim0840.parsing_task_orchestrator.v1.TextRecognitionResponseProto;

public class LLMResponseMapper {

    public static LLMResponseProto dtoToProto(LLMResponseDTO dto) {
        return LLMResponseProto.newBuilder()
                .setTaskId(dto.taskId())
                .setLlmOutput(dto.llmOutput())
                .build();
    }

    public static LLMResponseDTO protoToDto(LLMResponseProto proto) {
        return LLMResponseDTO.builder()
                .taskId(proto.getTaskId())
                .llmOutput(proto.getLlmOutput())
                .build();
    }
}
