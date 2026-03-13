package io.github.maksim0840.parsingtaskorchestrator.service;

import io.github.maksim0840.parsingtaskorchestrator.repository.TaskRepository;

public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void addTask() {}
}
