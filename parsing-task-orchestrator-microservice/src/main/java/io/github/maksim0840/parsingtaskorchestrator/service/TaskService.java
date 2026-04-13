package io.github.maksim0840.parsingtaskorchestrator.service;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.parsingtaskorchestrator.domain.Task;
import io.github.maksim0840.parsingtaskorchestrator.repository.TaskRepository;
import io.github.maksim0840.parsingtaskorchestrator.util.ClassJsonMapper;

import java.util.Map;
import java.util.Optional;

public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void addTask(String taskId,
                        HtmlParserRequestDTO htmlParserRequest,
                        HtmlPreprocessingRequestDTO htmlPreprocessingRequest,
                        TextRecognitionRequestDTO textRecognitionRequest) {

        boolean htmlParserRequired = (htmlParserRequest != null);
        boolean htmlPreprocessingRequired = (htmlPreprocessingRequest != null);
        boolean textRecognitionRequired = (textRecognitionRequest != null);

        Task task = new Task(
                taskId,
                htmlParserRequired,
                htmlParserRequired ? ClassJsonMapper.classToMap(htmlParserRequest) : Map.of(),
                htmlPreprocessingRequired,
                htmlPreprocessingRequired ? ClassJsonMapper.classToMap(htmlPreprocessingRequest) : Map.of(),
                textRecognitionRequired,
                textRecognitionRequired ? ClassJsonMapper.classToMap(textRecognitionRequest) : Map.of()
        );
        taskRepository.save(task);
    }

    public Optional<Task> getTask(String taskId) {
        return taskRepository.findById(taskId);
    }
}