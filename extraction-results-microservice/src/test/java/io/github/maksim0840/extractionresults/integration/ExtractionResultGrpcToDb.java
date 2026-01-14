package io.github.maksim0840.extractionresults.integration;

import io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest;
import io.github.maksim0840.extraction_result.v1.ExtractionResultProto;
import io.github.maksim0840.extraction_result.v1.ExtractionResultServiceGrpc;
import io.github.maksim0840.extractionresults.domain.ExtractionResult;
import io.github.maksim0840.extractionresults.repository.ExtractionResultRepository;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoJsonMapper;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для проверки корректности работы grpc сервера extraction-results.
 * Проверяется подключение, отправка запросов через blockingStub, получение запроса,
 * выполнение действий в базе данных и результат запроса
 */
@Testcontainers // включаем работу test-контейнеров (docker)
@SpringBootTest(properties = {
        "grpc.server.inProcessName=test",      // включаем in-process server (клиент и сервер общаются внутри одного JVM-процесса)
        "grpc.server.port=-1",                 // выключаем внешний server (не отдаем порт наружу)
        "grpc.client.inProcess.address=in-process:test" // подключаем клиента к in-process серверу
})
public class ExtractionResultGrpcToDb {

    // Testcontainers класс для запуска Docker-контейнера с базой данных внутри
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    // Подмена динамических spring свойств для подключения к тестовой базе данных
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    // Объект для отправки gRPC запросов серверу
    @GrpcClient("inProcess")
    private ExtractionResultServiceGrpc.ExtractionResultServiceBlockingStub blockingStub;

    // Репозиторий для отправки запросов к базе данных
    @Autowired
    ExtractionResultRepository repository;

    // Очищаем базу перед каждым новым тестом
    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    /*
    Проверяет happy-path для create:
    - gRPC create корректно возвращает заполненный ExtractionResult (id, url, userId, jsonResult, createdAt)
    - создаются две независимые записи (id разные)
    - обе записи действительно сохраняются в MongoDB и содержимое совпадает с запросом
    */
    @Test
    void createSeveral() {
        // Создаём запросы
        Map<String, Object> map1 = Map.of(
                "age", 40.0);
        CreateExtractionResultRequest request1 = CreateExtractionResultRequest.newBuilder()
                .setUrl("https://github.com/")
                .setUserId("5c3c63db-1902-442a-a9bb-ca9facaa609d")
                .setJsonResult(ProtoJsonMapper.mapToStruct(map1))
                .build();

        Map<String, Object> map2 = Map.of(
                "age", 30.0,
                "name", "Boris");
        CreateExtractionResultRequest request2 = CreateExtractionResultRequest.newBuilder()
                .setUrl("url_2")
                .setUserId("10")
                .setJsonResult(ProtoJsonMapper.mapToStruct(map2))
                .build();

        // Отправляем запросы на сервер и получаем ответы
        Instant timeBefore = Instant.now();
        ExtractionResultProto responseProto1 = blockingStub.create(request1).getExtractionResult();
        ExtractionResultProto responseProto2 = blockingStub.create(request2).getExtractionResult();
        Instant timeAfter = Instant.now();

        // Проверяем валидность полей ответа от gRPC сервера
        assertExtractionResultProtoValidity(request1.getUrl(), request1.getUserId(), map1, responseProto1, timeBefore, timeAfter);
        assertExtractionResultProtoValidity(request2.getUrl(), request2.getUserId(), map2, responseProto2, timeBefore, timeAfter);
        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());

        // Получаем результаты записи в бд
        ExtractionResult responseRepo1 = repository.findById(responseProto1.getId()).orElseThrow();
        ExtractionResult responseRepo2 = repository.findById(responseProto2.getId()).orElseThrow();

