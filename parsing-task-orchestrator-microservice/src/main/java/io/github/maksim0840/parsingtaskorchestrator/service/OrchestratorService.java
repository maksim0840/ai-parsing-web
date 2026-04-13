package io.github.maksim0840.parsingtaskorchestrator.service;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.parsingtaskorchestrator.domain.Task;
import io.github.maksim0840.parsingtaskorchestrator.rabbitmq.RabbitMQSender;
import io.github.maksim0840.parsingtaskorchestrator.repository.TaskRepository;

public class OrchestratorService {

    private final TaskService taskService;
    private final RabbitMQSender rabbitMQSender;

    public OrchestratorService(TaskService taskService, RabbitMQSender rabbitMQSender) {
        this.taskService = taskService;
        this.rabbitMQSender = rabbitMQSender;
    }

    public void distributeRequests(String taskId,
                                   HtmlParserRequestDTO htmlParserRequest,
                                   HtmlPreprocessingRequestDTO htmlPreprocessingRequest,
                                   TextRecognitionRequestDTO textRecognitionRequest) {
        taskService.addTask(taskId, htmlParserRequest, htmlPreprocessingRequest, textRecognitionRequest);
        sendRequestByChain Определить что за запрос и куда отправить
    }
}
