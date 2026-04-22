package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;

@Builder
@Nullable
public record LLMResponseDTO(
        String taskId,
        String llmOutput
) {
}
