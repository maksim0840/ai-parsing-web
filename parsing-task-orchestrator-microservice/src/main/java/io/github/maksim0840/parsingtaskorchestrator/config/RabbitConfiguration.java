package io.github.maksim0840.parsingtaskorchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitConfiguration {

    @Value("${rabbitmq.html_parser_queue.request_name}")
    private String htmlParserRequestQueueName;

    @Value("${rabbitmq.html_parser_queue.response_name}")
    private String htmlParserResponseQueueName;

    @Value("${rabbitmq.text_recognition_queue.request_name}")
    private String textRecognitionRequestQueueName;

    @Value("${rabbitmq.text_recognition_queue.response_name}")
    private String textRecognitionResponseQueueName;


    /**
     * Конфигурируем очереди
     * @return Очередь RabbitMQ
     */

    @Bean
    public Queue htmlParserRequestQueue() {
        return new Queue(htmlParserRequestQueueName, false); // очередь не переживёт restart
    }

    @Bean
    public Queue htmlParserResponseQueue() {
        return new Queue(htmlParserResponseQueueName, false);
    }

    @Bean
    public Queue textRecognitionRequestQueue() {
        return new Queue(textRecognitionRequestQueueName, false);
    }

    @Bean
    public Queue textRecognitionResponseQueue() {
        return new Queue(textRecognitionResponseQueueName, false);
    }
}


