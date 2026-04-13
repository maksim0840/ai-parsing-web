package io.github.maksim0840.parsingtaskorchestrator.repository;

import io.github.maksim0840.parsingtaskorchestrator.domain.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskRepository extends MongoRepository<Task, String> {
}
