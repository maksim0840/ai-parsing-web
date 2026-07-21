package io.github.maksim0840.parsingtaskorchestrator.integration;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.parsing_task_orchestrator.v1.*;
import io.github.maksim0840.parsingtaskorchestrator.entity.Task;
import io.github.maksim0840.parsingtaskorchestrator.mapper.JsonMapper;
import io.github.maksim0840.parsingtaskorchestrator.repository.TaskRepository;
import io.github.maksim0840.parsingtaskorchestrator.service.LLMService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Интеграционные тесты оркестратора: gRPC вход -> Redis -> RabbitMQ -> Redis -> gRPC выход.
 * <p>
 * Хранилище задач переведено с MongoDB на Redis (@RedisHash), поэтому вместо
 * MongoDBContainer поднимается контейнер с Redis. RabbitMQ поднимается отдельным
 * контейнером — очереди объявляет RabbitConfiguration приложения.
 * <p>
 * LLM-этап выполняется СИНХРОННО внутри пайплайна и ходит во внешние API (YandexGPT /
 * GigaChat) и в S3, поэтому LLMService подменяется моком. Всё остальное — по-настоящему.
 */
@Testcontainers // включаем работу test-контейнеров (docker)
@SpringBootTest(properties = {
        "grpc.server.inProcessName=test",      // включаем in-process server (клиент и сервер общаются внутри одного JVM-процесса)
        "grpc.server.port=-1",                 // выключаем внешний server (не отдаем порт наружу)
        "grpc.client.inProcess.address=in-process:test", // подключаем клиента к in-process серверу

        // Заглушки для обязательных настроек, которые в проде приходят из llm.env / s3_settings.env.
        // Без них контекст не поднимется: у этих плейсхолдеров нет значений по умолчанию.
        "llm.yandexgpt_model_api_name=test-yandexgpt",
        "llm.yandexgpt_model_view_name=YandexGPT",
        "llm.yandexgpt_api_key=test-key",
        "llm.yandexgpt_folder_id=test-folder",
        "llm.gigachat_model_api_name=test-gigachat",
        "llm.gigachat_model_view_name=GigaChat",
        "llm.gigachat_auth_key=test-auth",
        "s3.access_key=test-access",
        "s3.secret_key=test-secret",
        "s3.bucket_name=test-bucket"
})
public class ParsingOrchestratorGrpcStartToFinish {

