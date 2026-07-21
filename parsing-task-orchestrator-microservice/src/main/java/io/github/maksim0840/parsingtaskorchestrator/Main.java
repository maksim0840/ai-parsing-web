package io.github.maksim0840.parsingtaskorchestrator;

import io.github.maksim0840.parsingtaskorchestrator.config.properties.LLMProperties;
import io.github.maksim0840.parsingtaskorchestrator.config.properties.S3Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableConfigurationProperties({S3Properties.class, LLMProperties.class}) // регистрируем конфигурационные бины
public class Main {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
    }
}