package io.github.maksim0840.parsingtaskorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.parsingtaskorchestrator.dto.TaskDTO;
import io.github.maksim0840.parsingtaskorchestrator.rabbitmq.RabbitMQSender;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorService {

    private final TaskService taskService;
    private final RabbitMQSender rabbitMQSender;

    public OrchestratorService(TaskService taskService, RabbitMQSender rabbitMQSender) {
        this.taskService = taskService;
        this.rabbitMQSender = rabbitMQSender;
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
            endRequestsPipeline();
        }
    }

    public void distributeRequestsAfterHtmlParser(HtmlParserResponseDTO response) throws JsonProcessingException {
        if (response == null) {
            endRequestsPipeline();
            return;
        }

        TaskDTO task = taskService.getTask(response.taskId());

        if (task.htmlPreprocessingRequired()) {
            rabbitMQSender.sendToHtmlPreprocessingQueue(task.htmlPreprocessingRequest());
        } else if (task.textRecognitionRequired()) {
            rabbitMQSender.sendToTextRecognitionQueue(task.textRecognitionRequest());
        } else {
            endRequestsPipeline();
        }
    }

    public void distributeRequestsAfterHtmlPreprocessing(HtmlPreprocessingResponseDTO response) throws JsonProcessingException {
        if (response == null) {
            endRequestsPipeline();
            return;
        }

        TaskDTO task = taskService.getTask(response.taskId());

        if (task.textRecognitionRequired()) {
            rabbitMQSender.sendToTextRecognitionQueue(task.textRecognitionRequest());
        } else {
            endRequestsPipeline();
        }
    }

    public void distributeRequestsAfterTextRecognition(TextRecognitionResponseDTO response) {
        endRequestsPipeline();
    }

    public void endRequestsPipeline() {

    }
}
