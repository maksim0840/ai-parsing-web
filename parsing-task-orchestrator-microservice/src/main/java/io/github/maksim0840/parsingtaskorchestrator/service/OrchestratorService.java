package io.github.maksim0840.parsingtaskorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
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
            rabbitMQSender.sendToHtmlParserQueue(task.htmlParserRequest());
        } else if (task.htmlPreprocessingRequired()) {
            rabbitMQSender.sendToHtmlPreprocessingQueue(task.htmlPreprocessingRequest());
        } else if (task.textRecognitionRequired()) {
            rabbitMQSender.sendToTextRecognitionQueue(task.textRecognitionRequest());
        } else if (task.llmRequired()) {
            syncCallAndDistributeRequestsLLM(llmRequestDTO);
        } else {
            endRequestsPipeline(taskId);
        }
    }

    public void distributeRequestsAfterHtmlParser(HtmlParserResponseDTO response) throws JsonProcessingException {
        System.out.println("distributeRequestsAfterHtmlParser");
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
            rabbitMQSender.sendToHtmlPreprocessingQueue(task.htmlPreprocessingRequest());
        } else if (task.textRecognitionRequired()) {
            rabbitMQSender.sendToTextRecognitionQueue(task.textRecognitionRequest());
        } else if (task.llmRequired()) {
            syncCallAndDistributeRequestsLLM(task.llmRequest());
        } else {
            endRequestsPipeline(response.taskId());
        }
    }

    public void distributeRequestsAfterHtmlPreprocessing(HtmlPreprocessingResponseDTO response) throws JsonProcessingException {
        System.out.println("distributeRequestsAfterHtmlPreprocessing");
        // Добавляем ответ от сервиса HtmlParser
        TaskDTO task = taskService.setHtmlPreprocessingResponse(response.taskId(), response);

        // Распределяем следующий запрос
        if (task.textRecognitionRequired()) {
            rabbitMQSender.sendToTextRecognitionQueue(task.textRecognitionRequest());
        } else if (task.llmRequired()) {
            syncCallAndDistributeRequestsLLM(task.llmRequest());
        } else {
            endRequestsPipeline(response.taskId());
        }
    }

    public void distributeRequestsAfterTextRecognition(TextRecognitionResponseDTO response) {
        System.out.println("distributeRequestsAfterTextRecognition");
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
            syncCallAndDistributeRequestsLLM(task.llmRequest());
        } else {
            endRequestsPipeline(response.taskId());
        }
    }

    public void syncCallAndDistributeRequestsLLM(LLMRequestDTO request) {
        System.out.println("syncCallAndDistributeRequestsLLM");
        String output = llmService.sendRequestToModel(request.modelName(), request.systemMessage(), request.userMessage(), request.temperature(), request.maxOutputTokens(), request.htmlPaths(), request.textByImage());
        LLMResponseDTO response = new LLMResponseDTO(request.taskId(), output);
        taskService.setLLMResponse(response.taskId(), response);

        endRequestsPipeline(response.taskId());
    }

    public void endRequestsPipeline(String taskId) {
        System.out.println("endRequestsPipeline");
        TaskDTO taskDTO = taskService.getTask(taskId);
        finishGrpcClient.finishParsing(taskId, taskDTO.htmlParserResponse(), taskDTO.htmlPreprocessingResponse(), taskDTO.textRecognitionResponse(), taskDTO.llmResponse());
    }
}
