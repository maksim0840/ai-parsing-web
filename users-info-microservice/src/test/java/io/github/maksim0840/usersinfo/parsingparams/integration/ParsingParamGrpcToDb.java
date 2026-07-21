package io.github.maksim0840.usersinfo.parsingparams.integration;

import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import io.github.maksim0840.parsing_param.v1.*;
import io.github.maksim0840.usersinfo.Main;
import io.github.maksim0840.usersinfo.entity.ParsingParam;
import io.github.maksim0840.usersinfo.entity.User;
import io.github.maksim0840.usersinfo.repository.ParsingParamRepository;
import io.github.maksim0840.usersinfo.repository.UserRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты для проверки корректности работы grpc сервера users-info (ParsingParam domain).
 * Проверяется подключение, отправка запросов через blockingStub, получение запроса,
 * выполнение действий в базе данных и результат запроса.
 * <p>
 * Актуализировано под текущий контракт: поле description заменено на три jsonb-набора
 * параметров (htmlParserParams / htmlPreprocessingParams / llmParams), запросы get и delete
 * дополнительно принимают userId, пара (userId, name) уникальна, добавлены методы
 * edit, getNamesByUserId, getByUserIdAndName, renameByUserIdAndName, deleteByUserIdAndName.
 */
@Testcontainers // включаем работу test-контейнеров (docker)
@SpringBootTest(
        classes = Main.class,
        properties = {
                "grpc.server.inProcessName=test",      // включаем in-process server (клиент и сервер общаются внутри одного JVM-процесса)
                "grpc.server.port=-1",                 // выключаем внешний server (не отдаем порт наружу)
                "grpc.client.inProcess.address=in-process:test" // подключаем клиента к in-process серверу
        })
public class ParsingParamGrpcToDb {

    // Testcontainers класс для запуска Docker-контейнера с базой данных внутри
    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    // Подмена динамических spring свойств для подключения к тестовой базе данных
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    // Объект для отправки gRPC запросов серверу
    @GrpcClient("inProcess")
    private ParsingParamServiceGrpc.ParsingParamServiceBlockingStub blockingStub;

    // Репозитории для отправки запросов к базе данных
    @Autowired
    ParsingParamRepository parsingParamRepository;
    @Autowired
    UserRepository userRepository;

    // Объект для более низкоуровневых операций с базой данных (по сравнению с репозиторием)
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    // Очищаем базу перед каждым новым тестом
    @BeforeEach
    void cleanDb() {
        parsingParamRepository.deleteAll();
        userRepository.deleteAll();
    }


    // ==================================================================
    // ============================= CREATE =============================
    // ==================================================================

    /*
    Проверяет базовый сценарий create: создание нескольких параметров для разных пользователей
    - создаёт в БД двух пользователей (контекст для FK userId)
    - отправляет два gRPC create-запроса с разными (userId, name)
    - проверяет поля ответа (userId/name, createdAt в окне времени) и уникальность id
    - проверяет, что записи реально сохранились в БД и совпадают с ответами gRPC
    */
    @Test
    void createSeveral() {
        User user1 = saveUser("user1", UserRole.ROLE_USER);
        User user2 = saveUser("user2", UserRole.ROLE_ADMIN);

        CreateParsingParamRequest request1 = CreateParsingParamRequest.newBuilder()
                .setUserId(user1.getId())
                .setName("name")
                .build();

        CreateParsingParamRequest request2 = CreateParsingParamRequest.newBuilder()
                .setUserId(user2.getId())
                .setName("Дата")
                .build();

        Instant timeBefore = Instant.now();
        ParsingParamProto responseProto1 = blockingStub.create(request1).getParsingParam();
        ParsingParamProto responseProto2 = blockingStub.create(request2).getParsingParam();
        Instant timeAfter = Instant.now();

        assertProtoFieldsValidity(request1.getUserId(), request1.getName(), timeBefore, timeAfter, responseProto1);
        assertProtoFieldsValidity(request2.getUserId(), request2.getName(), timeBefore, timeAfter, responseProto2);
        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());

        ParsingParam responseRepo1 = parsingParamRepository.findById(responseProto1.getId()).orElseThrow();
        ParsingParam responseRepo2 = parsingParamRepository.findById(responseProto2.getId()).orElseThrow();

