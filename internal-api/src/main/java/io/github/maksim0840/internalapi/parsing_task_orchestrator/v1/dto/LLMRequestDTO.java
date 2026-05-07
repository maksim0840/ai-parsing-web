package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;

@Builder
@Nullable
public record LLMRequestDTO(
        String taskId,
        String modelName,
        String systemMessage,
        String userMessage,
        Double temperature,
        Integer maxOutputTokens
) {
    // Использовать значения этого же объекта, но с измененным полем userMessage
    public LLMRequestDTO withUserMessage(String newUserMessage) {
        return new LLMRequestDTO(
                this.taskId,
                this.modelName,
                this.systemMessage,
                newUserMessage,
                this.temperature,
                this.maxOutputTokens
        );
    }
}
