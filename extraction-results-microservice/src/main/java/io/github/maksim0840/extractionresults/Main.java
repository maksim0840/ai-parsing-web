package io.github.maksim0840.extractionresults;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing    // автозаполнение некоторых полей Spring-ом при создании объекта в базе
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
