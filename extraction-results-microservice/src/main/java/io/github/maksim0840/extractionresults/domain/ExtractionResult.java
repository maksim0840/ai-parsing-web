package io.github.maksim0840.extractionresults.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;


@Document("extraction_results")
@CompoundIndexes({
        // Создаём составной индекс по возрастанию userId и убыванию createdAt
        @CompoundIndex(name = "user_created_idx", def = "{'userId': 1, 'createdAt': -1}")
})
@Getter
@Setter
public class ExtractionResult {
    @Id
    private String id;                      // по умолчанию в MongoDB поле _id имеет тип ObjectId (12 байт), который удобнее хранить в строке
    private String url;
    private String userId;
    private Map<String, Object> jsonResult; // результат парсинга и корректировки данных (можно искать/фильтровать по полям)
    @CreatedDate                            // автозаполнение даты при сохранении
    private Instant createdAt;              // дата + часовой пояс

    public ExtractionResult() {}

    public ExtractionResult(String url, String userId, Map<String, Object> jsonResult) {
        this.url = url;
        this.userId = userId;
        this.jsonResult = jsonResult;
    }
}
