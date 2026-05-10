package io.github.maksim0840.apigateway;

import io.github.maksim0840.apigateway.config.properties.S3Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({S3Properties.class}) // регистрируем конфигурационные бины
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
