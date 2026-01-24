package io.github.maksim0840.usersinfo.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

@Entity
@Table(name = "parsing_params")
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
