package io.github.maksim0840.parsingtaskorchestrator.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("tasks")
@Getter
@Setter
@Builder
public class Task {
    @Id
    private String id;

    private boolean htmlParserRequired;
    private Map<String, Object> jsonHtmlParserRequest;

    private boolean htmlPreprocessingRequired;
    private Map<String, Object> jsonHtmlPreprocessingRequest;

    private boolean textRecognitionRequired;
    private Map<String, Object> jsonTextRecognitionRequest;

    // Запись удалиться через 30 минут (time to live)
    @Indexed(expireAfter = "30m")
    private Instant createdAt;


    public Task(String id,
                boolean htmlParserRequired,
                Map<String, Object> jsonHtmlParserRequest,
                boolean htmlPreprocessingRequired,
                Map<String, Object> jsonHtmlPreprocessingRequest,
                boolean textRecognitionRequired,
                Map<String, Object> jsonTextRecognitionRequest) {
        this.id = id;
        this.htmlParserRequired = htmlParserRequired;
        this.jsonHtmlParserRequest = jsonHtmlParserRequest;
        this.htmlPreprocessingRequired = htmlPreprocessingRequired;
        this.jsonHtmlPreprocessingRequest = jsonHtmlPreprocessingRequest;
        this.textRecognitionRequired = textRecognitionRequired;
        this.jsonTextRecognitionRequest = jsonTextRecognitionRequest;
        this.createdAt = Instant.now();
    }
}
