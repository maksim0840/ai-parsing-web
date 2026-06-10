package io.github.maksim0840.parsingtaskorchestrator.mapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.parsingtaskorchestrator.entity.Task;
import io.github.maksim0840.parsingtaskorchestrator.entity.model.*;
import io.github.maksim0840.parsingtaskorchestrator.dto.TaskDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    Task toEntity(TaskDTO dto);
    TaskDTO toDto(Task entity);

    HtmlParserRequest toEntity(HtmlParserRequestDTO dto);
    HtmlParserRequestDTO toDto(HtmlParserRequest entity);

    HtmlPreprocessingRequest toEntity(HtmlPreprocessingRequestDTO dto);
    HtmlPreprocessingRequestDTO toDto(HtmlPreprocessingRequest entity);

    TextRecognitionRequest toEntity(TextRecognitionRequestDTO dto);
    TextRecognitionRequestDTO toDto(TextRecognitionRequest entity);

    LLMRequest toEntity(LLMRequestDTO dto);
    LLMRequestDTO toDto(LLMRequest entity);

    HtmlParserResponse toEntity(HtmlParserResponseDTO dto);
    HtmlParserResponseDTO toDto(HtmlParserResponse entity);

    HtmlPreprocessingResponse toEntity(HtmlPreprocessingResponseDTO dto);
    HtmlPreprocessingResponseDTO toDto(HtmlPreprocessingResponse entity);

    TextRecognitionResponse toEntity(TextRecognitionResponseDTO dto);
    TextRecognitionResponseDTO toDto(TextRecognitionResponse entity);

    LLMResponse toEntity(LLMResponseDTO dto);
    LLMResponseDTO toDto(LLMResponse entity);
}
