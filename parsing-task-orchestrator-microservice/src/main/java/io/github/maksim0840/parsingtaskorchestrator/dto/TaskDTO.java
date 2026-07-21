package io.github.maksim0840.parsingtaskorchestrator.dto;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record TaskDTO(
        String id,
        String userId,
        boolean htmlParserRequired,
        HtmlParserRequestDTO htmlParserRequest,
        boolean htmlPreprocessingRequired,
        HtmlPreprocessingRequestDTO htmlPreprocessingRequest,
        boolean textRecognitionRequired,
        TextRecognitionRequestDTO textRecognitionRequest,
        boolean llmRequired,
        LLMRequestDTO llmRequest,
        HtmlParserResponseDTO htmlParserResponse,
        HtmlPreprocessingResponseDTO htmlPreprocessingResponse,
        TextRecognitionResponseDTO textRecognitionResponse,
        LLMResponseDTO llmResponse,
        Instant createdAt,
        TaskStatus status,
        String message
){
}
