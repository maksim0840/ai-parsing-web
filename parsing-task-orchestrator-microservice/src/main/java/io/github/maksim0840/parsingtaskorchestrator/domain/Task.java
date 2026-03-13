package io.github.maksim0840.parsingtaskorchestrator.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Task {
    @Id
    private Long id;
    private TaskStatus status;
}
