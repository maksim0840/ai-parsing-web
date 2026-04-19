package io.github.maksim0840.parsingtaskorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.parsingtaskorchestrator.dto.TaskDTO;
import io.github.maksim0840.parsingtaskorchestrator.grpc.OrchestratorFinishGrpcClient;
import io.github.maksim0840.parsingtaskorchestrator.rabbitmq.RabbitMQSender;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorService {

    private final TaskService taskService;
    private final RabbitMQSender rabbitMQSender;
    private final OrchestratorFinishGrpcClient finishGrpcClient;

    public OrchestratorService(TaskService taskService, RabbitMQSender rabbitMQSender, OrchestratorFinishGrpcClient finishGrpcClient) {
        this.taskService = taskService;
        this.rabbitMQSender = rabbitMQSender;
        this.finishGrpcClient = finishGrpcClient;
    }

    public void startRequestsPipeline(String taskId,
                                   HtmlParserRequestDTO htmlParserRequest,
                                   HtmlPreprocessingRequestDTO htmlPreprocessingRequest,
                                   TextRecognitionRequestDTO textRecognitionRequest) throws JsonProcessingException {

        TaskDTO task = taskService.addTask(taskId, htmlParserRequest, htmlPreprocessingRequest, textRecognitionRequest);

        if (task.htmlParserRequired()) {
            rabbitMQSender.sendToHtmlParserQueue(task.htmlParserRequest());
        } else if (task.htmlPreprocessingRequired()) {
            rabbitMQSender.sendToHtmlPreprocessingQueue(task.htmlPreprocessingRequest());
        } else if (task.textRecognitionRequired()) {
            rabbitMQSender.sendToTextRecognitionQueue(task.textRecognitionRequest());
        } else {
            endRequestsPipeline(taskId);
        }
    }

    public void distributeRequestsAfterHtmlParser(HtmlParserResponseDTO response) throws JsonProcessingException {
        TaskDTO task = taskService.setHtmlParserResponse(response.taskId(), response);

        if (task.htmlPreprocessingRequired()) {
            rabbitMQSender.sendToHtmlPreprocessingQueue(task.htmlPreprocessingRequest());
        } else if (task.textRecognitionRequired()) {
            rabbitMQSender.sendToTextRecognitionQueue(task.textRecognitionRequest());
        } else {
            endRequestsPipeline(response.taskId());
        }
    }

    public void distributeRequestsAfterHtmlPreprocessing(HtmlPreprocessingResponseDTO response) throws JsonProcessingException {
        TaskDTO task = taskService.setHtmlPreprocessingResponse(response.taskId(), response);

        if (task.textRecognitionRequired()) {
            rabbitMQSender.sendToTextRecognitionQueue(task.textRecognitionRequest());
        } else {
            endRequestsPipeline(response.taskId());
        }
    }

    public void distributeRequestsAfterTextRecognition(TextRecognitionResponseDTO response) {
        TaskDTO task = taskService.setTextRecognitionResponse(response.taskId(), response);

        endRequestsPipeline(response.taskId());
    }

    public void endRequestsPipeline(String taskId) {
        TaskDTO taskDTO = taskService.getTask(taskId);
        finishGrpcClient.finishParsing(taskId, taskDTO.htmlParserResponse(), taskDTO.htmlPreprocessingResponse(), taskDTO.textRecognitionResponse());
    }
}
