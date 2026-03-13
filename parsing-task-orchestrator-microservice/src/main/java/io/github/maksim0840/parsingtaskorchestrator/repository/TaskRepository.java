package io.github.maksim0840.parsingtaskorchestrator.repository;

import io.github.maksim0840.parsingtaskorchestrator.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
