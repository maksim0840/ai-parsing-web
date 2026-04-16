package io.github.maksim0840.parsingtaskorchestrator.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.parsingtaskorchestrator.util.JsonMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQSender {
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.html_parser_queue.request_name}")
    private String htmlParserRequestQueueName;

    @Value("${rabbitmq.html_preprocessing_queue.request_name}")
    private String htmlPreprocessingRequestQueueName;

    @Value("${rabbitmq.text_recognition_queue.request_name}")
    private String textRecognitionRequestQueueName;

    public RabbitMQSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendToHtmlParserQueue(HtmlParserRequestDTO request) throws JsonProcessingException {
        String message = JsonMapper.objectToString(request);
        rabbitTemplate.convertAndSend(htmlParserRequestQueueName, message);
    }

    public void sendToHtmlPreprocessingQueue(HtmlPreprocessingRequestDTO request) throws JsonProcessingException {
        String message = JsonMapper.objectToString(request);
        rabbitTemplate.convertAndSend(htmlPreprocessingRequestQueueName, message);
    }

    public void sendToTextRecognitionQueue(TextRecognitionRequestDTO request) throws JsonProcessingException {
        String message = JsonMapper.objectToString(request);
        rabbitTemplate.convertAndSend(textRecognitionRequestQueueName, message);
    }
}