        assertDomainFieldsValidity(request1.getUserId(), request1.getName(), timeBefore, timeAfter, responseRepo1);
        assertDomainFieldsValidity(request2.getUserId(), request2.getName(), timeBefore, timeAfter, responseRepo2);
        assertThat(responseRepo1.getId()).isNotEqualTo(responseRepo2.getId());
        assertThat(parsingParamRepository.count()).isEqualTo(2);
    }

    /*
    Проверяет полный round-trip всех трёх наборов параметров:
    - создаёт запись со всеми заполненными полями htmlParserParams / htmlPreprocessingParams / llmParams
    - проверяет, что ответ сервера содержит ровно те же значения
    - перечитывает запись через get и убеждается, что jsonb-колонки сохранили данные без потерь
    */
    @Test
    void createRoundTripsAllParams() {
        User user = saveUser("params.owner", UserRole.ROLE_USER);

        HtmlParserParamsProto parserParams = HtmlParserParamsProto.newBuilder()
                .setDownloadImages(true)
                .putHeaders("Authorization", "Bearer token")
                .putCookies("session", "abc123")
                .putProxy("http", "127.0.0.1:8080")
                .setPageComplexity("high")
                .setAdditionalPageLoadTimeoutS(15)
                .build();

        HtmlPreprocessingParamsProto preprocessingParams = HtmlPreprocessingParamsProto.newBuilder()
                .setNoscriptProcessing(true)
                .setScriptProcessing(false)
                .setImgProcessing(true)
                .setSourceProcessing(false)
                .build();

        LLMParamsProto llmParams = LLMParamsProto.newBuilder()
                .setModelName("yandexgpt")
                .setSystemMessage("Ты — парсер")
                .setUserMessage("Извлеки данные")
                .setTemperature(0.35)
                .setMaxOutputTokens(2500)
                .build();

        CreateParsingParamRequest request = CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId())
                .setName("full.params")
                .setHtmlParserParams(parserParams)
                .setHtmlPreprocessingParams(preprocessingParams)
                .setLlmParams(llmParams)
                .build();

        ParsingParamProto responseProto = blockingStub.create(request).getParsingParam();

        assertThat(responseProto.getHtmlParserParams()).isEqualTo(parserParams);
        assertThat(responseProto.getHtmlPreprocessingParams()).isEqualTo(preprocessingParams);
        assertThat(responseProto.getLlmParams()).isEqualTo(llmParams);

        // перечитываем из базы через gRPC — данные должны пережить сериализацию в jsonb
        ParsingParamProto reread = blockingStub.get(GetParsingParamRequest.newBuilder()
                .setId(responseProto.getId())
                .setUserId(user.getId())
                .build()).getParsingParam();

        assertThat(reread.getHtmlParserParams()).isEqualTo(parserParams);
        assertThat(reread.getHtmlPreprocessingParams()).isEqualTo(preprocessingParams);
        assertThat(reread.getLlmParams()).isEqualTo(llmParams);
    }

    /*
    Проверяет create без указания наборов параметров:
    - в protobuf незаданные вложенные сообщения приходят как экземпляры по умолчанию
    - запись создаётся, все параметры пустые, но не null
    */
    @Test
    void createWithoutParams() {
        User user = saveUser("no.params", UserRole.ROLE_USER);

        CreateParsingParamRequest request = CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId())
                .setName("empty.params")
                .build();

        ParsingParamProto responseProto = blockingStub.create(request).getParsingParam();

        assertThat(responseProto.getHtmlParserParams().hasDownloadImages()).isFalse();
        assertThat(responseProto.getHtmlParserParams().getHeadersMap()).isEmpty();
        assertThat(responseProto.getHtmlPreprocessingParams().hasNoscriptProcessing()).isFalse();
        assertThat(responseProto.getLlmParams().getModelName()).isEmpty();

        ParsingParam stored = parsingParamRepository.findById(responseProto.getId()).orElseThrow();
        assertThat(stored.getHtmlParserParams()).isNotNull();
        assertThat(stored.getHtmlPreprocessingParams()).isNotNull();
        assertThat(stored.getLlmParams()).isNotNull();
    }

    /*
    Проверяет поведение create при отсутствии userId в запросе:
    - в protobuf незаданный int64 становится нулём, пользователя с id=0 не существует
    - сервис бросает NotFoundException, но эндпоинт create не обрабатывает её отдельно
    ВНИМАНИЕ: ожидаемым по смыслу является NOT_FOUND — тест фиксирует текущее поведение
    (UNAVAILABLE) и должен быть обновлён после добавления catch (NotFoundException) в create.
    */
    @Test
    void createWithEmptyUserIdException() {
        saveUser("existing.user", UserRole.ROLE_USER);

        CreateParsingParamRequest request = CreateParsingParamRequest.newBuilder()
                .setName("Ссылка")
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.create(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(ex.getStatus().getDescription()).contains("user").contains("not found");
        assertThat(parsingParamRepository.count()).isZero();
    }

    /*
    Проверяет поведение create при пустом name:
    - отправляет запрос без name (в protobuf это пустая строка)
    - запись успешно создаётся с name == ""
    */
    @Test
    void createWithEmptyName() {
        User user = saveUser("user", UserRole.ROLE_USER);

        CreateParsingParamRequest request = CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId())
                .build();

        Instant timeBefore = Instant.now();
        ParsingParamProto responseProto = blockingStub.create(request).getParsingParam();
        Instant timeAfter = Instant.now();

        assertProtoFieldsValidity(request.getUserId(), "", timeBefore, timeAfter, responseProto);

        ParsingParam responseRepo = parsingParamRepository.findById(responseProto.getId()).orElseThrow();

        assertDomainFieldsValidity(request.getUserId(), "", timeBefore, timeAfter, responseRepo);
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }

    /*
    Проверяет уникальность пары (userId, name):
    - повторное создание параметра с тем же именем у того же пользователя отклоняется
    - сервис проверяет дубликат явно и бросает IllegalArgumentException -> INVALID_ARGUMENT
    */
    @Test
    void createDuplicateNameInvalidArgumentException() {
        User user = saveUser("dup.owner", UserRole.ROLE_USER);

        CreateParsingParamRequest request = CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId())
                .setName("duplicate")
                .build();

        blockingStub.create(request);

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.create(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(ex.getStatus().getDescription()).contains("already exists");
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }

    /*
    Проверяет, что уникальность имени действует в пределах одного пользователя:
    - два разных пользователя могут иметь параметры с одинаковым именем
    */
    @Test
    void createSameNameForDifferentUsers() {
        User user1 = saveUser("same.name.one", UserRole.ROLE_USER);
        User user2 = saveUser("same.name.two", UserRole.ROLE_USER);

        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user1.getId()).setName("shared").build());
        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user2.getId()).setName("shared").build());

        assertThat(parsingParamRepository.count()).isEqualTo(2);
    }


    // ==================================================================
    // ============================== EDIT ==============================
    // ==================================================================

    /*
    Проверяет happy-path для edit:
    - меняет name и все три набора параметров существующей записи
    - ответ содержит новые значения
    - изменения действительно попадают в базу
    - id и createdAt остаются прежними
    */
    @Test
    void editExistingData() {
        User user = saveUser("edit.owner", UserRole.ROLE_USER);

        ParsingParamProto created = blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId())
                .setName("before")
                .setLlmParams(LLMParamsProto.newBuilder().setModelName("old-model").build())
                .build()).getParsingParam();

        LLMParamsProto newLlmParams = LLMParamsProto.newBuilder()
                .setModelName("new-model")
                .setSystemMessage("новый системный промпт")
                .setTemperature(0.9)
                .build();

        EditParsingParamRequest request = EditParsingParamRequest.newBuilder()
                .setId(created.getId())
                .setUserId(user.getId())
                .setName("after")
                .setLlmParams(newLlmParams)
                .build();

        ParsingParamProto responseProto = blockingStub.edit(request).getParsingParam();

        assertThat(responseProto.getId()).isEqualTo(created.getId());
        assertThat(responseProto.getName()).isEqualTo("after");
        assertThat(responseProto.getLlmParams()).isEqualTo(newLlmParams);
        assertThat(ProtoTimeMapper.timestampToInstant(responseProto.getCreatedAt()))
                .isCloseTo(ProtoTimeMapper.timestampToInstant(created.getCreatedAt()), within(1, ChronoUnit.MILLIS));

        ParsingParam stored = parsingParamRepository.findById(created.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("after");
        assertThat(stored.getLlmParams().getModelName()).isEqualTo("new-model");
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }

    /*
    Проверяет обработку edit для несуществующего id:
    - сервис бросает NotFoundException, но эндпоинт edit не обрабатывает её отдельно
    ВНИМАНИЕ: ожидаемым по смыслу является NOT_FOUND — тест фиксирует текущее поведение
    (UNAVAILABLE) и должен быть обновлён после добавления catch (NotFoundException) в edit.
    */
    @Test
    void editWrongIdException() {
        User user = saveUser("edit.missing", UserRole.ROLE_USER);

        EditParsingParamRequest request = EditParsingParamRequest.newBuilder()
                .setId(-5)
                .setUserId(user.getId())
                .setName("whatever")
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.edit(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(ex.getStatus().getDescription()).contains("not found").contains("-5");
    }

    /*
    Проверяет разграничение доступа в edit.
    ВНИМАНИЕ: это тест на ДЫРУ В БЕЗОПАСНОСТИ, а не на корректное поведение.
    editParsingParam загружает запись только по id и безусловно переписывает владельца
    на userId из запроса, поэтому чужой пользователь может присвоить себе чужой параметр.
    Тест фиксирует текущее поведение; после добавления проверки владельца
    ожидание нужно поменять на NOT_FOUND и убрать перенос записи.
    */
    @Test
    void editAllowsForeignUserToStealRecord() {
        User owner = saveUser("edit.real.owner", UserRole.ROLE_USER);
        User intruder = saveUser("edit.intruder", UserRole.ROLE_USER);

        ParsingParamProto created = blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(owner.getId())
                .setName("victim")
                .build()).getParsingParam();

        EditParsingParamRequest request = EditParsingParamRequest.newBuilder()
                .setId(created.getId())
                .setUserId(intruder.getId())
                .setName("stolen")
                .build();

        ParsingParamProto responseProto = blockingStub.edit(request).getParsingParam();

        // запись перешла к чужому пользователю — так быть не должно
        assertThat(responseProto.getUserId()).isEqualTo(intruder.getId());
        ParsingParam stored = parsingParamRepository.findById(created.getId()).orElseThrow();
        assertThat(stored.getUser().getId()).isEqualTo(intruder.getId());
    }

    /*
    Проверяет edit при конфликте уникальности (userId, name):
    - у пользователя есть два параметра, второй переименовывается в имя первого
    - нарушение uq_parsing_params_user_name даёт ошибку записи
    ВНИМАНИЕ: сервис не проверяет дубликат в edit явно (в отличие от create и rename),
    поэтому клиент получает UNAVAILABLE вместо INVALID_ARGUMENT.
    */
    @Test
    void editDuplicateNameException() {
        User user = saveUser("edit.dup", UserRole.ROLE_USER);

        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId()).setName("first").build());
        ParsingParamProto second = blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId()).setName("second").build()).getParsingParam();

        EditParsingParamRequest request = EditParsingParamRequest.newBuilder()
                .setId(second.getId())
                .setUserId(user.getId())
                .setName("first")
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.edit(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }


    // ==================================================================
    // ============================== GET ===============================
    // ==================================================================

    /*
    Проверяет базовый сценарий get: получение нескольких существующих записей
    - создаёт двух пользователей и две записи ParsingParam
    - отправляет два gRPC get-запроса по корректным парам (id, userId)
    - проверяет соответствие доменной сущности и ответа
    */
    @Test
    void getSeveralExistingData() {
        User user1 = saveUser("user1", UserRole.ROLE_USER);
        User user2 = saveUser("user2", UserRole.ROLE_ADMIN);

        ParsingParam entity1 = new ParsingParam(user1, "Начало", emptyParserParams(), emptyPreprocessingParams(), emptyLlmParams());
        ParsingParam entity2 = new ParsingParam(user2, "Конец", emptyParserParams(), emptyPreprocessingParams(), emptyLlmParams());

        Instant timeBefore = Instant.now();
        parsingParamRepository.save(entity1);
        parsingParamRepository.save(entity2);
        Instant timeAfter = Instant.now();

        ParsingParamProto responseProto1 = blockingStub.get(GetParsingParamRequest.newBuilder()
                .setId(entity1.getId()).setUserId(user1.getId()).build()).getParsingParam();
        ParsingParamProto responseProto2 = blockingStub.get(GetParsingParamRequest.newBuilder()
                .setId(entity2.getId()).setUserId(user2.getId()).build()).getParsingParam();

        assertDomainProtoValidity(entity1, responseProto1, timeBefore, timeAfter);
        assertDomainProtoValidity(entity2, responseProto2, timeBefore, timeAfter);
        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());
    }

    /*
    Проверяет обработку get для несуществующего id:
    - ожидает NOT_FOUND с упоминанием запрошенного id
    */
    @Test
    void getWrongIdNotFoundException() {
        User user = saveUser("user1", UserRole.ROLE_USER);
        parsingParamRepository.save(new ParsingParam(user, "Подача тезисов",
                emptyParserParams(), emptyPreprocessingParams(), emptyLlmParams()));

        GetParsingParamRequest request = GetParsingParamRequest.newBuilder()
                .setId(12).setUserId(user.getId()).build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.get(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("not found").contains("12");
    }

    /*
    Проверяет разграничение доступа в get:
    - запись принадлежит одному пользователю, запрос приходит от другого
    ВНИМАНИЕ: текущая реализация бросает голый RuntimeException, который эндпоинт
    превращает в UNAVAILABLE. Ожидаемым по смыслу является NOT_FOUND.
    */
    @Test
    void getForeignUserIdException() {
        User owner = saveUser("get.owner", UserRole.ROLE_USER);
        User intruder = saveUser("get.intruder", UserRole.ROLE_USER);

        ParsingParam entity = new ParsingParam(owner, "secret",
                emptyParserParams(), emptyPreprocessingParams(), emptyLlmParams());
        parsingParamRepository.save(entity);

        GetParsingParamRequest request = GetParsingParamRequest.newBuilder()
                .setId(entity.getId()).setUserId(intruder.getId()).build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.get(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(ex.getStatus().getDescription()).contains("userId does not match");
    }

    /*
    Проверяет поведение get для записи с NULL в jsonb-колонках:
    - такие строки могут появиться при миграции или ручной правке данных
    - маппер proto не проверяет null и падает с NullPointerException
    ВНИМАНИЕ: тест фиксирует хрупкость ParsingParamProtoMapper — при исправлении
    (null -> пустое сообщение) ожидание нужно поменять на успешный ответ.
    */
    @Test
    void getWithNullParamsException() {
        User user = saveUser("null.params", UserRole.ROLE_USER);
        rawInsertParsingParamWithNullParams(1L, user.getId(), "broken", Instant.parse("2026-01-01T00:00:00.000Z"));

        GetParsingParamRequest request = GetParsingParamRequest.newBuilder()
                .setId(1L).setUserId(user.getId()).build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.get(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }


    // ==================================================================
    // ============================ GET LIST ============================
    // ==================================================================

    /*
    Проверяет getList с полным набором параметров фильтрации/пагинации/сортировки
    - формирует запрос с userId, createdFrom, createdTo, pageNum/pageSize и sortCreatedDesc=true
    - ожидает подмножество записей конкретного пользователя в заданном диапазоне дат
    */
    @Test
    void getListAllParams() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setUserId(4L)
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-08T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-09T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .setSortCreatedDesc(true)
                .build();

        checkGetListRequest(request, List.of(9L, 8L));
    }

    /*
    Проверяет getList без опциональных фильтров:
    - передаёт только pageNum/pageSize
    - ожидает все записи в дефолтном порядке (убывание createdAt)
    */
    @Test
    void getListNoOptionalParams() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setPageNum(0)
                .setPageSize(100)
                .build();

        checkGetListRequest(request, allIdsDesc());
    }

    /*
    Проверяет сортировку getList по возрастанию createdAt:
    - передаёт sortCreatedDesc=false
    */
    @Test
    void getListAscSorting() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setPageNum(0)
                .setPageSize(100)
                .setSortCreatedDesc(false)
                .build();

        checkGetListRequest(request, allIdsDesc().reversed());
    }

    /*
    Проверяет фильтрацию getList по userId:
    - ожидает только записи указанного пользователя
    */
    @Test
    void getListByUserId() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setUserId(3L)
                .setPageNum(0)
                .setPageSize(100)
                .build();

        checkGetListRequest(request, List.of(6L, 5L, 4L));
    }

    /*
    Проверяет фильтрацию getList по нижней границе createdFrom
    */
    @Test
    void getListByCreatedFrom() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-07T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        checkGetListRequest(request, List.of(10L, 9L, 8L, 7L));
    }

    /*
    Проверяет фильтрацию getList по верхней границе createdTo
    */
    @Test
    void getListByCreatedTo() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-02T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        checkGetListRequest(request, List.of(2L, 1L));
    }

    /*
    Проверяет фильтрацию getList по диапазону createdAt
    */
    @Test
    void getListDatesBetween() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-02T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-06T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        checkGetListRequest(request, List.of(6L, 5L, 4L, 3L, 2L));
    }

    /*
    Проверяет фильтрацию getList по диапазону дат, который шире всех дат в базе:
    - ожидает все 10 записей без ошибок
    */
    @Test
    void getListNotFromDbDatesBetween() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2025-01-01T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2027-01-01T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        checkGetListRequest(request, allIdsDesc());
    }

    /*
    Проверяет поведение getList при конфликтном диапазоне дат:
    - createdFrom позже createdTo, ожидается пустой список без исключения
    */
    @Test
    void getListConflictDatesNoData() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-08T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-05T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        checkGetListRequest(request, List.of());
    }

    /*
    Проверяет пагинацию getList на "средней" странице
    */
    @Test
    void getListMidPage() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setPageNum(2)
                .setPageSize(3)
                .build();

        checkGetListRequest(request, List.of(4L, 3L, 2L));
    }

    /*
    Проверяет поведение getList при выходе за пределы страниц
    */
    @Test
    void getListExceedingPageNum() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setPageNum(5)
                .setPageSize(2)
                .build();

        checkGetListRequest(request, List.of());
    }

    /*
    Проверяет валидацию параметров getList: pageSize = 0
    ВНИМАНИЕ: по смыслу это INVALID_ARGUMENT — тест фиксирует текущее поведение.
    */
    @Test
    void getListZeroPageSizeUnavailableException() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setPageNum(0)
                .setPageSize(0)
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.getList(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(ex.getStatus().getDescription()).contains("size").contains("less than one");
    }

    /*
    Проверяет валидацию параметров getList: отрицательный pageNum
    ВНИМАНИЕ: по смыслу это INVALID_ARGUMENT — тест фиксирует текущее поведение.
    */
    @Test
    void getListNegativePageNumUnavailableException() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setPageNum(-1)
                .setPageSize(37)
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.getList(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(ex.getStatus().getDescription()).contains("index").contains("less than zero");
    }

    /*
    Проверяет фильтрацию getList по userId, которого нет в базе:
    - ожидает пустой результат без ошибок
    */
    @Test
    void getListNotFromDbUserId() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setUserId(-1L)
                .setPageNum(0)
                .setPageSize(100)
                .build();

        checkGetListRequest(request, List.of());
    }


    // ==================================================================
    // ======================= GET NAMES BY USER ID =====================
    // ==================================================================

    /*
    Проверяет getNamesByUserId:
    - возвращает только имена параметров указанного пользователя
    - параметры других пользователей в выборку не попадают
    */
    @Test
    void getNamesByUserId() {
        User user1 = saveUser("names.one", UserRole.ROLE_USER);
        User user2 = saveUser("names.two", UserRole.ROLE_USER);

        blockingStub.create(CreateParsingParamRequest.newBuilder().setUserId(user1.getId()).setName("alpha").build());
        blockingStub.create(CreateParsingParamRequest.newBuilder().setUserId(user1.getId()).setName("beta").build());
        blockingStub.create(CreateParsingParamRequest.newBuilder().setUserId(user2.getId()).setName("gamma").build());

        List<String> names = blockingStub.getNamesByUserId(GetNamesParsingParamRequest.newBuilder()
                .setUserId(user1.getId()).build()).getNamesList();

        assertThat(names).containsExactlyInAnyOrder("alpha", "beta");
    }

    /*
    Проверяет getNamesByUserId для пользователя без параметров:
    - ожидается пустой список без исключений
    */
    @Test
    void getNamesByUserIdEmptyResult() {
        User user = saveUser("names.empty", UserRole.ROLE_USER);

        List<String> names = blockingStub.getNamesByUserId(GetNamesParsingParamRequest.newBuilder()
                .setUserId(user.getId()).build()).getNamesList();

        assertThat(names).isEmpty();
    }

    /*
    Проверяет getNamesByUserId для несуществующего пользователя:
    - метод не проверяет существование пользователя, ожидается пустой список
    */
    @Test
    void getNamesByUserIdUnknownUser() {
        List<String> names = blockingStub.getNamesByUserId(GetNamesParsingParamRequest.newBuilder()
                .setUserId(-1L).build()).getNamesList();

        assertThat(names).isEmpty();
    }


    // ==================================================================
    // ===================== GET BY USER ID AND NAME ====================
    // ==================================================================

    /*
    Проверяет getByUserIdAndName:
    - находит запись по паре (userId, name)
    - возвращает корректные поля
    */
    @Test
    void getByUserIdAndNameExistingData() {
        User user = saveUser("byname.owner", UserRole.ROLE_USER);

        ParsingParamProto created = blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId())
                .setName("target")
                .setLlmParams(LLMParamsProto.newBuilder().setModelName("gigachat").build())
                .build()).getParsingParam();

        ParsingParamProto found = blockingStub.getByUserIdAndName(
                GetParsingParamByUserIdAndNameRequest.newBuilder()
                        .setUserId(user.getId())
                        .setName("target")
                        .build()).getParsingParam();

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("target");
        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.getLlmParams().getModelName()).isEqualTo("gigachat");
    }

    /*
    Проверяет изоляцию по пользователям в getByUserIdAndName:
    - параметр с таким именем существует, но принадлежит другому пользователю
    - ожидается NOT_FOUND
    */
    @Test
    void getByUserIdAndNameForeignUserNotFoundException() {
        User owner = saveUser("byname.owner2", UserRole.ROLE_USER);
        User intruder = saveUser("byname.intruder", UserRole.ROLE_USER);

        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(owner.getId()).setName("private").build());

        GetParsingParamByUserIdAndNameRequest request = GetParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(intruder.getId())
                .setName("private")
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.getByUserIdAndName(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("not found").contains("private");
    }

    /*
    Проверяет getByUserIdAndName для несуществующего имени:
    - ожидается NOT_FOUND с упоминанием имени
    */
    @Test
    void getByUserIdAndNameWrongNameNotFoundException() {
        User user = saveUser("byname.missing", UserRole.ROLE_USER);
        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId()).setName("exists").build());

        GetParsingParamByUserIdAndNameRequest request = GetParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(user.getId())
                .setName("no_such_name")
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.getByUserIdAndName(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("no_such_name");
    }


    // ==================================================================
    // ==================== RENAME BY USER ID AND NAME ==================
    // ==================================================================

    /*
    Проверяет happy-path для renameByUserIdAndName:
    - имя параметра меняется в базе
    - остальные поля (id, владелец, параметры) не затрагиваются
    */
    @Test
    void renameExistingData() {
        User user = saveUser("rename.owner", UserRole.ROLE_USER);

        ParsingParamProto created = blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId())
                .setName("old_name")
                .setLlmParams(LLMParamsProto.newBuilder().setModelName("model").build())
                .build()).getParsingParam();

        blockingStub.renameByUserIdAndName(RenameParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(user.getId())
                .setOldName("old_name")
                .setNewName("new_name")
                .build());

        ParsingParam stored = parsingParamRepository.findById(created.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("new_name");
        assertThat(stored.getUser().getId()).isEqualTo(user.getId());
        assertThat(stored.getLlmParams().getModelName()).isEqualTo("model");
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }

    /*
    Проверяет renameByUserIdAndName при конфликте имён:
    - новое имя уже занято у этого же пользователя
    - сервис проверяет дубликат явно -> INVALID_ARGUMENT
    - имена в базе остаются прежними
    */
    @Test
    void renameToExistingNameInvalidArgumentException() {
        User user = saveUser("rename.dup", UserRole.ROLE_USER);

        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId()).setName("first").build());
        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId()).setName("second").build());

        RenameParsingParamByUserIdAndNameRequest request = RenameParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(user.getId())
                .setOldName("second")
                .setNewName("first")
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.renameByUserIdAndName(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(ex.getStatus().getDescription()).contains("already exists");
        assertThat(parsingParamRepository.findByUserIdAndName(user.getId(), "second")).isPresent();
    }

    /*
    Проверяет переименование в имя, занятое ДРУГИМ пользователем:
    - ограничение уникальности действует в пределах пользователя, конфликта нет
    - переименование должно пройти успешно
    */
    @Test
    void renameToNameTakenByAnotherUser() {
        User user1 = saveUser("rename.u1", UserRole.ROLE_USER);
        User user2 = saveUser("rename.u2", UserRole.ROLE_USER);

        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user1.getId()).setName("shared").build());
        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user2.getId()).setName("own").build());

        blockingStub.renameByUserIdAndName(RenameParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(user2.getId())
                .setOldName("own")
                .setNewName("shared")
                .build());

        assertThat(parsingParamRepository.findByUserIdAndName(user2.getId(), "shared")).isPresent();
        assertThat(parsingParamRepository.findByUserIdAndName(user1.getId(), "shared")).isPresent();
    }

    /*
    Проверяет renameByUserIdAndName для несуществующего oldName.
    ВНИМАНИЕ: тест фиксирует ОШИБКУ. Сервис не проверяет существование записи,
    UPDATE обновляет 0 строк, и клиент получает успешный ответ, хотя ничего не изменилось.
    После добавления проверки ожидание нужно поменять на NOT_FOUND.
    */
    @Test
    void renameNonExistingNameSilentlySucceeds() {
        User user = saveUser("rename.missing", UserRole.ROLE_USER);
        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId()).setName("exists").build());

        blockingStub.renameByUserIdAndName(RenameParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(user.getId())
                .setOldName("no_such_name")
                .setNewName("brand_new")
                .build());

        // ничего не изменилось, но ошибки клиент не получил
        assertThat(parsingParamRepository.findByUserIdAndName(user.getId(), "exists")).isPresent();
        assertThat(parsingParamRepository.findByUserIdAndName(user.getId(), "brand_new")).isEmpty();
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }


    // ==================================================================
    // ============================= DELETE =============================
    // ==================================================================

    /*
    Проверяет базовый сценарий delete: удаление нескольких существующих записей
    - удаление параметров не удаляет пользователей (нет каскада в эту сторону)
    */
    @Test
    void deleteSeveralExistingData() {
        User user1 = saveUser("user1", UserRole.ROLE_USER);
        User user2 = saveUser("user2", UserRole.ROLE_ADMIN);

        ParsingParam entity1 = new ParsingParam(user1, "country", emptyParserParams(), emptyPreprocessingParams(), emptyLlmParams());
        ParsingParam entity2 = new ParsingParam(user2, "city", emptyParserParams(), emptyPreprocessingParams(), emptyLlmParams());
        parsingParamRepository.save(entity1);
        parsingParamRepository.save(entity2);

        DeleteParsingParamRequest request1 = DeleteParsingParamRequest.newBuilder()
                .setId(entity1.getId()).setUserId(user1.getId()).build();
        DeleteParsingParamRequest request2 = DeleteParsingParamRequest.newBuilder()
                .setId(entity2.getId()).setUserId(user2.getId()).build();

        assertThat(parsingParamRepository.count()).isEqualTo(2);
        blockingStub.delete(request1);
        assertThat(parsingParamRepository.count()).isEqualTo(1);
        blockingStub.delete(request2);
        assertThat(parsingParamRepository.count()).isEqualTo(0);

        // Пользователи не удалились вместе с параметрами
        assertThat(userRepository.count()).isEqualTo(2);
    }

    /*
    Проверяет обработку delete для несуществующего id:
    - ожидает NOT_FOUND с текстом "didn't exist" и самим id
    */
    @Test
    void deleteWrongIdNotFoundException() {
        User user = saveUser("user", UserRole.ROLE_USER);
        parsingParamRepository.save(new ParsingParam(user, "not a name 123 @",
                emptyParserParams(), emptyPreprocessingParams(), emptyLlmParams()));

        DeleteParsingParamRequest request = DeleteParsingParamRequest.newBuilder()
                .setId(-2).setUserId(user.getId()).build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.delete(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("didn't exist").contains("-2");
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }

    /*
    Проверяет разграничение доступа в delete:
    - чужой пользователь не должен удалить запись
    - проверка идёт через existsByIdAndUserId, поэтому ожидается NOT_FOUND
    - запись остаётся в базе
    */
    @Test
    void deleteForeignUserIdNotFoundException() {
        User owner = saveUser("del.owner", UserRole.ROLE_USER);
        User intruder = saveUser("del.intruder", UserRole.ROLE_USER);

        ParsingParam entity = new ParsingParam(owner, "protected",
                emptyParserParams(), emptyPreprocessingParams(), emptyLlmParams());
        parsingParamRepository.save(entity);

        DeleteParsingParamRequest request = DeleteParsingParamRequest.newBuilder()
                .setId(entity.getId()).setUserId(intruder.getId()).build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.delete(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }


    // ==================================================================
    // =================== DELETE BY USER ID AND NAME ===================
    // ==================================================================

    /*
    Проверяет happy-path для deleteByUserIdAndName:
    - удаляется только запись указанного пользователя с указанным именем
    - одноимённая запись другого пользователя остаётся нетронутой
    */
    @Test
    void deleteByUserIdAndNameExistingData() {
        User user1 = saveUser("delname.u1", UserRole.ROLE_USER);
        User user2 = saveUser("delname.u2", UserRole.ROLE_USER);

        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user1.getId()).setName("shared").build());
        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user2.getId()).setName("shared").build());

        blockingStub.deleteByUserIdAndName(DeleteParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(user1.getId())
                .setName("shared")
                .build());

        assertThat(parsingParamRepository.findByUserIdAndName(user1.getId(), "shared")).isEmpty();
        assertThat(parsingParamRepository.findByUserIdAndName(user2.getId(), "shared")).isPresent();
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }

    /*
    Проверяет deleteByUserIdAndName для несуществующего имени:
    - ожидается NOT_FOUND с текстом "didn't exist"
    */
    @Test
    void deleteByUserIdAndNameWrongNameNotFoundException() {
        User user = saveUser("delname.missing", UserRole.ROLE_USER);
        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId()).setName("exists").build());

        DeleteParsingParamByUserIdAndNameRequest request = DeleteParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(user.getId())
                .setName("no_such_name")
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.deleteByUserIdAndName(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("didn't exist");
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }

    /*
    Проверяет изоляцию по пользователям в deleteByUserIdAndName:
    - чужой пользователь не может удалить запись по имени
    - ожидается NOT_FOUND, запись остаётся в базе
    */
    @Test
    void deleteByUserIdAndNameForeignUserNotFoundException() {
        User owner = saveUser("delname.owner", UserRole.ROLE_USER);
        User intruder = saveUser("delname.intruder", UserRole.ROLE_USER);

        blockingStub.create(CreateParsingParamRequest.newBuilder()
                .setUserId(owner.getId()).setName("private").build());

        DeleteParsingParamByUserIdAndNameRequest request = DeleteParsingParamByUserIdAndNameRequest.newBuilder()
                .setUserId(intruder.getId())
                .setName("private")
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.deleteByUserIdAndName(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }


    // ==================================================================
    // =========================== HELPERS ==============================
    // ==================================================================

    // Все id тестового набора в порядке убывания createdAt
    private List<Long> allIdsDesc() {
        return List.of(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);
    }

    // Сохраняет пользователя с уникальным именем (name объявлен как unique)
    private User saveUser(String name, UserRole role) {
        User user = new User(name, "passwordHash_" + name, role);
        return userRepository.save(user);
    }

    // Пустые (но не null) наборы параметров — null в jsonb ломает ParsingParamProtoMapper
    private io.github.maksim0840.usersinfo.entity.model.HtmlParserParams emptyParserParams() {
        return new io.github.maksim0840.usersinfo.entity.model.HtmlParserParams();
    }

    private io.github.maksim0840.usersinfo.entity.model.HtmlPreprocessingParams emptyPreprocessingParams() {
        return new io.github.maksim0840.usersinfo.entity.model.HtmlPreprocessingParams();
    }

    private io.github.maksim0840.usersinfo.entity.model.LLMParams emptyLlmParams() {
        return new io.github.maksim0840.usersinfo.entity.model.LLMParams();
    }

    // Вспомогательный метод для тестов getList: наполняет базу и сверяет ответ сервера
    private void checkGetListRequest(GetListParsingParamRequest request, List<Long> expectedParamIds) {
        seedUsersAndParams();

        List<ParsingParamProto> actualParamsProto = blockingStub.getList(request).getParsingParamsList();
        assertThat(actualParamsProto.size()).isEqualTo(expectedParamIds.size());

        for (int i = 0; i < expectedParamIds.size(); i++) {
            ParsingParam domain = parsingParamRepository.findById(expectedParamIds.get(i)).orElseThrow();
            ParsingParamProto proto = actualParamsProto.get(i);
            assertDomainProtoValidity(domain, proto, null, null);
        }
    }

    // Наполняет базу фиксированным набором: 4 пользователя и 10 параметров
    private void seedUsersAndParams() {
        List<User> users = List.of(
                User.builder().id(1L).name("user1").passwordHash("passwordHash1").role(UserRole.ROLE_VISITOR).createdAt(Instant.parse("2026-01-01T00:00:00.000Z")).build(),
                User.builder().id(2L).name("user2").passwordHash("passwordHash2").role(UserRole.ROLE_USER).createdAt(Instant.parse("2026-01-02T00:00:00.000Z")).build(),
                User.builder().id(3L).name("user3").passwordHash("passwordHash3").role(UserRole.ROLE_ADMIN).createdAt(Instant.parse("2026-01-03T00:00:00.000Z")).build(),
                User.builder().id(4L).name("user4").passwordHash("passwordHash4").role(UserRole.ROLE_VISITOR).createdAt(Instant.parse("2026-01-04T00:00:00.000Z")).build()
        );
        rawDbInsertUser(users);
        assertThat(userRepository.count()).isEqualTo(users.size());

        // (id, userId, name, createdAt) — параметры заполняем пустыми jsonb-объектами
        rawDbInsertParsingParam(1L, 1L, "name1", Instant.parse("2026-01-01T00:00:00.000Z"));
        rawDbInsertParsingParam(2L, 2L, "name2", Instant.parse("2026-01-02T00:00:00.000Z"));
        rawDbInsertParsingParam(3L, 2L, "name3", Instant.parse("2026-01-03T00:00:00.000Z"));
        rawDbInsertParsingParam(4L, 3L, "name4", Instant.parse("2026-01-04T00:00:00.000Z"));
        rawDbInsertParsingParam(5L, 3L, "name5", Instant.parse("2026-01-05T00:00:00.000Z"));
        rawDbInsertParsingParam(6L, 3L, "name6", Instant.parse("2026-01-06T00:00:00.000Z"));
        rawDbInsertParsingParam(7L, 4L, "name7", Instant.parse("2026-01-07T00:00:00.000Z"));
        rawDbInsertParsingParam(8L, 4L, "name8", Instant.parse("2026-01-08T00:00:00.000Z"));
        rawDbInsertParsingParam(9L, 4L, "name9", Instant.parse("2026-01-09T00:00:00.000Z"));
        rawDbInsertParsingParam(10L, 4L, "name10", Instant.parse("2026-01-10T00:00:00.000Z"));

        assertThat(parsingParamRepository.count()).isEqualTo(10);
    }

    // Низкоуровневая вставка тестовых данных напрямую в PostgreSQL таблицу users
    private void rawDbInsertUser(List<User> entities) {
        String sql = """
                INSERT INTO users (id, name, password_hash, role, created_at)
                VALUES (:id, :name, :password_hash, :role, :created_at)
                """;

        for (User e : entities) {
            jdbcTemplate.update(sql, new MapSqlParameterSource()
                    .addValue("id", e.getId())
                    .addValue("name", e.getName())
                    .addValue("password_hash", e.getPasswordHash())
                    .addValue("role", e.getRole().name())
                    .addValue("created_at", Timestamp.from(e.getCreatedAt()))
            );
        }
    }

    /*
    Низкоуровневая вставка в parsing_params.
    Строковые значения для jsonb-колонок нужно приводить явно через CAST,
    иначе PostgreSQL отвергнет их как character varying.
    */
    private void rawDbInsertParsingParam(Long id, Long userId, String name, Instant createdAt) {
        String sql = """
                INSERT INTO parsing_params (id, user_id, name, created_at,
                                            html_parser_params, html_preprocessing_params, llm_params)
                VALUES (:id, :user_id, :name, :created_at,
                        CAST(:html_parser_params AS jsonb),
                        CAST(:html_preprocessing_params AS jsonb),
                        CAST(:llm_params AS jsonb))
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("user_id", userId)
                .addValue("name", name)
                .addValue("created_at", Timestamp.from(createdAt))
                .addValue("html_parser_params", "{}")
                .addValue("html_preprocessing_params", "{}")
                .addValue("llm_params", "{}")
        );
    }

    // Вставка записи, у которой jsonb-колонки равны NULL (имитация некорректных/старых данных)
    private void rawInsertParsingParamWithNullParams(Long id, Long userId, String name, Instant createdAt) {
        String sql = """
                INSERT INTO parsing_params (id, user_id, name, created_at,
                                            html_parser_params, html_preprocessing_params, llm_params)
                VALUES (:id, :user_id, :name, :created_at, NULL, NULL, NULL)
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("user_id", userId)
                .addValue("name", name)
                .addValue("created_at", Timestamp.from(createdAt))
        );
    }

    // Проверяет валидность полей ParsingParamProto (ответ gRPC, обычно после create)
    private void assertProtoFieldsValidity(
            Long expectedUserId,
            String expectedName,
            Instant timeBefore,
            Instant timeAfter,
            ParsingParamProto actualProto
    ) {
        assertThat(actualProto.getUserId()).isEqualTo(expectedUserId);
        assertThat(actualProto.getName()).isEqualTo(expectedName);

        assertThat(ProtoTimeMapper.timestampToInstant(actualProto.getCreatedAt()))
                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
    }

    // Проверяет валидность полей доменной сущности ParsingParam, прочитанной из БД
    private void assertDomainFieldsValidity(
            Long expectedUserId,
            String expectedName,
            Instant timeBefore,
            Instant timeAfter,
            ParsingParam actualDomain
    ) {
        assertThat(actualDomain.getUser().getId()).isEqualTo(expectedUserId);
        assertThat(actualDomain.getName()).isEqualTo(expectedName);

        assertThat(actualDomain.getCreatedAt())
                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
    }

    // Проверяет соответствие одной и той же сущности в двух представлениях (domain vs proto)
    private void assertDomainProtoValidity(
            ParsingParam domain,
            ParsingParamProto proto,
            Instant timeBefore,
            Instant timeAfter
    ) {
        assertThat(domain.getId()).isEqualTo(proto.getId());
        assertThat(domain.getUser().getId()).isEqualTo(proto.getUserId());
        assertThat(domain.getName()).isEqualTo(proto.getName());
        assertThat(domain.getCreatedAt()).isCloseTo(
                ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()),
                within(1, ChronoUnit.MILLIS));

        if ((timeBefore != null) && (timeAfter != null)) {
            assertThat(domain.getCreatedAt())
                    .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
        }
    }
}
