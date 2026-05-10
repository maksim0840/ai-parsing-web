package io.github.maksim0840.parsingtaskorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import io.github.maksim0840.parsingtaskorchestrator.dto.TaskDTO;
import io.github.maksim0840.parsingtaskorchestrator.grpc.OrchestratorFinishGrpcClient;
import io.github.maksim0840.parsingtaskorchestrator.rabbitmq.RabbitMQSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrchestratorService {

    private final TaskService taskService;
    private final LLMService llmService;
    private final RabbitMQSender rabbitMQSender;
    private final OrchestratorFinishGrpcClient finishGrpcClient;

    public OrchestratorService(TaskService taskService, LLMService llmService, RabbitMQSender rabbitMQSender, OrchestratorFinishGrpcClient finishGrpcClient) {
        this.taskService = taskService;
        this.llmService = llmService;
        this.rabbitMQSender = rabbitMQSender;
        this.finishGrpcClient = finishGrpcClient;
    }

    public void startRequestsPipeline(String taskId,
                                      HtmlParserRequestDTO htmlParserRequest,
                                      HtmlPreprocessingRequestDTO htmlPreprocessingRequest,
                                      TextRecognitionRequestDTO textRecognitionRequest,
                                      LLMRequestDTO llmRequestDTO) throws JsonProcessingException {
        System.out.println("startRequestsPipeline");
        TaskDTO task = taskService.addTask(taskId, htmlParserRequest, htmlPreprocessingRequest, textRecognitionRequest, llmRequestDTO);
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
            endPipelineSuccess(taskId);
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
        // Добавляем в запрос HtmlPreprocessing новый htmlPath, полученный из ответа HtmlParser
        if (task.htmlPreprocessingRequired()) {
            task = taskService.setHtmlPreprocessingRequest(
                    response.taskId(),
                    task.htmlPreprocessingRequest().addToHtmlPaths(response.htmlPath())
            );
        }
        // Добавляем в запрос TextRecognition новые imagePaths, полученные из ответа HtmlParser
        if (task.textRecognitionRequired()) {
            task = taskService.setTextRecognitionRequest(
                    response.taskId(),
                    task.textRecognitionRequest().addAllToImagePaths(response.imagePaths())
            );
        }
        // Добавляем в запрос LLM новый htmlPath, полученные из ответа HtmlParser
        if (task.llmRequired()) {
            task = taskService.setLLMRequest(
                    response.taskId(),
                    task.llmRequest().addToHtmlPaths(response.htmlPath())
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
                    task.llmRequest().putAllToTextByImage(response.textByImage())
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
        finishGrpcClient.finishParsing(taskId, taskDTO.htmlParserResponse(), taskDTO.htmlPreprocessingResponse(), taskDTO.textRecognitionResponse(), taskDTO.llmResponse());
    }

    private void endPipelineFail(String taskId, String message) {
        System.out.println("endRequestsPipelineFail");
        TaskDTO taskDTO = taskService.setStatusAndMessage(taskId, TaskStatus.FAILED, message);
        finishGrpcClient.finishParsing(taskId, taskDTO.htmlParserResponse(), taskDTO.htmlPreprocessingResponse(), taskDTO.textRecognitionResponse(), taskDTO.llmResponse());
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


    public TaskStatus getStatus(String taskId) {
        if (!taskService.isTaskExists(taskId)) {
            return TaskStatus.NOT_REGISTERED;
        }
        TaskDTO taskDTO = taskService.getTask(taskId);
        return taskDTO.status();
    }

    public String getMessage(String taskId) {
        TaskDTO taskDTO = taskService.getTask(taskId);
        return taskDTO.message();
    }
}
