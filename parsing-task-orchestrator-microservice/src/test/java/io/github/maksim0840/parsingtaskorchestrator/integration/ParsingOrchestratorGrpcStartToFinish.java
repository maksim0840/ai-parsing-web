//package io.github.maksim0840.parsingtaskorchestrator.integration;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
//import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserRequestMapper;
//import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingRequestMapper;
//import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TextRecognitionRequestMapper;
//import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStartServiceGrpc;
//import io.github.maksim0840.parsingtaskorchestrator.config.RabbitConfiguration;
//import io.github.maksim0840.parsingtaskorchestrator.repository.TaskRepository;
//import io.github.maksim0840.parsingtaskorchestrator.util.JsonMapper;
//import net.devh.boot.grpc.client.inject.GrpcClient;
//import org.junit.jupiter.api.Test;
//import org.springframework.amqp.core.AmqpAdmin;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.rabbit.core.RabbitAdmin;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.mongodb.MongoDBContainer;
//import org.testcontainers.rabbitmq.RabbitMQContainer;
//import org.testcontainers.utility.DockerImageName;
//import org.junit.jupiter.api.BeforeEach;
//
//import java.util.List;
//import java.util.Map;
//
//import static java.util.concurrent.TimeUnit.SECONDS;
//import static org.assertj.core.api.Assertions.allOf;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//
//@Testcontainers // включаем работу test-контейнеров (docker)
//@SpringBootTest(properties = {
//        "grpc.server.inProcessName=test",      // включаем in-process server (клиент и сервер общаются внутри одного JVM-процесса)
//        "grpc.server.port=-1",                 // выключаем внешний server (не отдаем порт наружу)
//        "grpc.client.inProcess.address=in-process:test" // подключаем клиента к in-process серверу
//})
//public class ParsingOrchestratorGrpcStartToFinish {
//
//    @Container
//    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
//
//    @Container
//    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));
//
//    // Подмена динамических spring свойств для подключения к тестовой базе данных
//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
//        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
//        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
//        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
//        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
//    }
//
//    // Объект для отправки gRPC запросов серверу
////    @GrpcClient("parsing_orchestrator_finish")
////    private OrchestratorFinishServiceGrpc.OrchestratorFinishServiceBlockingStub finishServiceBlockingStub;
//    @GrpcClient("inProcess")
//    private OrchestratorStartServiceGrpc.OrchestratorStartServiceBlockingStub startServiceBlockingStub;
//
//    // Репозиторий для отправки запросов к базе данных
//    @Autowired
//    TaskRepository repository;
//
//    @Autowired
//    RabbitTemplate rabbitTemplate;
//
//    @Autowired
//    AmqpAdmin amqpAdmin;
//
//    private static String htmlParserRequestQueueName = "html_parser_queue_request";
//    private static String htmlParserResponseQueueName = "html_parser_queue_response";
//    private static String htmlPreprocessingRequestQueueName = "html_preprocessing_queue_request";
//    private static String htmlPreprocessingResponseQueueName = "html_preprocessing_queue_response";
//    private static String textRecognitionRequestQueueName = "text_recognition_queue_request";
//    private static String textRecognitionResponseQueueName = "text_recognition_queue_response";
//
//    @BeforeEach
//    void cleanContainers() {
//        repository.deleteAll();
//        amqpAdmin.purgeQueue(htmlParserRequestQueueName, true);
//        amqpAdmin.purgeQueue(htmlParserResponseQueueName, true);
//        amqpAdmin.purgeQueue(htmlPreprocessingRequestQueueName, true);
//        amqpAdmin.purgeQueue(htmlPreprocessingResponseQueueName, true);
//        amqpAdmin.purgeQueue(textRecognitionRequestQueueName, true);
//        amqpAdmin.purgeQueue(textRecognitionResponseQueueName, true);
//    }
//
//    @Test
//    void startParsing() throws JsonProcessingException {
//        String taskId = "12345";
//        HtmlParserRequestDTO htmlParserRequestDTO = new HtmlParserRequestDTO(
//                taskId,
//                "https://www.google.com/",
//                "out/html",
//                "out/imgs",
//                Boolean.TRUE,
//                Map.of("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:148.0) Gecko/20100101 Firefox/148.0"),
//                Map.of("cf_clearance", "kT0y0NrK8EL.5ex2MAZFs4ePHGWFZ7gtzb8G5WCHEUU-1775927163-1.2.1.1-goZ"),
//                Map.of("ip", "127.0.0.1", "port", "5050", "username", "admin", "password", "admin123"),
//                "DIFFICULT",
//                5
//        );
//        HtmlPreprocessingRequestDTO htmlPreprocessingRequestDTO = new HtmlPreprocessingRequestDTO(
//                taskId,
//                List.of("out/html/asd943opds453j.html"),
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE,
//                Boolean.TRUE
//        );
//        TextRecognitionRequestDTO textRecognitionRequestDTO = new TextRecognitionRequestDTO(
//                taskId,
//                List.of("out/imgs/23nikoj.png", "out/imgs/kn7ewqr.png", "out/imgs/iopjasdf.png")
//        );
//        HtmlParserResponseDTO htmlParserResponseDTO = new HtmlParserResponseDTO(
//                taskId,
//                true,
//                "OK",
//                "out/html/asd943opds453j.html",
//                List.of("out/imgs/23nikoj.png", "out/imgs/kn7ewqr.png", "out/imgs/iopjasdf.png")
//        );
//        HtmlPreprocessingResponseDTO htmlPreprocessingResponseDTO = new HtmlPreprocessingResponseDTO(
//                taskId,
//                true,
//                "OK",
//                List.of("out/html/asd943opds453j.html")
//        );
//        TextRecognitionResponseDTO textRecognitionResponseDTO = new TextRecognitionResponseDTO(
//                taskId,
//                true,
//                "OK",
//                Map.of("out/imgs/23nikoj.png", "some text 1", "out/imgs/kn7ewqr.png", "some text 2", "out/imgs/iopjasdf.png", "")
//        );
//
//
//
//        OrchestratorStartRequest request = OrchestratorStartRequest.newBuilder()
//                .setTaskId(taskId)
//                .setHtmlParserRequest(HtmlParserRequestMapper.dtoToProto(htmlParserRequestDTO))
//                .setHtmlPreprocessingRequest(HtmlPreprocessingRequestMapper.dtoToProto(htmlPreprocessingRequestDTO))
//                .setTextRecognitionRequest(TextRecognitionRequestMapper.dtoToProto(textRecognitionRequestDTO))
//                .build();
//
//        startServiceBlockingStub.startParsing(request);
//        await().atMost(5, SECONDS).until(() -> rabbitMessageCount(htmlParserRequestQueueName) == 1);                    // сообщение опубликовалось в очередь
//        assertOtherQueuesEmpty(htmlParserRequestQueueName);                                                                     // все остальные очереди пустые
//        assertThat(rabbitGetMessage(htmlParserRequestQueueName)).isEqualTo(JsonMapper.objectToString(htmlParserRequestDTO));    // сообщение в очереди корректное
//
//        rabbitPublishMessage(htmlParserResponseQueueName, JsonMapper.objectToString(htmlParserResponseDTO));
//        await().atMost(5, SECONDS).until(() -> rabbitMessageCount(htmlPreprocessingRequestQueueName) == 1);                         // сообщение опубликовалось в очередь
//        assertOtherQueuesEmpty(htmlPreprocessingRequestQueueName);                                                                          // все остальные очереди пустые
//        assertThat(rabbitGetMessage(htmlPreprocessingRequestQueueName)).isEqualTo(JsonMapper.objectToString(htmlPreprocessingRequestDTO));  // сообщение в очереди корректное
//
//        rabbitPublishMessage(htmlPreprocessingResponseQueueName, JsonMapper.objectToString(htmlPreprocessingResponseDTO));
//        await().atMost(5, SECONDS).until(() -> rabbitMessageCount(textRecognitionRequestQueueName) == 1);                       // сообщение опубликовалось в очередь
//        assertOtherQueuesEmpty(textRecognitionRequestQueueName);                                                                        // все остальные очереди пустые
//        assertThat(rabbitGetMessage(textRecognitionRequestQueueName)).isEqualTo(JsonMapper.objectToString(textRecognitionRequestDTO));  // сообщение в очереди корректное
//
//        rabbitPublishMessage(textRecognitionResponseQueueName, JsonMapper.objectToString(textRecognitionResponseDTO));
//        assertOtherQueuesEmpty(""); // ВСЕ очереди пусты
//
//    }
//
//    void assertOtherQueuesEmpty(String targetQueueName) {
//        List<String> queueNames = new java.util.ArrayList<>(List.of(
//                htmlParserRequestQueueName,
//                htmlParserResponseQueueName,
//                htmlPreprocessingRequestQueueName,
//                htmlPreprocessingResponseQueueName,
//                textRecognitionRequestQueueName,
//                textRecognitionResponseQueueName
//        ));
//        queueNames.remove(targetQueueName);
//
//        queueNames.forEach(
//                queueName -> assertThat(rabbitMessageCount(queueName)).isEqualTo(0)
//        );
//    }
//
//    int rabbitMessageCount(String queueName) {
//        return (int) amqpAdmin.getQueueProperties(queueName).get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
//    }
//
//    String rabbitGetMessage(String queueName) {
//        return new String(rabbitTemplate.receive(queueName).getBody());
//    }
//
//    void rabbitPublishMessage(String queueName, String message) {
//        rabbitTemplate.convertAndSend(queueName, message);
//    }
//}
