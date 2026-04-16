package io.github.maksim0840.parsingtaskorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.parsingtaskorchestrator.domain.Task;
import io.github.maksim0840.parsingtaskorchestrator.dto.TaskDTO;
import io.github.maksim0840.parsingtaskorchestrator.exception.TaskNotFoundException;
import io.github.maksim0840.parsingtaskorchestrator.repository.TaskRepository;
import io.github.maksim0840.parsingtaskorchestrator.util.JsonMapper;
import io.github.maksim0840.parsingtaskorchestrator.util.TaskMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskDTO addTask(String taskId,
                        HtmlParserRequestDTO htmlParserRequest,
                        HtmlPreprocessingRequestDTO htmlPreprocessingRequest,
                        TextRecognitionRequestDTO textRecognitionRequest) {

        boolean htmlParserRequired = (htmlParserRequest != null);
        boolean htmlPreprocessingRequired = (htmlPreprocessingRequest != null);
        boolean textRecognitionRequired = (textRecognitionRequest != null);

        Task task = new Task(
                taskId,
                htmlParserRequired,
                htmlParserRequired ? JsonMapper.objectToMap(htmlParserRequest) : Map.of(),
                htmlPreprocessingRequired,
                htmlPreprocessingRequired ? JsonMapper.objectToMap(htmlPreprocessingRequest) : Map.of(),
                textRecognitionRequired,
                textRecognitionRequired ? JsonMapper.objectToMap(textRecognitionRequest) : Map.of()
        );
        return TaskMapper.domainToDto(taskRepository.save(task));
    }

    public TaskDTO getTask(String taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow(
                () -> new TaskNotFoundException("task not found")
        );
        return TaskMapper.domainToDto(task);
    }
}