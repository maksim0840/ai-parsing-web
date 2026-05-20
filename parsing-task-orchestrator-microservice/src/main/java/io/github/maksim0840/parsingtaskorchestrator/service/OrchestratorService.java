package io.github.maksim0840.parsingtaskorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import io.github.maksim0840.parsingtaskorchestrator.dto.StatusDTO;
import io.github.maksim0840.parsingtaskorchestrator.dto.TaskDTO;
import io.github.maksim0840.parsingtaskorchestrator.exception.TaskNotFoundException;
import io.github.maksim0840.parsingtaskorchestrator.rabbitmq.RabbitMQSender;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorService {

    private final TaskService taskService;
    private final LLMService llmService;
    private final RabbitMQSender rabbitMQSender;

    public OrchestratorService(TaskService taskService, LLMService llmService, RabbitMQSender rabbitMQSender) {
        this.taskService = taskService;
        this.llmService = llmService;
        this.rabbitMQSender = rabbitMQSender;
    }

    public void startRequestsPipeline(TaskDTO task) throws JsonProcessingException {
        System.out.println("startRequestsPipeline");
        System.out.println("start task:");
        System.out.println(task);

        if (task.htmlParserRequired()) {
            startHtmlParsing(task.htmlParserRequest());
        } else if (task.htmlPreprocessingRequired()) {
            startHtmlPreprocessing(task.htmlPreprocessingRequest());
        } else if (task.textRecognitionRequired()) {
            startTextRecognition(task.textRecognitionRequest());
        } else if (task.llmRequired()) {
            startLlmProcessing(task.llmRequest());
        } else {
            endPipelineSuccess(task.id());
        }
    }


    public void distributeRequestsAfterHtmlParser(HtmlParserResponseDTO response) throws JsonProcessingException {
        System.out.println("distributeRequestsAfterHtmlParser");
        if (!response.success()) {
            endPipelineFail(response.taskId(), response.message());
            return;
        }

        // Добавляем ответ от сервиса HtmlParser
        TaskDTO task = taskService.setHtmlParserResponse(response.taskId(), response);
        // Добавляем в запрос HtmlPreprocessing новые htmlPaths, полученный из ответа HtmlParser
        if (task.htmlPreprocessingRequired()) {
            task = taskService.setHtmlPreprocessingRequest(
                    response.taskId(),
                    task.htmlPreprocessingRequest().addAllToHtmlDocs(response.htmlDocs())
            );
        }
        // Добавляем в запрос TextRecognition новые imagePaths, полученные из ответа HtmlParser
        if (task.textRecognitionRequired()) {
            task = taskService.setTextRecognitionRequest(
                    response.taskId(),
                    task.textRecognitionRequest().addAllToImages(response.images())
            );
        }
        // Добавляем в запрос LLM новый htmlPath, полученные из ответа HtmlParser
        if (task.llmRequired()) {
            task = taskService.setLLMRequest(
                    response.taskId(),
                    task.llmRequest().addAllToHtmlDocs(response.htmlDocs())
            );
        }

        // Распределяем следующий запрос
        if (task.htmlPreprocessingRequired()) {
            startHtmlPreprocessing(task.htmlPreprocessingRequest());
        } else if (task.textRecognitionRequired()) {
            startTextRecognition(task.textRecognitionRequest());
        } else if (task.llmRequired()) {
            startLlmProcessing(task.llmRequest());
        } else {
            endPipelineSuccess(response.taskId());
        }
    }

    public void distributeRequestsAfterHtmlPreprocessing(HtmlPreprocessingResponseDTO response) throws JsonProcessingException {
        System.out.println("distributeRequestsAfterHtmlPreprocessing");
        if (!response.success()) {
            endPipelineFail(response.taskId(), response.message());
            return;
        }

        // Добавляем ответ от сервиса HtmlParser
        TaskDTO task = taskService.setHtmlPreprocessingResponse(response.taskId(), response);

        // Распределяем следующий запрос
        if (task.textRecognitionRequired()) {
            rabbitMQSender.sendToTextRecognitionQueue(task.textRecognitionRequest());
        } else if (task.llmRequired()) {
            startLlmProcessing(task.llmRequest());
        } else {
            endPipelineSuccess(response.taskId());
        }
    }

    public void distributeRequestsAfterTextRecognition(TextRecognitionResponseDTO response) {
        System.out.println("distributeRequestsAfterTextRecognition");
        if (!response.success()) {
            endPipelineFail(response.taskId(), response.message());
            return;
        }

        // Добавляем ответ от сервиса HtmlParser
        TaskDTO task = taskService.setTextRecognitionResponse(response.taskId(), response);
        // Добавляем в запрос LLM новые textByImage, полученные из ответа TextRecognition
        if (task.llmRequired()) {
            task = taskService.setLLMRequest(
                    response.taskId(),
                    task.llmRequest().addAllToImages(response.images())
            );
        }

        // Распределяем следующий запрос
        if (task.llmRequired()) {
            startLlmProcessing(task.llmRequest());
        } else {
            endPipelineSuccess(response.taskId());
        }
    }

    public void distributeRequestsAfterLLM(LLMResponseDTO response) {
        System.out.println("distributeRequestsAfterLLM");
        if (!response.success()) {
            endPipelineFail(response.taskId(), response.message());
            return;
        }

        // Добавляем ответ от сервиса LLM
        TaskDTO task = taskService.setLLMResponse(response.taskId(), response);

        endPipelineSuccess(response.taskId());
    }


    private void endPipelineSuccess(String taskId) {
        System.out.println("endPipelineSuccess");
        TaskDTO taskDTO = taskService.setStatusAndMessage(taskId, TaskStatus.DONE, "");
    }

    private void endPipelineFail(String taskId, String message) {
        System.out.println("endRequestsPipelineFail");
        TaskDTO taskDTO = taskService.setStatusAndMessage(taskId, TaskStatus.FAILED, message);
    }

    private void startHtmlParsing(HtmlParserRequestDTO request) throws JsonProcessingException {
        taskService.setStatusAndMessage(request.taskId(), TaskStatus.HTML_PARSING, "");
        rabbitMQSender.sendToHtmlParserQueue(request);
    }

    private void startHtmlPreprocessing(HtmlPreprocessingRequestDTO request) throws JsonProcessingException {
        taskService.setStatusAndMessage(request.taskId(), TaskStatus.HTML_PREPROCESSING, "");
        rabbitMQSender.sendToHtmlPreprocessingQueue(request);
    }

    private void startTextRecognition(TextRecognitionRequestDTO request) throws JsonProcessingException {
        taskService.setStatusAndMessage(request.taskId(), TaskStatus.TEXT_RECOGNITION, "");
        rabbitMQSender.sendToTextRecognitionQueue(request);
    }

    private void startLlmProcessing(LLMRequestDTO request) {
        taskService.setStatusAndMessage(request.taskId(), TaskStatus.LLM_PROCESSING, "");
        LLMResponseDTO response = llmService.processLlmRequest(request);
        distributeRequestsAfterLLM(response);
    }


    public StatusDTO getStatusInfo(String taskId) {
        if (!taskService.isTaskExists(taskId)) {
            return new StatusDTO(TaskStatus.NOT_REGISTERED, null);
        }
        TaskDTO taskDTO = taskService.getTask(taskId);
        return new StatusDTO(taskDTO.status(), taskDTO.message());
    }

    public TaskDTO getTask(String taskId) {
        return taskService.getTask(taskId);
    }
}
