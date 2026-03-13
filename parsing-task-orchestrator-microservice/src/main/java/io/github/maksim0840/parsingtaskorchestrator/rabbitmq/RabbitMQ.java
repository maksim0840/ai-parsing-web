package io.github.maksim0840.parsingtaskorchestrator.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQ {
    private final RabbitTemplate rabbitTemplate;

    public RabbitMQ(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    private void send(String queueName, String message) {
        rabbitTemplate.convertAndSend(queueName, message);
    }

    public void sendHtmlParserMessage(String message) {
        send("html_parser_request", message);
    }

    public void sendTextRecognitionMessage(String message) {
        send("text_recognition_request", message);
    }

}

