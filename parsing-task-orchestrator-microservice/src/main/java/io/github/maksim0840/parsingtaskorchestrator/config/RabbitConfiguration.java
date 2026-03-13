package io.github.maksim0840.parsingtaskorchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitConfiguration {

    @Bean
    public Queue queue() {
        return new Queue("name", false); // очередь name, которая уничтожается при выключении сервиса
    }
}
