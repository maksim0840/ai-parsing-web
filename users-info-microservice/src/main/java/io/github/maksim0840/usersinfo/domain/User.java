package io.github.maksim0840.usersinfo.domain;

import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)  // добавляем слушателя жизненного цикла JPA-сущности (для автоматического проставления CreatedDate)
@Table(name = "users",
        indexes = {
                // составной индекс ("role, created_at")
                @Index(name = "idx_users_role_created_at", columnList = "role, created_at"),
                // отдельный индекс для "created_at"
                @Index(name = "idx_users_created_at", columnList = "created_at")
                // отдельный индекс для "role" не требуется, т.к. его заменяет leftmost prefix из составного ("role, created_at")
        }
)
@Getter
@Setter
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToMany(mappedBy = "user")
    private List<ParsingParam> parsingParams;

    private String name;
    @Column(name = "password_hash")     // названия колонок с составными именами лучше указать явно
    private String passwordHash;
    @Enumerated(EnumType.STRING)        // сохранить enum в виде строки с именем константы
    private UserRole role;
    @CreatedDate                        // автозаполнение даты при сохранении
    @Column(name = "created_at")
    private Instant createdAt;

    public User() {}

    public User(String name, String passwordHash, io.github.maksim0840.internalapi.user.v1.enums.UserRole role) {
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public User(Long id, List<ParsingParam> parsingParams, String name, String passwordHash, UserRole role, Instant createdAt) {
        this.id = id;
        this.parsingParams = parsingParams;
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }
}
