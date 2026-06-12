package io.github.maksim0840.usersinfo.entity;

import io.github.maksim0840.internalapi.user.v1.dto.HtmlPreprocessingParamsDTO;
import io.github.maksim0840.usersinfo.entity.model.HtmlParserParams;
import io.github.maksim0840.usersinfo.entity.model.HtmlPreprocessingParams;
import io.github.maksim0840.usersinfo.entity.model.LLMParams;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)  // добавляем слушателя жизненного цикла JPA-сущности (для автоматического проставления CreatedDate)
@Table(name = "parsing_params",
        indexes = {
                // составной индекс ("user_id, created_at")
                @Index(name = "idx_parsing_params_user_created_at", columnList = "user_id, created_at"),
                // отдельный индекс для "created_at"
                @Index(name = "idx_parsing_params_created_at", columnList = "created_at")
                // отдельный индекс для "user_id" не требуется, т.к. его заменяет leftmost prefix из составного ("user_id, created_at")
        }
)
@Getter
@Setter
@Builder
public class ParsingParam {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    private String name;

    @CreatedDate                            // автозаполнение даты при сохранении
    @Column(name = "created_at")            // названия колонок с составными именами лучше указать явно
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)            // храним как jsonb
    @Column(name = "html_parser_params", columnDefinition = "jsonb")
    HtmlParserParams htmlParserParams;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "html_preprocessing_params", columnDefinition = "jsonb")
    HtmlPreprocessingParams htmlPreprocessingParams;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "llm_params", columnDefinition = "jsonb")
    LLMParams llmParams;

    public ParsingParam(User user, String name, HtmlParserParams htmlParserParams, HtmlPreprocessingParams htmlPreprocessingParams, LLMParams llmParams) {
        this.user = user;
        this.name = name;
        this.htmlParserParams = htmlParserParams;
        this.htmlPreprocessingParams = htmlPreprocessingParams;
        this.llmParams = llmParams;
    }
}
