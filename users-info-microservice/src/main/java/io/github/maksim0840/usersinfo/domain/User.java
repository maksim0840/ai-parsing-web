package io.github.maksim0840.usersinfo.domain;

import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "users")
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
    private String passwordHash;
    @Enumerated(EnumType.STRING)    // сохранить enum в виде строки с именем константы
    private UserRole role;
    @CreatedDate                            // автозаполнение даты при сохранении
    private Instant createdAt;

    public User(String name, String passwordHash, io.github.maksim0840.internalapi.user.v1.enums.UserRole role) {
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public User() {}
}