    // Redis вместо MongoDB: официального модуля Testcontainers для Redis нет,
    // поэтому используем GenericContainer из core-модуля.
    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @Container
    static RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    // Подмена динамических spring свойств для подключения к тестовым контейнерам
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    }

    // Объекты для отправки gRPC запросов серверу
    @GrpcClient("inProcess")
    private OrchestratorStartServiceGrpc.OrchestratorStartServiceBlockingStub startStub;
    @GrpcClient("inProcess")
    private OrchestratorStorageServiceGrpc.OrchestratorStorageServiceBlockingStub storageStub;

    // Репозиторий для проверки состояния в Redis
    @Autowired
    TaskRepository repository;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    AmqpAdmin amqpAdmin;

    // LLM-этап синхронный и ходит наружу — подменяем мокой
    @MockitoBean
    LLMService llmService;

    @Value("${rabbitmq.html_parser_queue.request_name}")
    String htmlParserRequestQueue;
    @Value("${rabbitmq.html_parser_queue.response_name}")
    String htmlParserResponseQueue;
    @Value("${rabbitmq.html_preprocessing_queue.request_name}")
    String htmlPreprocessingRequestQueue;
    @Value("${rabbitmq.html_preprocessing_queue.response_name}")
    String htmlPreprocessingResponseQueue;
    @Value("${rabbitmq.text_recognition_queue.request_name}")
    String textRecognitionRequestQueue;
    @Value("${rabbitmq.text_recognition_queue.response_name}")
    String textRecognitionResponseQueue;

    private static final String USER_ID = "user-42";

    @BeforeEach
    void cleanContainers() {
        repository.deleteAll();
        allQueues().forEach(queue -> amqpAdmin.purgeQueue(queue, true));

        // по умолчанию LLM отвечает успешно
        doAnswer(invocation -> {
            LLMRequestDTO request = invocation.getArgument(0);
            return new LLMResponseDTO(request.taskId(), true, "", "LLM OUTPUT");
        }).when(llmService).processLlmRequest(any());
    }


    // ==================================================================
    // ======================== ПОЛНЫЙ ПАЙПЛАЙН =========================
    // ==================================================================

    /*
    Проверяет полный проход пайплайна: HtmlParser -> HtmlPreprocessing -> TextRecognition -> LLM.
    На каждом шаге:
    - оркестратор кладёт запрос в нужную очередь и не трогает остальные
    - содержимое сообщения соответствует ожидаемому DTO
    - после публикации ответа пайплайн переходит к следующему этапу
    В конце задача переходит в статус DONE, а getTaskResult отдаёт все четыре ответа.
    */
    @Test
    void fullPipelineThroughAllStages() throws Exception {
        String taskId = "task-full";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .setHtmlParserRequest(htmlParserRequestProto(taskId))
                .setHtmlPreprocessingRequest(htmlPreprocessingRequestProto(taskId))
                .setTextRecognitionRequest(textRecognitionRequestProto(taskId))
                .setLlmRequest(llmRequestProto(taskId))
                .build());

        // --- этап 1: запрос ушёл в html_parser ---
        awaitMessageIn(htmlParserRequestQueue);
        assertOtherQueuesEmpty(htmlParserRequestQueue);
        assertThat(statusOf(taskId)).isEqualTo(TaskStatusProto.HTML_PARSING);

        HtmlParserRequestDTO sentParserRequest =
                JsonMapper.stringToObject(receive(htmlParserRequestQueue), HtmlParserRequestDTO.class);
        assertThat(sentParserRequest.taskId()).isEqualTo(taskId);
        assertThat(sentParserRequest.url()).isEqualTo("https://www.google.com/");
        assertThat(sentParserRequest.headers()).containsEntry("User-Agent", "Mozilla/5.0");
        assertThat(sentParserRequest.additionalPageLoadTimeoutS()).isEqualTo(5);

        // --- этап 2: отвечаем от html_parser, ждём html_preprocessing ---
        List<FileInfoDTO> parsedHtml = List.of(fileInfoDto("out/html/page.html", FileTypeDto.HTML));
        List<FileInfoDTO> parsedImages = List.of(
                fileInfoDto("out/imgs/a.png", FileTypeDto.IMG),
                fileInfoDto("out/imgs/b.png", FileTypeDto.IMG));

        publish(htmlParserResponseQueue, new HtmlParserResponseDTO(taskId, true, "OK", parsedHtml, parsedImages));

        awaitMessageIn(htmlPreprocessingRequestQueue);
        assertOtherQueuesEmpty(htmlPreprocessingRequestQueue);
        assertThat(statusOf(taskId)).isEqualTo(TaskStatusProto.HTML_PREPROCESSING);

        HtmlPreprocessingRequestDTO sentPreprocessingRequest =
                JsonMapper.stringToObject(receive(htmlPreprocessingRequestQueue), HtmlPreprocessingRequestDTO.class);
        assertThat(sentPreprocessingRequest.taskId()).isEqualTo(taskId);
        // htmlDocs из ответа парсера доехали до запроса препроцессинга
        assertThat(sentPreprocessingRequest.htmlDocs())
                .extracting(FileInfoDTO::filePath)
                .containsExactly("out/html/page.html");

        // --- этап 3: отвечаем от html_preprocessing, ждём text_recognition ---
        List<FileInfoDTO> preprocessedHtml = List.of(fileInfoDto("out/html/page.clean.html", FileTypeDto.HTML));
        publish(htmlPreprocessingResponseQueue, new HtmlPreprocessingResponseDTO(taskId, true, "OK", preprocessedHtml));

        awaitMessageIn(textRecognitionRequestQueue);
        assertOtherQueuesEmpty(textRecognitionRequestQueue);

        TextRecognitionRequestDTO sentRecognitionRequest =
                JsonMapper.stringToObject(receive(textRecognitionRequestQueue), TextRecognitionRequestDTO.class);
        assertThat(sentRecognitionRequest.taskId()).isEqualTo(taskId);
        // images из ответа парсера доехали до запроса распознавания
        assertThat(sentRecognitionRequest.images())
                .extracting(FileInfoDTO::filePath)
                .containsExactly("out/imgs/a.png", "out/imgs/b.png");

        // --- этап 4: отвечаем от text_recognition, LLM отрабатывает синхронно ---
        List<FileInfoDTO> recognizedImages = List.of(
                fileInfoDtoWithDescription("out/imgs/a.png", "текст с картинки A"),
                fileInfoDtoWithDescription("out/imgs/b.png", "текст с картинки B"));
        publish(textRecognitionResponseQueue, new TextRecognitionResponseDTO(taskId, true, "OK", recognizedImages));

        await().atMost(10, SECONDS).until(() -> statusOf(taskId) == TaskStatusProto.DONE);
        assertOtherQueuesEmpty(""); // ВСЕ очереди пусты

        // --- финальный результат ---
        TaskResultOrchestratorProto result = resultOf(taskId);
        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getHtmlParserResponse().getSuccess()).isTrue();
        assertThat(result.getHtmlPreprocessingResponse().getSuccess()).isTrue();
        assertThat(result.getTextRecognitionResponse().getSuccess()).isTrue();
        assertThat(result.getLlmResponse().getSuccess()).isTrue();
        assertThat(result.getLlmResponse().getLlmOutput()).isEqualTo("LLM OUTPUT");
    }

    /*
    Проверяет, что LLM-запрос получает данные, накопленные предыдущими этапами:
    - htmlDocs из ответа html_parser
    - images (с распознанным текстом в description) из ответа text_recognition
    */
    @Test
    void llmRequestAccumulatesUpstreamData() throws Exception {
        String taskId = "task-llm-context";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .setHtmlParserRequest(htmlParserRequestProto(taskId))
                .setTextRecognitionRequest(textRecognitionRequestProto(taskId))
                .setLlmRequest(llmRequestProto(taskId))
                .build());

        awaitMessageIn(htmlParserRequestQueue);
        receive(htmlParserRequestQueue);

        publish(htmlParserResponseQueue, new HtmlParserResponseDTO(taskId, true, "OK",
                List.of(fileInfoDto("out/html/page.html", FileTypeDto.HTML)),
                List.of(fileInfoDto("out/imgs/a.png", FileTypeDto.IMG))));

        awaitMessageIn(textRecognitionRequestQueue);
        receive(textRecognitionRequestQueue);

        publish(textRecognitionResponseQueue, new TextRecognitionResponseDTO(taskId, true, "OK",
                List.of(fileInfoDtoWithDescription("out/imgs/a.png", "распознанный текст"))));

        await().atMost(10, SECONDS).until(() -> statusOf(taskId) == TaskStatusProto.DONE);

        Task stored = repository.findById(taskId).orElseThrow();
        assertThat(stored.getLlmRequest().getHtmlDocs())
                .extracting(doc -> doc.getFilePath())
                .contains("out/html/page.html");
        assertThat(stored.getLlmRequest().getImages())
                .extracting(image -> image.getDescription())
                .contains("распознанный текст");
    }


    // ==================================================================
    // ===================== ЧАСТИЧНЫЕ ПАЙПЛАЙНЫ ========================
    // ==================================================================

    /*
    Проверяет пайплайн, в котором требуется только html_parser:
    - запрос уходит в очередь парсера
    - после успешного ответа задача сразу переходит в DONE, минуя остальные этапы
    */
    @Test
    void pipelineWithOnlyHtmlParser() throws Exception {
        String taskId = "task-parser-only";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .setHtmlParserRequest(htmlParserRequestProto(taskId))
                .build());

        awaitMessageIn(htmlParserRequestQueue);
        receive(htmlParserRequestQueue);

        publish(htmlParserResponseQueue, new HtmlParserResponseDTO(taskId, true, "OK", List.of(), List.of()));

        await().atMost(10, SECONDS).until(() -> statusOf(taskId) == TaskStatusProto.DONE);
        assertOtherQueuesEmpty("");
    }

    /*
    Проверяет пайплайн, в котором требуется только LLM:
    - RabbitMQ вообще не задействован, LLM отрабатывает синхронно
    - задача сразу переходит в DONE, результат содержит ответ модели
    */
    @Test
    void pipelineWithOnlyLlm() {
        String taskId = "task-llm-only";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .setLlmRequest(llmRequestProto(taskId))
                .build());

        await().atMost(10, SECONDS).until(() -> statusOf(taskId) == TaskStatusProto.DONE);
        assertOtherQueuesEmpty("");
        assertThat(resultOf(taskId).getLlmResponse().getLlmOutput()).isEqualTo("LLM OUTPUT");
    }

    /*
    Проверяет запуск задачи без единого этапа:
    - ни один сервис не требуется, пайплайн завершается сразу
    - задача создаётся в Redis и получает статус DONE
    */
    @Test
    void pipelineWithoutAnyStage() {
        String taskId = "task-empty";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .build());

        await().atMost(10, SECONDS).until(() -> statusOf(taskId) == TaskStatusProto.DONE);
        assertOtherQueuesEmpty("");
        assertThat(repository.findById(taskId)).isPresent();
    }

    /*
    Проверяет, что startParsing сохраняет задачу в Redis со всеми флагами required:
    - требуемые этапы отмечены true, остальные false
    - начальный статус CREATED сменяется первым этапом пайплайна
    */
    @Test
    void startParsingPersistsTaskInRedis() throws Exception {
        String taskId = "task-persist";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .setHtmlParserRequest(htmlParserRequestProto(taskId))
                .setLlmRequest(llmRequestProto(taskId))
                .build());

        awaitMessageIn(htmlParserRequestQueue);

        Task stored = repository.findById(taskId).orElseThrow();
        assertThat(stored.getUserId()).isEqualTo(USER_ID);
        assertThat(stored.isHtmlParserRequired()).isTrue();
        assertThat(stored.isHtmlPreprocessingRequired()).isFalse();
        assertThat(stored.isTextRecognitionRequired()).isFalse();
        assertThat(stored.isLlmRequired()).isTrue();
        assertThat(stored.getCreatedAt()).isNotNull();
    }


    // ==================================================================
    // ========================== ОШИБКИ ЭТАПОВ =========================
    // ==================================================================

    /*
    Проверяет обработку неуспешного ответа от html_parser:
    - пайплайн останавливается, следующий этап не запускается
    - задача переходит в FAILED, сообщение об ошибке сохраняется
    */
    @Test
    void htmlParserFailureStopsPipeline() throws Exception {
        String taskId = "task-parser-fail";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .setHtmlParserRequest(htmlParserRequestProto(taskId))
                .setHtmlPreprocessingRequest(htmlPreprocessingRequestProto(taskId))
                .build());

        awaitMessageIn(htmlParserRequestQueue);
        receive(htmlParserRequestQueue);

        publish(htmlParserResponseQueue,
                new HtmlParserResponseDTO(taskId, false, "страница недоступна", List.of(), List.of()));

        await().atMost(10, SECONDS).until(() -> statusOf(taskId) == TaskStatusProto.FAILED);

        // следующий этап не запустился
        assertOtherQueuesEmpty("");
        assertThat(messageOf(taskId)).isEqualTo("страница недоступна");
    }

    /*
    Проверяет обработку неуспешного ответа от html_preprocessing:
    - задача переходит в FAILED, text_recognition не запускается
    */
    @Test
    void htmlPreprocessingFailureStopsPipeline() throws Exception {
        String taskId = "task-preprocessing-fail";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .setHtmlPreprocessingRequest(htmlPreprocessingRequestProto(taskId))
                .setTextRecognitionRequest(textRecognitionRequestProto(taskId))
                .build());

        awaitMessageIn(htmlPreprocessingRequestQueue);
        receive(htmlPreprocessingRequestQueue);

        publish(htmlPreprocessingResponseQueue,
                new HtmlPreprocessingResponseDTO(taskId, false, "битый html", List.of()));

        await().atMost(10, SECONDS).until(() -> statusOf(taskId) == TaskStatusProto.FAILED);
        assertOtherQueuesEmpty("");
        assertThat(messageOf(taskId)).isEqualTo("битый html");
    }

    /*
    Проверяет обработку неуспешного ответа от text_recognition:
    - задача переходит в FAILED, LLM-этап не запускается
    */
    @Test
    void textRecognitionFailureStopsPipeline() throws Exception {
        String taskId = "task-recognition-fail";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .setTextRecognitionRequest(textRecognitionRequestProto(taskId))
                .setLlmRequest(llmRequestProto(taskId))
                .build());

        awaitMessageIn(textRecognitionRequestQueue);
        receive(textRecognitionRequestQueue);

        publish(textRecognitionResponseQueue,
                new TextRecognitionResponseDTO(taskId, false, "OCR упал", List.of()));

        await().atMost(10, SECONDS).until(() -> statusOf(taskId) == TaskStatusProto.FAILED);
        assertThat(messageOf(taskId)).isEqualTo("OCR упал");
    }

    /*
    Проверяет обработку неуспешного ответа LLM-сервиса:
    - LLMService возвращает success=false (например, модель недоступна)
    - задача переходит в FAILED с сообщением от сервиса
    */
    @Test
    void llmFailureMarksTaskFailed() {
        String taskId = "task-llm-fail";

        doAnswer(invocation -> {
            LLMRequestDTO request = invocation.getArgument(0);
            return new LLMResponseDTO(request.taskId(), false, "[llm service] Unknown model", null);
        }).when(llmService).processLlmRequest(any());

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .setLlmRequest(llmRequestProto(taskId))
                .build());

        await().atMost(10, SECONDS).until(() -> statusOf(taskId) == TaskStatusProto.FAILED);
        assertThat(messageOf(taskId)).contains("Unknown model");
    }


    // ==================================================================
    // ========================= GET TASK STATUS ========================
    // ==================================================================

    /*
    Проверяет getTaskStatus для незарегистрированной задачи:
    - сервис не бросает ошибку, а возвращает статус NOT_REGISTERED
    */
    @Test
    void getTaskStatusForUnknownTask() {
        TaskStatusOrchestratorProto status = storageStub.getTaskStatus(
                GetTaskStatusOrchestratorRequest.newBuilder()
                        .setTaskId("no-such-task")
                        .setUserId(USER_ID)
                        .build()).getTaskStatusOrchestrator();

        assertThat(status.getStatus()).isEqualTo(TaskStatusProto.NOT_REGISTERED);
    }

    /*
    Проверяет разграничение доступа в getTaskStatus:
    - задача принадлежит одному пользователю, запрос приходит от другого
    - эндпоинт заворачивает любую RuntimeException в NOT_FOUND
    */
    @Test
    void getTaskStatusForeignUserNotFoundException() {
        String taskId = "task-status-foreign";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .build());

        await().atMost(10, SECONDS).until(() -> repository.findById(taskId).isPresent());

        GetTaskStatusOrchestratorRequest request = GetTaskStatusOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId("intruder")
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> storageStub.getTaskStatus(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("userId does not match");
    }


    // ==================================================================
    // ========================= GET TASK RESULT ========================
    // ==================================================================

    /*
    Проверяет getTaskResult для несуществующей задачи:
    - TaskService бросает TaskNotFoundException, эндпоинт отдаёт NOT_FOUND
    */
    @Test
    void getTaskResultForUnknownTaskNotFoundException() {
        GetTaskResultOrchestratorRequest request = GetTaskResultOrchestratorRequest.newBuilder()
                .setTaskId("no-such-task")
                .setUserId(USER_ID)
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> storageStub.getTaskResult(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    /*
    Проверяет разграничение доступа в getTaskResult.
    ВНИМАНИЕ: та же ситуация, что в getTaskStatus, но код ответа другой — UNAVAILABLE
    вместо NOT_FOUND, потому что getTaskResult ловит TaskNotFoundException и RuntimeException
    по отдельности. Тест фиксирует текущее (несогласованное) поведение.
    */
    @Test
    void getTaskResultForeignUserUnavailableException() {
        String taskId = "task-result-foreign";

        startStub.startParsing(StartParsingOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .build());

        await().atMost(10, SECONDS).until(() -> repository.findById(taskId).isPresent());

        GetTaskResultOrchestratorRequest request = GetTaskResultOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId("intruder")
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> storageStub.getTaskResult(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(ex.getStatus().getDescription()).contains("userId does not match");
    }


    // ==================================================================
    // =========================== HELPERS ==============================
    // ==================================================================

    private List<String> allQueues() {
        return List.of(
                htmlParserRequestQueue, htmlParserResponseQueue,
                htmlPreprocessingRequestQueue, htmlPreprocessingResponseQueue,
                textRecognitionRequestQueue, textRecognitionResponseQueue);
    }

    private void assertOtherQueuesEmpty(String targetQueueName) {
        List<String> queueNames = new ArrayList<>(allQueues());
        queueNames.remove(targetQueueName);
        queueNames.forEach(queueName -> assertThat(messageCount(queueName))
                .withFailMessage("очередь %s должна быть пустой", queueName)
                .isEqualTo(0));
    }

    private void awaitMessageIn(String queueName) {
        await().atMost(10, SECONDS).until(() -> messageCount(queueName) == 1);
    }

    private int messageCount(String queueName) {
        return (int) amqpAdmin.getQueueProperties(queueName).get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
    }

    private String receive(String queueName) {
        return new String(rabbitTemplate.receive(queueName).getBody());
    }

    private void publish(String queueName, Object payload) throws Exception {
        rabbitTemplate.convertAndSend(queueName, JsonMapper.objectToString(payload));
    }

    private TaskStatusProto statusOf(String taskId) {
        return storageStub.getTaskStatus(GetTaskStatusOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .build()).getTaskStatusOrchestrator().getStatus();
    }

    private String messageOf(String taskId) {
        return storageStub.getTaskStatus(GetTaskStatusOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .build()).getTaskStatusOrchestrator().getMessage();
    }

    private TaskResultOrchestratorProto resultOf(String taskId) {
        return storageStub.getTaskResult(GetTaskResultOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(USER_ID)
                .build()).getTaskResultOrchestrator();
    }

    // --- фабрики proto-запросов ---

    private HtmlParserRequestProto htmlParserRequestProto(String taskId) {
        return HtmlParserRequestProto.newBuilder()
                .setTaskId(taskId)
                .setUrl("https://www.google.com/")
                .setHtmlOutDir("out/html")
                .setImagesOutDir("out/imgs")
                .setDownloadImages(true)
                .putAllHeaders(Map.of("User-Agent", "Mozilla/5.0"))
                .putAllCookies(Map.of("cf_clearance", "abc"))
                .putAllProxy(Map.of("ip", "127.0.0.1", "port", "5050"))
                .setPageComplexity("DIFFICULT")
                .setAdditionalPageLoadTimeoutS(5)
                .build();
    }

    private HtmlPreprocessingRequestProto htmlPreprocessingRequestProto(String taskId) {
        return HtmlPreprocessingRequestProto.newBuilder()
                .setTaskId(taskId)
                .setNoscriptProcessing(true)
                .setScriptProcessing(true)
                .setStyleProcessing(true)
                .setImgProcessing(true)
                .build();
    }

    private TextRecognitionRequestProto textRecognitionRequestProto(String taskId) {
        return TextRecognitionRequestProto.newBuilder()
                .setTaskId(taskId)
                .build();
    }

    private LLMRequestProto llmRequestProto(String taskId) {
        return LLMRequestProto.newBuilder()
                .setTaskId(taskId)
                .setModelName("YandexGPT")
                .setSystemMessage("Ты — парсер")
                .setUserMessage("Извлеки данные")
                .setTemperature(0.2)
                .setMaxOutputTokens(2500)
                .build();
    }

    // --- фабрики DTO ---

    private enum FileTypeDto {HTML, IMG}

    private FileInfoDTO fileInfoDto(String filePath, FileTypeDto type) {
        return FileInfoDTO.builder()
                .filePath(filePath)
                .fileName(filePath.substring(filePath.lastIndexOf('/') + 1))
                .fileType(type == FileTypeDto.HTML
                        ? io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.FileType.HTML
                        : io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.FileType.IMG)
                .sizeBytes(1024L)
                .description("")
                .valid(true)
                .errorMessage("")
                .build();
    }

    private FileInfoDTO fileInfoDtoWithDescription(String filePath, String description) {
        return FileInfoDTO.builder()
                .filePath(filePath)
                .fileName(filePath.substring(filePath.lastIndexOf('/') + 1))
                .fileType(io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.FileType.IMG)
                .sizeBytes(2048L)
                .description(description)
                .valid(true)
                .errorMessage("")
                .build();
    }
}
