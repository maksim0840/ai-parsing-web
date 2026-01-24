//package io.github.maksim0840.parsingparams.integration;
//
//import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
//import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoJsonMapper;
//import io.github.maksim0840.parsing_param.v1.ParsingParamServiceGrpc;
//import io.github.maksim0840.usersinfo.repository.ParsingParamRepository;
//import io.grpc.Status;
//import io.grpc.StatusRuntimeException;
//import net.devh.boot.grpc.client.inject.GrpcClient;
//import org.junit.jupiter.api.BeforeEach;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.postgresql.PostgreSQLContainer;
//
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
///**
// * Тесты для проверки корректности работы grpc сервера extraction-results.
// * Проверяется подключение, отправка запросов через blockingStub, получение запроса,
// * выполнение действий в базе данных и результат запроса
// */
//@Testcontainers // включаем работу test-контейнеров (docker)
//@SpringBootTest(properties = {
//        "grpc.server.inProcessName=test",      // включаем in-process server (клиент и сервер общаются внутри одного JVM-процесса)
//        "grpc.server.port=-1",                 // выключаем внешний server (не отдаем порт наружу)
//        "grpc.client.inProcess.address=in-process:test" // подключаем клиента к in-process серверу
//})
//public class ParsingParamGrpcToDb {
//
//    // Testcontainers класс для запуска Docker-контейнера с базой данных внутри
//    @Container
//    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("testdb").withUsername("test").withPassword("test");
//
//    // Подмена динамических spring свойств для подключения к тестовой базе данных
//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
//        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
//        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
//    }
//
//    // Объект для отправки gRPC запросов серверу
//    @GrpcClient("inProcess")
//    private ParsingParamServiceGrpc.ParsingParamServiceBlockingStub blockingStub;
//
//    // Репозиторий для отправки запросов к базе данных
//    @Autowired
//    ParsingParamRepository repository;
//
//    // Объект для более низкоуровневых операций с базой данных (по сравнению с репозиторием)
////    @Autowired
////    private Postgresqlt mongoTemplate;
//
//    // Очищаем базу перед каждым новым тестом
//    @BeforeEach
//    void cleanDb() {
//        repository.deleteAll();
//    }
//
//    /*
//    Проверяет happy-path для create:
//    - gRPC create корректно возвращает заполненный ExtractionResult (id, url, userId, jsonResult, createdAt)
//    - создаются две независимые записи (id разные)
//    - обе записи действительно сохраняются в MongoDB и содержимое совпадает с запросом
//    */
//    @Test
//    void createSeveral() {
//        // Создаём запросы
//        Map<String, Object> map1 = Map.of(
//                "age", 40.0);
//        CreateExtractionResultRequest request1 = CreateExtractionResultRequest.newBuilder()
//                .setUrl("https://github.com/")
//                .setUserId("5c3c63db-1902-442a-a9bb-ca9facaa609d")
//                .setJsonResult(ProtoJsonMapper.mapToStruct(map1))
//                .build();
//
//        Map<String, Object> map2 = Map.of(
//                "age", 30.0,
//                "name", "Boris");
//        CreateExtractionResultRequest request2 = CreateExtractionResultRequest.newBuilder()
//                .setUrl("url_2")
//                .setUserId("10")
//                .setJsonResult(ProtoJsonMapper.mapToStruct(map2))
//                .build();
//
//        // Отправляем запросы на сервер и получаем ответы
//        Instant timeBefore = Instant.now();
//        ExtractionResultProto responseProto1 = blockingStub.create(request1).getExtractionResult();
//        ExtractionResultProto responseProto2 = blockingStub.create(request2).getExtractionResult();
//        Instant timeAfter = Instant.now();
//
//        // Проверяем валидность полей ответа от gRPC сервера
//        assertExtractionResultProtoValidity(request1.getUrl(), request1.getUserId(), map1, responseProto1, timeBefore, timeAfter);
//        assertExtractionResultProtoValidity(request2.getUrl(), request2.getUserId(), map2, responseProto2, timeBefore, timeAfter);
//        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());
//
//        // Получаем результаты записи в бд
//        ExtractionResult responseRepo1 = repository.findById(responseProto1.getId()).orElseThrow();
//        ExtractionResult responseRepo2 = repository.findById(responseProto2.getId()).orElseThrow();
//
//        // Проверяем валидность записанной в базу данных информации
//        assertExtractionResultValidity(request1.getUrl(), request1.getUserId(), map1, responseRepo1, timeBefore, timeAfter);
//        assertExtractionResultValidity(request2.getUrl(), request2.getUserId(), map2, responseRepo2, timeBefore, timeAfter);
//        assertThat(responseRepo1.getId()).isNotEqualTo(responseRepo2.getId());
//        assertThat(repository.count()).isEqualTo(2);
//    }
//
//    /*
//    Проверяет поведение create при отсутствии userId в запросе:
//    - сервер принимает запрос с неуказанным userId (в protobuf это становится пустой строкой)
//    - в ответе и в базе userId сохраняется как ""
//    - запись успешно создаётся и доступна по id
//    */
//    @Test
//    void createWithSkippedParam() {
//        Map<String, Object> map = Map.of(
//                "online", true);
//        CreateExtractionResultRequest request = CreateExtractionResultRequest.newBuilder()
//                .setUrl("not-a-url:just-a-string")
//                .setJsonResult(ProtoJsonMapper.mapToStruct(map))
//                .build();
//
//        Instant timeBefore = Instant.now();
//        ExtractionResultProto responseProto = blockingStub.create(request).getExtractionResult();
//        Instant timeAfter = Instant.now();
//
//        assertExtractionResultProtoValidity(request.getUrl(), "", map, responseProto, timeBefore, timeAfter);
//
//        ExtractionResult responseRepo = repository.findById(responseProto.getId()).orElseThrow();
//
//        assertExtractionResultValidity(request.getUrl(), "", map, responseRepo, timeBefore, timeAfter);
//        assertThat(repository.count()).isEqualTo(1);
//    }
//
//    /*
//     Проверяет, что create корректно обрабатывает пустой jsonResult:
//     - запрос с jsonResult = {} успешно создаёт запись
//     - в ответе и в Mongo сохраняется пустая Map без потери данных
//     - остальные поля (id/createdAt/url/userId) заполнены корректно
//     */
//    @Test
//    void createWithEmptyJson() {
//        Map<String, Object> map = Map.of();
//        CreateExtractionResultRequest request = CreateExtractionResultRequest.newBuilder()
//                .setUrl("http://example.com/page?id=42")
//                .setUserId("user user")
//                .setJsonResult(ProtoJsonMapper.mapToStruct(map))
//                .build();
//
//        Instant timeBefore = Instant.now();
//        ExtractionResultProto responseProto = blockingStub.create(request).getExtractionResult();
//        Instant timeAfter = Instant.now();
//
//        assertExtractionResultProtoValidity(request.getUrl(), request.getUserId(), map, responseProto, timeBefore, timeAfter);
//
//        ExtractionResult responseRepo = repository.findById(responseProto.getId()).orElseThrow();
//
//        assertExtractionResultValidity(request.getUrl(), request.getUserId(),  map, responseRepo, timeBefore, timeAfter);
//        assertThat(repository.count()).isEqualTo(1);
//    }
//
//    /*
//    Проверяет сохранение и возврат вложенной структуры jsonResult (nested JSON):
//    - gRPC create принимает многоуровневую Map и не "сплющивает" структуру
//    - вложенные поля сохраняются без искажений и читаются обратно из Mongo
//    - ответ сервера соответствует сохранённым данным
//    */
//    @Test
//    void createWithNestedJson() {
//        Map<String, Object> map = Map.of(
//                "cars", Map.of(
//                        "car1", Map.of(
//                                "model", "Ford",
//                                "year", "2014"
//                        ),
//                        "car2", "MBV 2012"
//                )
//        );
//        CreateExtractionResultRequest request = CreateExtractionResultRequest.newBuilder()
//                .setUrl("some string")
//                .setUserId("id=500")
//                .setJsonResult(ProtoJsonMapper.mapToStruct(map))
//                .build();
//
//        Instant timeBefore = Instant.now();
//        ExtractionResultProto responseProto = blockingStub.create(request).getExtractionResult();
//        Instant timeAfter = Instant.now();
//
//        assertExtractionResultProtoValidity(request.getUrl(), request.getUserId(), map, responseProto, timeBefore, timeAfter);
//
//        ExtractionResult responseRepo = repository.findById(responseProto.getId()).orElseThrow();
//
//        assertExtractionResultValidity(request.getUrl(), request.getUserId(), map, responseRepo, timeBefore, timeAfter);
//        assertThat(repository.count()).isEqualTo(1);
//    }
//
//    @Test
//    void getExistingData() {
//        Map<String, Object> map1 = Map.of(
//                "profile", Map.of(
//                        "city", "London",
//                        "zip", "SW1A 1AA"
//                )
//        );
//        ExtractionResult entity1 = new ExtractionResult("localhost:8080/api/v1/items", "admin; DROP TABLE users;", map1);
//
//        Map<String, Object> map2 = Map.of(
//                "numbers", List.of(1.0, 2.5, 3.0)
//        );
//        ExtractionResult entity2 = new ExtractionResult("abc".repeat(50), "\uD83D\uDD25user\uD83D\uDD25", map2);
//
//        Instant timeBefore = Instant.now();
//        repository.save(entity1);
//        repository.save(entity2);
//        Instant timeAfter = Instant.now();
//
//        GetExtractionResultRequest request1 = GetExtractionResultRequest.newBuilder().setId(entity1.getId()).build();
//        GetExtractionResultRequest request2 = GetExtractionResultRequest.newBuilder().setId(entity2.getId()).build();
//        ExtractionResultProto responseProto1 = blockingStub.get(request1).getExtractionResult();
//        ExtractionResultProto responseProto2 = blockingStub.get(request2).getExtractionResult();
//
//        assertExtractionResultDomainProtoValidity(entity1, responseProto1, timeBefore, timeAfter);
//        assertExtractionResultDomainProtoValidity(entity2, responseProto2, timeBefore, timeAfter);
//        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());
//    }
//
//
//    @Test
//    void getWrongIdNotFoundException() {
//        ExtractionResult entity = new ExtractionResult("url", "userid", Map.of("age", 40));
//
//        repository.save(entity);
//
//        GetExtractionResultRequest request = GetExtractionResultRequest.newBuilder().setId("*@$").build();
//
//        // Ожидаем, что при запросе произошла ошибка
//        StatusRuntimeException ex = assertThrows(
//                StatusRuntimeException.class,
//                () -> blockingStub.get(request)
//        );
//
//        // Проверяем подробности ошибки
//        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
//        assertThat(ex.getStatus().getDescription()).contains("not found").contains("*@$");
//    }
//
//    @Test
//    void getListAllParams() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setUserId("4")
//                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-08T00:00:00.000Z")))
//                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-09T00:00:00.000Z")))
//                .setPageNum(0)
//                .setPageSize(100)
//                .setSortCreatedDesc(true)
//                .build();
//
//        List<String> expectedResultIds = List.of(expand24("9"), expand24("8"));
//
//        checkGetListRequest(request, expectedResultIds);
//    }
//
//    @Test
//    void getListNoOptionalParams() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<String> expectedResultIds = List.of(expand24("10"), expand24("9"), expand24("8"), expand24("7"), expand24("6"), expand24("5"), expand24("4"), expand24("3"), expand24("2"), expand24("1"));
//
//        checkGetListRequest(request, expectedResultIds);
//    }
//
//    @Test
//    void getListAscSorting() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setPageNum(0)
//                .setPageSize(100)
//                .setSortCreatedDesc(false)
//                .build();
//
//        List<String> expectedResultIds = List.of(expand24("1"), expand24("2"), expand24("3"), expand24("4"), expand24("5"), expand24("6"), expand24("7"), expand24("8"), expand24("9"), expand24("10"));
//
//        checkGetListRequest(request, expectedResultIds);
//    }
//
//    @Test
//    void getListByUserId() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setUserId("3")
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<String> expectedResultIds = List.of(expand24("6"), expand24("5"), expand24("4"));
//
//        checkGetListRequest(request, expectedResultIds);
//    }
//
//    @Test
//    void getListByCreatedFrom() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-05T00:00:00.000Z")))
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<String> expectedResultIds = List.of(expand24("10"), expand24("9"), expand24("8"), expand24("7"), expand24("6"), expand24("5"));
//
//        checkGetListRequest(request, expectedResultIds);
//    }
//
//    @Test
//    void getListByCreatedTo() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-05T00:00:00.000Z")))
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<String> expectedResultIds = List.of(expand24("5"), expand24("4"), expand24("3"), expand24("2"), expand24("1"));
//
//        checkGetListRequest(request, expectedResultIds);
//    }
//
//    @Test
//    void getListDatesBetween() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-01T00:00:00.000Z")))
//                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-03T00:00:00.000Z")))
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<String> expectedResultIds = List.of(expand24("3"), expand24("2"), expand24("1"));
//
//        checkGetListRequest(request, expectedResultIds);
//
//    }
//
//    @Test
//    void getListConflictDatesNoData() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-08T00:00:00.000Z")))
//                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-05T00:00:00.000Z")))
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<String> expectedResultIds = List.of();
//
//        checkGetListRequest(request, expectedResultIds);
//    }
//
//    @Test
//    void getListMidPage() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setPageNum(2)
//                .setPageSize(3)
//                .build();
//
//        List<String> expectedResultIds = List.of(expand24("4"), expand24("3"), expand24("2"));
//
//        checkGetListRequest(request, expectedResultIds);
//    }
//
//    @Test
//    void getListExceedingPageNum() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setPageNum(5)
//                .setPageSize(2)
//                .build();
//
//        List<String> expectedResultIds = List.of();
//
//        checkGetListRequest(request, expectedResultIds);
//    }
//
//    @Test
//    void getListZeroPageSizeUnavailableException() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setPageNum(0)
//                .setPageSize(0)
//                .build();
//
//        // Ожидаем, что при запросе произошла ошибка
//        StatusRuntimeException ex = assertThrows(
//                StatusRuntimeException.class,
//                () -> blockingStub.getList(request)
//        );
//
//        System.out.println(ex.getStatus().getDescription());
//
//        // Проверяем подробности ошибки
//        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
//        assertThat(ex.getStatus().getDescription()).contains("size").contains("less than one");
//    }
//
//    @Test
//    void getListNegativePageNumUnavailableException() {
//        GetListExtractionResultRequest request = GetListExtractionResultRequest.newBuilder()
//                .setPageNum(-1)
//                .setPageSize(37)
//                .build();
//
//        // Ожидаем, что при запросе произошла ошибка
//        StatusRuntimeException ex = assertThrows(
//                StatusRuntimeException.class,
//                () -> blockingStub.getList(request)
//        );
//
//        // Проверяем подробности ошибки
//        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
//        assertThat(ex.getStatus().getDescription()).contains("index").contains("less than zero");
//    }
//
//    @Test
//    void deleteExistingData() {
//        Map<String, Object> map1 = Map.of(
//                "profile", Map.of(
//                        "city", "London",
//                        "zip", "SW1A 1AA"
//                )
//        );
//        ExtractionResult entity1 = new ExtractionResult("localhost:8080/api/v1/items", "admin; DROP TABLE users;", map1);
//
//        Map<String, Object> map2 = Map.of(
//                "numbers", List.of(1.0, 2.5, 3.0)
//        );
//        ExtractionResult entity2 = new ExtractionResult("abc".repeat(50), "\uD83D\uDD25user\uD83D\uDD25", map2);
//
//        Instant timeBefore = Instant.now();
//        repository.save(entity1);
//        repository.save(entity2);
//        Instant timeAfter = Instant.now();
//
//        DeleteExtractionResultRequest request1 = DeleteExtractionResultRequest.newBuilder().setId(entity1.getId()).build();
//        DeleteExtractionResultRequest request2 = DeleteExtractionResultRequest.newBuilder().setId(entity2.getId()).build();
//
//        assertThat(repository.count()).isEqualTo(2);
//        blockingStub.delete(request1);
//        assertThat(repository.count()).isEqualTo(1);
//        blockingStub.delete(request2);
//        assertThat(repository.count()).isEqualTo(0);
//    }
//
//
//    @Test
//    void deleteWrongIdNotFoundException() {
//        ExtractionResult entity = new ExtractionResult("url", "userid", Map.of("age", 40));
//
//        repository.save(entity);
//
//        DeleteExtractionResultRequest request = DeleteExtractionResultRequest.newBuilder().setId("*@$").build();
//
//        // Ожидаем, что при запросе произошла ошибка
//        StatusRuntimeException ex = assertThrows(
//                StatusRuntimeException.class,
//                () -> blockingStub.delete(request)
//        );
//
//        // Проверяем подробности ошибки
//        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
//        assertThat(ex.getStatus().getDescription()).contains("didn't exist").contains("*@$");
//    }
//
//    void checkGetListRequest(GetListExtractionResultRequest request, List<String> expectedResultIds) {
//        List<ExtractionResult> entities = List.of(
//                ExtractionResult.builder().id(expand24("1")).url("url1").userId("1").jsonResult(Map.of("age", 10.0)).createdAt(Instant.parse("2026-01-01T00:00:00.000Z")).build(),
//                ExtractionResult.builder().id(expand24("2")).url("url2").userId("2").jsonResult(Map.of("age", 20.0)).createdAt(Instant.parse("2026-01-02T00:00:00.000Z")).build(),
//                ExtractionResult.builder().id(expand24("3")).url("url3").userId("2").jsonResult(Map.of("age", 30.0)).createdAt(Instant.parse("2026-01-03T00:00:00.000Z")).build(),
//                ExtractionResult.builder().id(expand24("4")).url("url4").userId("3").jsonResult(Map.of("age", 40.0)).createdAt(Instant.parse("2026-01-04T00:00:00.000Z")).build(),
//                ExtractionResult.builder().id(expand24("5")).url("url5").userId("3").jsonResult(Map.of("age", 50.0)).createdAt(Instant.parse("2026-01-05T00:00:00.000Z")).build(),
//                ExtractionResult.builder().id(expand24("6")).url("url6").userId("3").jsonResult(Map.of("age", 60.0)).createdAt(Instant.parse("2026-01-06T00:00:00.000Z")).build(),
//                ExtractionResult.builder().id(expand24("7")).url("url7").userId("4").jsonResult(Map.of("age", 70.0)).createdAt(Instant.parse("2026-01-07T00:00:00.000Z")).build(),
//                ExtractionResult.builder().id(expand24("8")).url("url8").userId("4").jsonResult(Map.of("age", 80.0)).createdAt(Instant.parse("2026-01-08T00:00:00.000Z")).build(),
//                ExtractionResult.builder().id(expand24("9")).url("url9").userId("4").jsonResult(Map.of("age", 90.0)).createdAt(Instant.parse("2026-01-09T00:00:00.000Z")).build(),
//                ExtractionResult.builder().id(expand24("10")).url("url10").userId("4").jsonResult(Map.of("age", 100.0)).createdAt(Instant.parse("2026-01-10T00:00:00.000Z")).build()
//        );
//
//        // Вставляем данные в базу данных
//        rawDbInsert(entities);
//        assertThat(repository.count()).isEqualTo(entities.size());
//
//        // Отправляем gRPC серверу запрос
//        List<ExtractionResultProto> actualResultsProto = blockingStub.getList(request).getExtractionResultsList();
//        assertThat(actualResultsProto.size()).isEqualTo(expectedResultIds.size());
//
//        // Проверяем порядок и соответствие полей
//        for (int i = 0; i < expectedResultIds.size(); i++) {
//            ExtractionResult expectedResult = repository.findById(expectedResultIds.get(i)).orElseThrow();
//            ExtractionResultProto actualResultProto = actualResultsProto.get(i);
//            assertExtractionResultDomainProtoValidity(expectedResult, actualResultProto, null, null);
//        }
//    }
//
//    // Расширить переданный id нулями до 24 символов "8" -> "000000000000000000000008"
//    String expand24(String id) {
//        return "0".repeat(24 - id.length()) + id;
//    }
//
//    void rawDbInsert(List<ExtractionResult> entities) {
//        String collection = mongoTemplate.getCollectionName(ExtractionResult.class);
//
//        List<Document> docs = entities.stream()
//                .map(e ->
//                        new Document("_id", new ObjectId(e.getId())) // в домене поле id имеет тип String, но _id в MongoDb имеет тип ObjectId
//                        .append("url", e.getUrl())
//                        .append("userId", e.getUserId())
//                        .append("jsonResult", e.getJsonResult()) // Map/List/примитивы ок
//                        .append("createdAt", java.util.Date.from(e.getCreatedAt()))
//                ).toList();
//
//        mongoTemplate.getCollection(collection).insertMany(docs);
//    }
//
//    private void assertExtractionResultProtoValidity(
//            String url,
//            String userId,
//            Map<String, Object> map,
//            ExtractionResultProto proto,
//            Instant timeBefore,
//            Instant timeAfter
//    ) {
//        assertThat(proto.getId()).isNotBlank();
//        assertThat(ObjectId.isValid(proto.getId())).isTrue();
//
//        assertThat(proto.getUrl()).isEqualTo(url);
//        assertThat(proto.getUserId()).isEqualTo(userId);
//        assertThat(ProtoJsonMapper.structToMap(proto.getJsonResult())).isEqualTo(map);
//
//        assertThat(ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()))
//                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
//
//    }
//
//    private void assertExtractionResultValidity(
//            String url,
//            String userId,
//            Map<String, Object> map,
//            ExtractionResult domain,
//            Instant timeBefore,
//            Instant timeAfter
//    ) {
//        assertThat(domain.getId()).isNotBlank();
//        assertThat(ObjectId.isValid(domain.getId())).isTrue();
//
//        assertThat(domain.getUrl()).isEqualTo(url);
//        assertThat(domain.getUserId()).isEqualTo(userId);
//        assertThat(domain.getJsonResult()).isEqualTo(map);
//
//        assertThat(domain.getCreatedAt())
//                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
//
//    }
//
//    private void assertExtractionResultDomainProtoValidity(
//            ExtractionResult domain,
//            ExtractionResultProto proto,
//            Instant timeBefore,
//            Instant timeAfter
//    ) {
//        assertThat(domain.getId()).isEqualTo(proto.getId());
//        assertThat(domain.getUrl()).isEqualTo(proto.getUrl());
//        assertThat(domain.getUserId()).isEqualTo(proto.getUserId());
//        assertThat(domain.getJsonResult()).isEqualTo(ProtoJsonMapper.structToMap(proto.getJsonResult()));
//        assertThat(domain.getCreatedAt()).isCloseTo(
//                ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()),
//                within(1, ChronoUnit.MILLIS));
//
//        if ((timeBefore != null) && (timeAfter != null)) {
//            assertThat(domain.getCreatedAt())
//                    .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
//        }
//
//    }
//}