        // Проверяем валидность записанной в базу данных информации
        assertExtractionResultValidity(request1.getUrl(), request1.getUserId(), map1, responseRepo1, timeBefore, timeAfter);
        assertExtractionResultValidity(request2.getUrl(), request2.getUserId(), map2, responseRepo2, timeBefore, timeAfter);
        assertThat(responseRepo1.getId()).isNotEqualTo(responseRepo2.getId());
        assertThat(repository.count()).isEqualTo(2);
    }

    /*
    Проверяет поведение create при отсутствии userId в запросе:
    - сервер принимает запрос с неуказанным userId (в protobuf это становится пустой строкой)
    - в ответе и в базе userId сохраняется как ""
    - запись успешно создаётся и доступна по id
    */
    @Test
    void createWithSkippedParam() {
        Map<String, Object> map = Map.of(
                "online", true);
        CreateExtractionResultRequest request = CreateExtractionResultRequest.newBuilder()
                .setUrl("not-a-url:just-a-string")
                .setJsonResult(ProtoJsonMapper.mapToStruct(map))
                .build();

        Instant timeBefore = Instant.now();
        ExtractionResultProto responseProto = blockingStub.create(request).getExtractionResult();
        Instant timeAfter = Instant.now();

        assertExtractionResultProtoValidity(request.getUrl(), "", map, responseProto, timeBefore, timeAfter);

        ExtractionResult responseRepo = repository.findById(responseProto.getId()).orElseThrow();

        assertExtractionResultValidity(request.getUrl(), "", map, responseRepo, timeBefore, timeAfter);
        assertThat(repository.count()).isEqualTo(1);
    }

   /*
    Проверяет, что create корректно обрабатывает пустой jsonResult:
    - запрос с jsonResult = {} успешно создаёт запись
    - в ответе и в Mongo сохраняется пустая Map без потери данных
    - остальные поля (id/createdAt/url/userId) заполнены корректно
    */
    @Test
    void createWithEmptyJson() {
        Map<String, Object> map = Map.of();
        CreateExtractionResultRequest request = CreateExtractionResultRequest.newBuilder()
                .setUrl("http://example.com/page?id=42")
                .setUserId("user user")
                .setJsonResult(ProtoJsonMapper.mapToStruct(map))
                .build();

        Instant timeBefore = Instant.now();
        ExtractionResultProto responseProto = blockingStub.create(request).getExtractionResult();
        Instant timeAfter = Instant.now();

        assertExtractionResultProtoValidity(request.getUrl(), request.getUserId(), map, responseProto, timeBefore, timeAfter);

        ExtractionResult responseRepo = repository.findById(responseProto.getId()).orElseThrow();

        assertExtractionResultValidity(request.getUrl(), request.getUserId(),  map, responseRepo, timeBefore, timeAfter);
        assertThat(repository.count()).isEqualTo(1);
    }

    /*
    Проверяет сохранение и возврат вложенной структуры jsonResult (nested JSON):
    - gRPC create принимает многоуровневую Map и не "сплющивает" структуру
    - вложенные поля сохраняются без искажений и читаются обратно из Mongo
    - ответ сервера соответствует сохранённым данным
    */
    @Test
    void createWithNestedJson() {
        Map<String, Object> map = Map.of(
                "cars", Map.of(
                        "car1", Map.of(
                                "model", "Ford",
                                "year", "2014"
                        ),
                        "car2", "MBV 2012"
                )
        );
        CreateExtractionResultRequest request = CreateExtractionResultRequest.newBuilder()
                .setUrl("some string")
                .setUserId("id=500")
                .setJsonResult(ProtoJsonMapper.mapToStruct(map))
                .build();

        Instant timeBefore = Instant.now();
        ExtractionResultProto responseProto = blockingStub.create(request).getExtractionResult();
        Instant timeAfter = Instant.now();

        assertExtractionResultProtoValidity(request.getUrl(), request.getUserId(), map, responseProto, timeBefore, timeAfter);

        ExtractionResult responseRepo = repository.findById(responseProto.getId()).orElseThrow();

        assertExtractionResultValidity(request.getUrl(), request.getUserId(), map, responseRepo, timeBefore, timeAfter);
        assertThat(repository.count()).isEqualTo(1);
    }

    private void assertExtractionResultProtoValidity(
            String url,
            String userId,
            Map<String, Object> map,
            ExtractionResultProto proto,
            Instant timeBefore,
            Instant timeAfter
    ) {
        assertThat(proto.getId()).isNotBlank();
        assertThat(ObjectId.isValid(proto.getId())).isTrue();

        assertThat(proto.getUrl()).isEqualTo(url);
        assertThat(proto.getUserId()).isEqualTo(userId);
        assertThat(ProtoJsonMapper.structToMap(proto.getJsonResult())).isEqualTo(map);

        assertThat(ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()))
                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));

    }

    private void assertExtractionResultValidity(
            String url,
            String userId,
            Map<String, Object> map,
            ExtractionResult domain,
            Instant timeBefore,
            Instant timeAfter
    ) {
        assertThat(domain.getId()).isNotBlank();
        assertThat(ObjectId.isValid(domain.getId())).isTrue();

        assertThat(domain.getUrl()).isEqualTo(url);
        assertThat(domain.getUserId()).isEqualTo(userId);
        assertThat(domain.getJsonResult()).isEqualTo(map);

        assertThat(domain.getCreatedAt())
                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));

    }
}
