package io.github.maksim0840.parsingtaskorchestrator.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionResponseDTO;
import io.github.maksim0840.parsingtaskorchestrator.service.OrchestratorService;
import io.github.maksim0840.parsingtaskorchestrator.mapper.JsonMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQListener {

    private final OrchestratorService orchestratorService;

    public RabbitMQListener(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @RabbitListener(queues = "#{environment.getProperty('rabbitmq.html_parser_queue.response_name')}")
    public void receiveFromHtmlParserQueue(String message) throws JsonProcessingException {
        HtmlParserResponseDTO response = JsonMapper.stringToObject(message, HtmlParserResponseDTO.class);
        orchestratorService.distributeRequestsAfterHtmlParser(response);

    }

    @RabbitListener(queues = "#{environment.getProperty('rabbitmq.html_preprocessing_queue.response_name')}")
    public void receiveFromHtmlPreprocessingQueue(String message) throws JsonProcessingException {
        HtmlPreprocessingResponseDTO response = JsonMapper.stringToObject(message, HtmlPreprocessingResponseDTO.class);
        orchestratorService.distributeRequestsAfterHtmlPreprocessing(response);
    }

    @RabbitListener(queues = "#{environment.getProperty('rabbitmq.text_recognition_queue.response_name')}")
    public void receiveTextRecognitionQueue(String message) throws JsonProcessingException {
        TextRecognitionResponseDTO response = JsonMapper.stringToObject(message, TextRecognitionResponseDTO.class);
        orchestratorService.distributeRequestsAfterTextRecognition(response);
    }
}
