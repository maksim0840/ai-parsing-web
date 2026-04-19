package io.github.maksim0840.parsingtaskorchestrator.util;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.parsingtaskorchestrator.domain.Task;
import io.github.maksim0840.parsingtaskorchestrator.dto.TaskDTO;

import java.time.Instant;

public class TaskMapper {

    public static TaskDTO domainToDto(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .htmlParserRequired(task.isHtmlParserRequired())
                .htmlParserRequest(JsonMapper.mapToObject(
                        task.getJsonHtmlParserRequest(),
                        HtmlParserRequestDTO.class)
                )
                .htmlPreprocessingRequired(task.isHtmlPreprocessingRequired())
                .htmlPreprocessingRequest(JsonMapper.mapToObject(
                        task.getJsonHtmlPreprocessingRequest(),
                        HtmlPreprocessingRequestDTO.class)
                )
                .textRecognitionRequired(task.isTextRecognitionRequired())
                .textRecognitionRequest(JsonMapper.mapToObject(
                        task.getJsonTextRecognitionRequest(),
                        TextRecognitionRequestDTO.class)
                )
                .htmlParserResponse(JsonMapper.mapToObject(
                        task.getJsonHtmlParserResponse(),
                        HtmlParserResponseDTO.class)
                )
                .htmlPreprocessingResponse(JsonMapper.mapToObject(
                        task.getJsonHtmlPreprocessingResponse(),
                        HtmlPreprocessingResponseDTO.class)
                )
                .textRecognitionResponse(JsonMapper.mapToObject(
                        task.getJsonTextRecognitionResponse(),
                        TextRecognitionResponseDTO.class)
                )
                .createdAt(task.getCreatedAt())
                .build();
    }
}