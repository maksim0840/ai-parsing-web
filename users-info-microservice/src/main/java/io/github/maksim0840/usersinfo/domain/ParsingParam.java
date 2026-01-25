package io.github.maksim0840.usersinfo.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
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
    private String description;
    @CreatedDate                            // автозаполнение даты при сохранении
    @Column(name = "created_at")            // названия колонок с составными именами лучше указать явно
    private Instant createdAt;

    public ParsingParam() {}

    public ParsingParam(User user, String name, String description) {
        this.user = user;
        this.name = name;
        this.description = description;
    }

    public ParsingParam(Long id, User user, String name, String description, Instant createdAt) {
        this.id = id;
        this.user = user;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }
}
