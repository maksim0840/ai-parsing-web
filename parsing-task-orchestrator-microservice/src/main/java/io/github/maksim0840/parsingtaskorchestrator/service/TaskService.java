package io.github.maksim0840.parsingtaskorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
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
                           TextRecognitionRequestDTO textRecognitionRequest,
                           LLMRequestDTO llmRequest) {

        boolean htmlParserRequired = (htmlParserRequest != null);
        boolean htmlPreprocessingRequired = (htmlPreprocessingRequest != null);
        boolean textRecognitionRequired = (textRecognitionRequest != null);
        boolean llmRequired = (llmRequest != null);

        Task task = new Task(
                taskId,
                htmlParserRequired,
                htmlParserRequired ? JsonMapper.objectToMap(htmlParserRequest) : Map.of(),
                htmlPreprocessingRequired,
                htmlPreprocessingRequired ? JsonMapper.objectToMap(htmlPreprocessingRequest) : Map.of(),
                textRecognitionRequired,
                textRecognitionRequired ? JsonMapper.objectToMap(textRecognitionRequest) : Map.of(),
                llmRequired,
                llmRequired ? JsonMapper.objectToMap(llmRequest) : Map.of()
        );
        return TaskMapper.domainToDto(taskRepository.save(task));
    }

    public boolean isTaskExists(String taskId) {
        return taskRepository.existsById(taskId);
    }

    public TaskDTO getTask(String taskId) {
        Task task = findTaskById(taskId);
        return TaskMapper.domainToDto(task);
    }

    public TaskDTO setHtmlParserRequest(String taskId, HtmlParserRequestDTO htmlParserRequest) {
        Task task = findTaskById(taskId);
        task.setJsonHtmlParserRequest(JsonMapper.objectToMap(htmlParserRequest));
        taskRepository.save(task);
        return TaskMapper.domainToDto(task);
    }

    public TaskDTO setHtmlPreprocessingRequest(String taskId, HtmlPreprocessingRequestDTO htmlPreprocessingRequest) {
        Task task = findTaskById(taskId);
        task.setJsonHtmlPreprocessingRequest(JsonMapper.objectToMap(htmlPreprocessingRequest));
        taskRepository.save(task);
        return TaskMapper.domainToDto(task);
    }

    public TaskDTO setTextRecognitionRequest(String taskId, TextRecognitionRequestDTO textRecognitionRequest) {
        Task task = findTaskById(taskId);
        task.setJsonTextRecognitionRequest(JsonMapper.objectToMap(textRecognitionRequest));
        taskRepository.save(task);
        return TaskMapper.domainToDto(task);
    }

    public TaskDTO setLLMRequest(String taskId, LLMRequestDTO llmRequest) {
        Task task = findTaskById(taskId);
        task.setJsonLLMRequest(JsonMapper.objectToMap(llmRequest));
        taskRepository.save(task);
        return TaskMapper.domainToDto(task);
    }

    public TaskDTO setHtmlParserResponse(String taskId, HtmlParserResponseDTO htmlParserResponse) {
        Task task = findTaskById(taskId);
        task.setJsonHtmlParserResponse(JsonMapper.objectToMap(htmlParserResponse));
        taskRepository.save(task);
        return TaskMapper.domainToDto(task);
    }

    public TaskDTO setHtmlPreprocessingResponse(String taskId, HtmlPreprocessingResponseDTO htmlPreprocessingResponse) {
        Task task = findTaskById(taskId);
        task.setJsonHtmlPreprocessingResponse(JsonMapper.objectToMap(htmlPreprocessingResponse));
        taskRepository.save(task);
        return TaskMapper.domainToDto(task);
    }

    public TaskDTO setTextRecognitionResponse(String taskId, TextRecognitionResponseDTO textRecognitionResponse) {
        Task task = findTaskById(taskId);
        task.setJsonTextRecognitionResponse(JsonMapper.objectToMap(textRecognitionResponse));
        taskRepository.save(task);
        return TaskMapper.domainToDto(task);
    }

    public TaskDTO setLLMResponse(String taskId, LLMResponseDTO llmResponse) {
        Task task = findTaskById(taskId);
        task.setJsonLLMResponse(JsonMapper.objectToMap(llmResponse));
        taskRepository.save(task);
        return TaskMapper.domainToDto(task);
    }

    public TaskDTO setStatusAndMessage(String taskId, TaskStatus status, String message) {
        Task task = findTaskById(taskId);
        task.setStatus(status);
        task.setMessage(message);
        taskRepository.save(task);
        return TaskMapper.domainToDto(task);
    }

    private Task findTaskById(String taskId) {
        return taskRepository.findById(taskId).orElseThrow(
                () -> new TaskNotFoundException("task not found")
        );
    }
}