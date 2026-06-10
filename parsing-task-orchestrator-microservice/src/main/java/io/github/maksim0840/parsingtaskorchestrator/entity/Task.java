package io.github.maksim0840.parsingtaskorchestrator.entity;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import io.github.maksim0840.parsingtaskorchestrator.entity.model.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.Instant;

@RedisHash(value = "tasks", timeToLive = 1800) // запись храниться 30 минут
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    private String id;

    private boolean htmlParserRequired;
    private HtmlParserRequest htmlParserRequest;

    private boolean htmlPreprocessingRequired;
    private HtmlPreprocessingRequest htmlPreprocessingRequest;

    private boolean textRecognitionRequired;
    private TextRecognitionRequest textRecognitionRequest;

    private boolean llmRequired;
    private LLMRequest llmRequest;

    private HtmlParserResponse htmlParserResponse;
    private HtmlPreprocessingResponse htmlPreprocessingResponse;
    private TextRecognitionResponse textRecognitionResponse;
    private LLMResponse llmResponse;

    private Instant createdAt;

    private TaskStatus status;
    String message;


    public Task(String id,
                boolean htmlParserRequired,
                HtmlParserRequest htmlParserRequest,
                boolean htmlPreprocessingRequired,
                HtmlPreprocessingRequest htmlPreprocessingRequest,
                boolean textRecognitionRequired,
                TextRecognitionRequest textRecognitionRequest,
                boolean llmRequired,
                LLMRequest llmRequest) {

        this.id = id;
        this.htmlParserRequired = htmlParserRequired;
        this.htmlParserRequest = htmlParserRequest;
        this.htmlPreprocessingRequired = htmlPreprocessingRequired;
        this.htmlPreprocessingRequest = htmlPreprocessingRequest;
        this.textRecognitionRequired = textRecognitionRequired;
        this.textRecognitionRequest = textRecognitionRequest;
        this.llmRequired = llmRequired;
        this.llmRequest = llmRequest;
        this.htmlParserResponse = new HtmlParserResponse();
        this.htmlPreprocessingResponse = new HtmlPreprocessingResponse();
        this.textRecognitionResponse = new TextRecognitionResponse();
        this.llmResponse = new LLMResponse();
        this.createdAt = Instant.now();
        this.status = TaskStatus.CREATED;
        this.message = "";
    }
}
