package io.github.maksim0840.parsingtaskorchestrator.domain;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    private String id;

    private boolean htmlParserRequired;
    private Map<String, Object> jsonHtmlParserRequest;

    private boolean htmlPreprocessingRequired;
    private Map<String, Object> jsonHtmlPreprocessingRequest;

    private boolean textRecognitionRequired;
    private Map<String, Object> jsonTextRecognitionRequest;

    private boolean llmRequired;
    private Map<String, Object> jsonLLMRequest;

    private Map<String, Object> jsonHtmlParserResponse;
    private Map<String, Object> jsonHtmlPreprocessingResponse;
    private Map<String, Object> jsonTextRecognitionResponse;
    private Map<String, Object> jsonLLMResponse;

    // Запись удалиться через 30 минут (time to live)
    @Indexed(expireAfter = "30m")
    private Instant createdAt;

    private TaskStatus status;
    String message;


    public Task(String id,
                boolean htmlParserRequired,
                Map<String, Object> jsonHtmlParserRequest,
                boolean htmlPreprocessingRequired,
                Map<String, Object> jsonHtmlPreprocessingRequest,
                boolean textRecognitionRequired,
                Map<String, Object> jsonTextRecognitionRequest,
                boolean llmRequired,
                Map<String, Object> jsonLLMRequest) {

        this.id = id;
        this.htmlParserRequired = htmlParserRequired;
        this.jsonHtmlParserRequest = jsonHtmlParserRequest;
        this.htmlPreprocessingRequired = htmlPreprocessingRequired;
        this.jsonHtmlPreprocessingRequest = jsonHtmlPreprocessingRequest;
        this.textRecognitionRequired = textRecognitionRequired;
        this.jsonTextRecognitionRequest = jsonTextRecognitionRequest;
        this.llmRequired = llmRequired;
        this.jsonLLMRequest = jsonLLMRequest;
        this.jsonHtmlParserResponse = Map.of();
        this.jsonHtmlPreprocessingResponse = Map.of();
        this.jsonTextRecognitionResponse = Map.of();
        this.jsonLLMResponse = Map.of();
        this.createdAt = Instant.now();
        this.status = TaskStatus.CREATED;
        this.message = "";
    }
}
