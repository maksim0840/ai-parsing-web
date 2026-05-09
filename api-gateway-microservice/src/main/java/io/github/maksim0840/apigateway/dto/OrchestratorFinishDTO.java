package io.github.maksim0840.apigateway.dto;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionResponseDTO;
import lombok.Builder;

@Builder
public record OrchestratorFinishDTO(
        String taskId,
        HtmlParserResponseDTO htmlParserResponse,
        HtmlPreprocessingResponseDTO htmlPreprocessingResponse,
        TextRecognitionResponseDTO textRecognitionResponse,
        LLMResponseDTO llmResponse
) {
}
