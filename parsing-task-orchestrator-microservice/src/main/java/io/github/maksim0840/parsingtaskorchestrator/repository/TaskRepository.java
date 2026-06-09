package io.github.maksim0840.parsingtaskorchestrator.repository;

import io.github.maksim0840.parsingtaskorchestrator.domain.Task;
import org.springframework.data.repository.CrudRepository;

public interface TaskRepository extends CrudRepository<Task, String> {
}
