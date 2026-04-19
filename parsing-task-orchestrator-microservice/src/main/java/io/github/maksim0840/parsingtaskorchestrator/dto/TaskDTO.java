package io.github.maksim0840.parsingtaskorchestrator.dto;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import lombok.Builder;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.Map;

@Builder
public record TaskDTO(
        String id,
        boolean htmlParserRequired,
        HtmlParserRequestDTO htmlParserRequest,
        boolean htmlPreprocessingRequired,
        HtmlPreprocessingRequestDTO htmlPreprocessingRequest,
        boolean textRecognitionRequired,
        TextRecognitionRequestDTO textRecognitionRequest,
        HtmlParserResponseDTO htmlParserResponse,
        HtmlPreprocessingResponseDTO htmlPreprocessingResponse,
        TextRecognitionResponseDTO textRecognitionResponse,
        Instant createdAt
){
}
