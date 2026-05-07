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
        TaskDTO task = taskService.setHtmlParserResponse(response.taskId(), response);

        if (task.htmlPreprocessingRequired()) {
            // Добавляем в запрос HtmlPreprocessing новый htmlPath, полученный из ответа HtmlParser
            HtmlPreprocessingRequestDTO newRequest = task.htmlPreprocessingRequest();
            List<String> newHtmlPaths = newRequest.htmlPaths();
            newHtmlPaths.add(response.htmlPath());
            rabbitMQSender.sendToHtmlPreprocessingQueue(newRequest.withHtmlPaths(newHtmlPaths));
        } else if (task.textRecognitionRequired()) {
            // Добавляем в запрос TextRecognition новые imagePaths, полученные из ответа HtmlParser
            TextRecognitionRequestDTO newRequest = task.textRecognitionRequest();
            List<String> newImagePaths = newRequest.imagePaths();
            newImagePaths.addAll(response.imagePaths());
            rabbitMQSender.sendToTextRecognitionQueue(newRequest.withImagePaths(newImagePaths));
        } else if (task.llmRequired()) {
            syncCallAndDistributeRequestsLLM(task.llmRequest());
        } else {
            endRequestsPipeline(response.taskId());
        }
    }

    public void distributeRequestsAfterHtmlPreprocessing(HtmlPreprocessingResponseDTO response) throws JsonProcessingException {
        System.out.println("distributeRequestsAfterHtmlPreprocessing");
        TaskDTO task = taskService.setHtmlPreprocessingResponse(response.taskId(), response);

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
        TaskDTO task = taskService.setTextRecognitionResponse(response.taskId(), response);

        if (task.llmRequired()) {
            syncCallAndDistributeRequestsLLM(task.llmRequest());
        } else {
            endRequestsPipeline(response.taskId());
        }
    }

    public void syncCallAndDistributeRequestsLLM(LLMRequestDTO request) {
        System.out.println("syncCallAndDistributeRequestsLLM");
        String output = llmService.sendRequestToModel(request.modelName(), request.systemMessage(), request.userMessage(), request.temperature(), request.maxOutputTokens());
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
