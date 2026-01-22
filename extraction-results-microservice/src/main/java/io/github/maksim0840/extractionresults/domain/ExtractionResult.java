package io.github.maksim0840.extractionresults.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Домен, описывающий сущность объекта в таблице базы данных MongoDB.
 * Предназначен для хранения информации об итоговом результате парсинга и анализа данных (поле jsonResult).
 */
@Document("extraction_results")
@CompoundIndexes({
        // Создаём составной индекс по возрастанию userId и убыванию createdAt
        @CompoundIndex(name = "user_created_idx", def = "{'userId': 1, 'createdAt': -1}")
})
@Getter
@Setter
@Builder
public class ExtractionResult {
    @Id
    private String id;                      // по умолчанию в MongoDB поле _id имеет тип ObjectId, который удобнее хранить в строке
    private String url;
    private String userId;
    private Map<String, Object> jsonResult; // результат парсинга и корректировки данных
    @CreatedDate                            // автозаполнение даты при сохранении
    private Instant createdAt;              // дата + часовой пояс

    public ExtractionResult() {}

    public ExtractionResult(String url, String userId, Map<String, Object> jsonResult) {
        this.url = url;
        this.userId = userId;
        this.jsonResult = jsonResult;
    }

    public ExtractionResult(String id, String url, String userId, Map<String, Object> jsonResult, Instant createdAt) {
        this.id = id;
        this.url = url;
        this.userId = userId;
        this.jsonResult = jsonResult;
        this.createdAt = createdAt;
    }
}
