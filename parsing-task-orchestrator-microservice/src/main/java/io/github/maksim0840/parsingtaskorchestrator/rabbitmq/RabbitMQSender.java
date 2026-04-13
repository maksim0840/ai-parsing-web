package io.github.maksim0840.parsingtaskorchestrator.rabbitmq;

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

    public void sendToHtmlParserQueue(String message) {
        rabbitTemplate.convertAndSend(htmlParserRequestQueueName, message);
    }

    public void sendToHtmlPreprocessingRequestQueue(String message) {
        rabbitTemplate.convertAndSend(htmlPreprocessingRequestQueueName, message);
    }

    public void sendToTextRecognitionQueue(String message) {
        rabbitTemplate.convertAndSend(textRecognitionRequestQueueName, message);
    }
}

