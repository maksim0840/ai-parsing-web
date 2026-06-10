package io.github.maksim0840.parsingtaskorchestrator.service;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import io.github.maksim0840.parsingtaskorchestrator.entity.Task;
import io.github.maksim0840.parsingtaskorchestrator.entity.model.HtmlParserRequest;
import io.github.maksim0840.parsingtaskorchestrator.entity.model.HtmlPreprocessingRequest;
import io.github.maksim0840.parsingtaskorchestrator.entity.model.LLMRequest;
import io.github.maksim0840.parsingtaskorchestrator.entity.model.TextRecognitionRequest;
import io.github.maksim0840.parsingtaskorchestrator.dto.TaskDTO;
import io.github.maksim0840.parsingtaskorchestrator.exception.TaskNotFoundException;
import io.github.maksim0840.parsingtaskorchestrator.mapper.TaskMapper;
import io.github.maksim0840.parsingtaskorchestrator.repository.TaskRepository;

import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
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
                htmlParserRequired ? taskMapper.toEntity(htmlParserRequest) : new HtmlParserRequest(),
                htmlPreprocessingRequired,
                htmlPreprocessingRequired ? taskMapper.toEntity(htmlPreprocessingRequest) : new HtmlPreprocessingRequest(),
                textRecognitionRequired,
                textRecognitionRequired ? taskMapper.toEntity(textRecognitionRequest) : new TextRecognitionRequest(),
                llmRequired,
                llmRequired ? taskMapper.toEntity(llmRequest) : new LLMRequest()
        );
        return taskMapper.toDto(taskRepository.save(task));
    }

    public boolean isTaskExists(String taskId) {
        return taskRepository.existsById(taskId);
    }

    public TaskDTO getTask(String taskId) {
        Task task = findTaskById(taskId);
        return taskMapper.toDto(task);
    }

    public TaskDTO setHtmlParserRequest(String taskId, HtmlParserRequestDTO htmlParserRequestDTO) {
        Task task = findTaskById(taskId);
        task.setHtmlParserRequest(taskMapper.toEntity(htmlParserRequestDTO));
        taskRepository.save(task);
        return taskMapper.toDto(task);
    }

    public TaskDTO setHtmlPreprocessingRequest(String taskId, HtmlPreprocessingRequestDTO htmlPreprocessingRequestDTO) {
        Task task = findTaskById(taskId);
        task.setHtmlPreprocessingRequest(taskMapper.toEntity(htmlPreprocessingRequestDTO));
        taskRepository.save(task);
        return taskMapper.toDto(task);
    }

    public TaskDTO setTextRecognitionRequest(String taskId, TextRecognitionRequestDTO textRecognitionRequestDTO) {
        Task task = findTaskById(taskId);
        task.setTextRecognitionRequest(taskMapper.toEntity(textRecognitionRequestDTO));
        taskRepository.save(task);
        return taskMapper.toDto(task);
    }

    public TaskDTO setLLMRequest(String taskId, LLMRequestDTO llmRequestDTO) {
        Task task = findTaskById(taskId);
        task.setLlmRequest(taskMapper.toEntity(llmRequestDTO));
        taskRepository.save(task);
        return taskMapper.toDto(task);
    }

    public TaskDTO setHtmlParserResponse(String taskId, HtmlParserResponseDTO htmlParserResponseDTO) {
        Task task = findTaskById(taskId);
        task.setHtmlParserResponse(taskMapper.toEntity(htmlParserResponseDTO));
        taskRepository.save(task);
        return taskMapper.toDto(task);
    }

    public TaskDTO setHtmlPreprocessingResponse(String taskId, HtmlPreprocessingResponseDTO htmlPreprocessingResponseDTO) {
        Task task = findTaskById(taskId);
        task.setHtmlPreprocessingResponse(taskMapper.toEntity(htmlPreprocessingResponseDTO));
        taskRepository.save(task);
        return taskMapper.toDto(task);
    }

    public TaskDTO setTextRecognitionResponse(String taskId, TextRecognitionResponseDTO textRecognitionResponseDTO) {
        Task task = findTaskById(taskId);
        task.setTextRecognitionResponse(taskMapper.toEntity(textRecognitionResponseDTO));
        taskRepository.save(task);
        return taskMapper.toDto(task);
    }

    public TaskDTO setLLMResponse(String taskId, LLMResponseDTO llmResponseDTO) {
        Task task = findTaskById(taskId);
        task.setLlmResponse(taskMapper.toEntity(llmResponseDTO));
        taskRepository.save(task);
        return taskMapper.toDto(task);
    }

    public TaskDTO setStatusAndMessage(String taskId, TaskStatus status, String message) {
        Task task = findTaskById(taskId);
        task.setStatus(status);
        task.setMessage(message);
        taskRepository.save(task);
        return taskMapper.toDto(task);
    }

    private Task findTaskById(String taskId) {
        return taskRepository.findById(taskId).orElseThrow(
                () -> new TaskNotFoundException("task not found")
        );
    }
}