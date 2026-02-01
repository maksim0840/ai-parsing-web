package io.github.maksim0840.parsingparams.integration;

import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoJsonMapper;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import io.github.maksim0840.internalapi.user.v1.mapper.ProtoUserRoleMapper;
import io.github.maksim0840.parsing_param.v1.*;
import io.github.maksim0840.user.v1.*;
import io.github.maksim0840.usersinfo.Main;
import io.github.maksim0840.usersinfo.domain.ParsingParam;
import io.github.maksim0840.usersinfo.domain.User;
import io.github.maksim0840.usersinfo.repository.ParsingParamRepository;
import io.github.maksim0840.usersinfo.repository.UserRepository;
import io.github.maksim0840.usersinfo.utils.PasswordEncryption;
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
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты для проверки корректности работы grpc сервера users-info (ParsingParam domain).
 * Проверяется подключение, отправка запросов через blockingStub, получение запроса,
 * выполнение действий в базе данных и результат запроса
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
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("testdb").withUsername("test").withPassword("test");

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

    // Репозиторий для отправки запросов к базе данных
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

    /*
    Проверяет базовый сценарий create для ParsingParam: создание нескольких параметров для разных пользователей
    - создаёт в БД двух пользователей (контекст для FK userId)
    - отправляет два gRPC create-запроса с разными (userId, name, description)
    - проверяет поля ответа (userId/name/description, createdAt в окне времени) и уникальность id
    - проверяет, что записи реально сохранились в БД и совпадают с ответами gRPC
    */
    @Test
    void createSeveral() {
        // Создаём контекст
        User user1 = new User("user1", "passwordHash1", UserRole.USER);
        User user2 = new User("user2", "passwordHash2", UserRole.ADMIN);
        userRepository.save(user1);
        userRepository.save(user2);

        // Создаём запросы
        CreateParsingParamRequest request1 = CreateParsingParamRequest.newBuilder()
                .setUserId(user1.getId())
                .setName("name")
                .setDescription("description")
                .build();

        CreateParsingParamRequest request2 = CreateParsingParamRequest.newBuilder()
                .setUserId(user2.getId())
                .setName("Дата")
                .setDescription("DD:MM:YYYY")
                .build();

        // Отправляем запросы на сервер и получаем ответы
        Instant timeBefore = Instant.now();
        ParsingParamProto responseProto1 = blockingStub.create(request1).getParsingParam();
        ParsingParamProto responseProto2 = blockingStub.create(request2).getParsingParam();
        Instant timeAfter = Instant.now();

        // Проверяем валидность полей ответа от gRPC сервера
        assertParsingParamProtoFieldsValidity(request1.getUserId(), request1.getName(), request1.getDescription(), timeBefore, timeAfter, responseProto1);
        assertParsingParamProtoFieldsValidity(request2.getUserId(), request2.getName(), request2.getDescription(), timeBefore, timeAfter, responseProto2);
        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());

        // Получаем результаты записи в бд
        ParsingParam responseRepo1 = parsingParamRepository.findById(responseProto1.getId()).orElseThrow();
        ParsingParam responseRepo2 = parsingParamRepository.findById(responseProto2.getId()).orElseThrow();

        // Проверяем валидность записанной в базу данных информации
        assertParsingParamDomainFieldsValidity(request1.getUserId(), request1.getName(), request1.getDescription(), timeBefore, timeAfter, responseRepo1);
        assertParsingParamDomainFieldsValidity(request2.getUserId(), request2.getName(), request2.getDescription(), timeBefore, timeAfter, responseRepo2);
        assertThat(responseRepo1.getId()).isNotEqualTo(responseRepo2.getId());
        assertThat(parsingParamRepository.count()).isEqualTo(2);
    }

    /*
    Проверяет валидацию create: отсутствие обязательного userId
    - создаёт пользователя в БД (но НЕ передаёт его id в запросе)
    - отправляет gRPC create-запрос без userId
    - ожидает StatusRuntimeException со статусом NOT_FOUND
    - проверяет, что описание ошибки содержит смысл "user not found"
    */
    @Test
    void createWithEmptyUserId() {
        User user = new User("user", "passwordHash", UserRole.USER);
        userRepository.save(user);

        CreateParsingParamRequest request = CreateParsingParamRequest.newBuilder()
                .setName("Ссылка")
                .setDescription("формат https://www.[]")
                .build();

        // Ожидаем, что при запросе произошла ошибка
        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.create(request)
        );

        // Проверяем подробности ошибки
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        System.out.println(ex.getStatus().getDescription());
        assertThat(ex.getStatus().getDescription()).contains("user").contains("not found");
    }

    /*
    Проверяет поведение create при пустом name (необязательное поле/разрешён пустой ввод)
    - создаёт пользователя в БД
    - отправляет gRPC create-запрос без name (получится пустая строка по умолчанию)
    - ожидает успешное создание записи
    - проверяет поля ответа и соответствие сохранённой сущности в БД (name == "")
    */
    @Test
    void createWithEmptyName() {
        User user = new User("user", "passwordHash", UserRole.USER);
        userRepository.save(user);

        CreateParsingParamRequest request = CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId())
                .setDescription("описание")
                .build();

        Instant timeBefore = Instant.now();
        ParsingParamProto responseProto = blockingStub.create(request).getParsingParam();
        Instant timeAfter = Instant.now();

        assertParsingParamProtoFieldsValidity(request.getUserId(), "", request.getDescription(), timeBefore, timeAfter, responseProto);

        ParsingParam responseRepo = parsingParamRepository.findById(responseProto.getId()).orElseThrow();

        assertParsingParamDomainFieldsValidity(request.getUserId(), "", request.getDescription(), timeBefore, timeAfter, responseRepo);
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }

    /*
    Проверяет поведение create при пустом description (необязательное поле/разрешён пустой ввод)
    - создаёт пользователя в БД
    - отправляет gRPC create-запрос без description (получится пустая строка по умолчанию)
    - ожидает успешное создание записи
    - проверяет поля ответа и соответствие сохранённой сущности в БД (description == "")
    */
    @Test
    void createWithEmptyDescription() {
        User user = new User("user", "passwordHash", UserRole.USER);
        userRepository.save(user);

        CreateParsingParamRequest request = CreateParsingParamRequest.newBuilder()
                .setUserId(user.getId())
                .setName("название")
                .build();

        Instant timeBefore = Instant.now();
        ParsingParamProto responseProto = blockingStub.create(request).getParsingParam();
        Instant timeAfter = Instant.now();

        assertParsingParamProtoFieldsValidity(request.getUserId(), request.getName(), "", timeBefore, timeAfter, responseProto);

        ParsingParam responseRepo = parsingParamRepository.findById(responseProto.getId()).orElseThrow();

        assertParsingParamDomainFieldsValidity(request.getUserId(), request.getName(), "", timeBefore, timeAfter, responseRepo);
        assertThat(parsingParamRepository.count()).isEqualTo(1);
    }


    /*
    Проверяет базовый сценарий get для ParsingParam: получение нескольких существующих записей
    - создаёт двух пользователей и две сущности ParsingParam (каждая привязана к своему пользователю)
    - сохраняет сущности напрямую в БД
    - отправляет два gRPC get-запроса по корректным id
    - проверяет соответствие доменной сущности и ответа (id/userId/name/description/createdAt)
    - дополнительно проверяет, что id в ответах различаются
    */
    @Test
    void getSeveralExistingData() {
        User user1 = new User("user1", "passwordHash1", UserRole.USER);
        User user2 = new User("user2", "passwordHash2", UserRole.ADMIN);
        userRepository.save(user1);
        userRepository.save(user2);

        ParsingParam entity1 = new ParsingParam(user1, "Начало", "дата и время начала конференции");
        ParsingParam entity2 = new ParsingParam(user2, "Конце", "дата и время конца конференции");

        Instant timeBefore = Instant.now();
        parsingParamRepository.save(entity1);
        parsingParamRepository.save(entity2);
        Instant timeAfter = Instant.now();

        GetParsingParamRequest request1 = GetParsingParamRequest.newBuilder().setId(entity1.getId()).build();
        GetParsingParamRequest request2 = GetParsingParamRequest.newBuilder().setId(entity2.getId()).build();
        ParsingParamProto responseProto1 = blockingStub.get(request1).getParsingParam();
        ParsingParamProto responseProto2 = blockingStub.get(request2).getParsingParam();

        assertParsingParamDomainProtoValidity(entity1, responseProto1, timeBefore, timeAfter);
        assertParsingParamDomainProtoValidity(entity2, responseProto2, timeBefore, timeAfter);
        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());
    }

    /*
    Проверяет обработку ошибки get: запрос несуществующего id
    - создаёт пользователя и одну сущность ParsingParam в БД (чтобы база была не пустой)
    - отправляет gRPC get-запрос с id, которого нет в БД
    - ожидает StatusRuntimeException со статусом NOT_FOUND
    - проверяет, что описание ошибки содержит "not found" и сам запрошенный id
    */
    @Test
    void getWrongIdNotFoundException() {
        User user = new User("user1", "passwordHash1", UserRole.USER);
        userRepository.save(user);

        ParsingParam entity = new ParsingParam(user, "Подача тезисов", "дата и время вступительной части конференции");

        parsingParamRepository.save(entity);

        GetParsingParamRequest request = GetParsingParamRequest.newBuilder().setId(12).build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.get(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("not found").contains("12");
    }


    /*
    Проверяет getList с полным набором параметров фильтрации/пагинации/сортировки
    - формирует запрос с userId, createdFrom, createdTo, pageNum/pageSize и sortCreatedDesc=true
    - ожидает получить подмножество записей конкретного пользователя в заданном диапазоне дат
    - проверяет, что результаты совпадают по id и идут в ожидаемом порядке
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

        List<Long> expectedParamIds = List.of(9L, 8L);

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет getList без опциональных фильтров (только пагинация)
    - формирует запрос только с pageNum/pageSize
    - ожидает вернуть все записи из БД в дефолтной сортировке сервиса
    - проверяет, что пришли все элементы и порядок соответствует ожидаемому
    */
    @Test
    void getListNoOptionalParams() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedParamIds = List.of(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет getList: сортировка по createdAt по возрастанию
    - формирует запрос с sortCreatedDesc=false
    - ожидает вернуть все записи в порядке от самых ранних к самым поздним
    - проверяет, что порядок id совпадает с ожидаемым
    */
    @Test
    void getListAscSorting() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setPageNum(0)
                .setPageSize(100)
                .setSortCreatedDesc(false)
                .build();

        List<Long> expectedParamIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет getList: фильтрация только по userId
    - формирует запрос с userId и параметрами страницы
    - ожидает вернуть только записи, принадлежащие указанному пользователю
    - проверяет, что пришли именно нужные id и в правильном порядке
    */
    @Test
    void getListByUserId() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setUserId(3L)
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedParamIds = List.of(6L, 5L, 4L);

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет getList: фильтрация по нижней границе createdFrom
    - формирует запрос с createdFrom и параметрами страницы
    - ожидает вернуть записи, у которых createdAt >= createdFrom
    - проверяет, что в ответе нет более ранних записей и порядок корректный
    */
    @Test
    void getListByCreatedFrom() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-07T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedParamIds = List.of(10L, 9L, 8L, 7L);

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет getList: фильтрация по верхней границе createdTo
    - формирует запрос с createdTo и параметрами страницы
    - ожидает вернуть записи, у которых createdAt <= createdTo
    - проверяет, что в ответе нет более поздних записей и порядок корректный
    */
    @Test
    void getListByCreatedTo() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-02T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedParamIds = List.of(2L, 1L);

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет getList: фильтрация по диапазону дат createdFrom..createdTo
    - формирует запрос с createdFrom и createdTo (оба внутри диапазона данных в БД)
    - ожидает вернуть записи, попадающие в диапазон включительно
    - проверяет, что набор id и порядок совпадают с ожидаемыми
    */
    @Test
    void getListDatesBetween() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-02T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-06T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedParamIds = List.of(6L, 5L, 4L, 3L, 2L);

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет getList: фильтрация по диапазону дат, который шире данных в БД
    - формирует запрос createdFrom/createdTo так, чтобы диапазон полностью покрывал все записи
    - ожидает, что фильтрация пройдёт успешно и вернёт все элементы (ничего не отфильтруется)
    - проверяет, что возвращены все id в ожидаемом порядке
    */
    @Test
    void getListNotFromDbDatesBetween() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2025-01-01T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2027-01-01T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedParamIds = List.of(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет getList: конфликтный диапазон дат (createdFrom > createdTo)
    - формирует запрос с createdFrom позже createdTo
    - ожидает корректное поведение сервиса без падения: пустой результат
    - проверяет, что список результатов пуст
    */
    @Test
    void getListConflictDatesNoData() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-07T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-05T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedParamIds = List.of();

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет getList: корректная работа пагинации на "серединной" странице
    - формирует запрос с pageNum и pageSize так, чтобы выбиралась непервая страница
    - ожидает вернуть ровно pageSize элементов из середины общего списка
    - проверяет, что пришли ожидаемые id в правильном порядке
    */
    @Test
    void getListMidPage() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setPageNum(2)
                .setPageSize(3)
                .build();

        List<Long> expectedParamIds = List.of(4L, 3L, 2L);

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет getList: запрос страницы, выходящей за пределы данных
    - формирует запрос с pageNum, для которого данных уже нет
    - ожидает пустой результат без ошибок
    - проверяет, что вернулся пустой список
    */
    @Test
    void getListExceedingPageNum() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setPageNum(5)
                .setPageSize(2)
                .build();

        List<Long> expectedParamIds = List.of();

        checkGetListRequest(request, expectedParamIds);
    }

    /*
    Проверяет валидацию параметров getList: pageSize = 0
    - отправляет запрос с pageSize=0
    - ожидает StatusRuntimeException со статусом UNAVAILABLE
    - проверяет, что описание ошибки содержит смысл "size less than one"
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
    - отправляет запрос с pageNum=-1
    - ожидает StatusRuntimeException со статусом UNAVAILABLE
    - проверяет, что описание ошибки содержит упоминание некорректного индекса (less than zero)
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
    Проверяет getList: фильтрация по userId, которого нет в БД
    - отправляет запрос с userId, отсутствующим среди записей ParsingParam
    - ожидает корректное поведение без ошибок: пустой результат
    - проверяет, что список результатов пуст
    */
    @Test
    void getListNotFromDbUserId() {
        GetListParsingParamRequest request = GetListParsingParamRequest.newBuilder()
                .setUserId(-1L)
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedParamIds = List.of();

        checkGetListRequest(request, expectedParamIds);
    }


    /*
    Проверяет базовый сценарий delete для ParsingParam: удаление нескольких существующих записей
    - создаёт двух пользователей и две сущности ParsingParam
    - отправляет gRPC delete-запросы по существующим id
    - проверяет, что после каждого удаления количество записей в parsingParamRepository уменьшается
    - дополнительно проверяет, что удаление параметров не удаляет пользователей (нет каскада в эту сторону)
    */
    @Test
    void deleteSeveralExistingData() {
        User user1 = new User("user1", "passwordHash1", UserRole.USER);
        User user2 = new User("user2", "passwordHash2", UserRole.ADMIN);
        userRepository.save(user1);
        userRepository.save(user2);

        ParsingParam entity1 = new ParsingParam(user1, "country", "country");
        ParsingParam entity2 = new ParsingParam(user2, "city", "city");

        parsingParamRepository.save(entity1);
        parsingParamRepository.save(entity2);

        DeleteParsingParamRequest request1 = DeleteParsingParamRequest.newBuilder().setId(entity1.getId()).build();
        DeleteParsingParamRequest request2 = DeleteParsingParamRequest.newBuilder().setId(entity2.getId()).build();

        assertThat(parsingParamRepository.count()).isEqualTo(2);
        blockingStub.delete(request1);
        assertThat(parsingParamRepository.count()).isEqualTo(1);
        blockingStub.delete(request2);
        assertThat(parsingParamRepository.count()).isEqualTo(0);

        // Пользователи не удалились вместе с параметрами
        assertThat(userRepository.count()).isEqualTo(2);
    }

    /*
    Проверяет обработку ошибки delete: удаление несуществующего id
    - создаёт пользователя и одну сущность ParsingParam в БД
    - отправляет gRPC delete-запрос с id, которого нет в БД
    - ожидает StatusRuntimeException со статусом NOT_FOUND
    - проверяет, что описание ошибки содержит "didn't exist" и сам id
    */
    @Test
    void deleteWrongIdNotFoundException() {
        User user = new User("user", "passwordHash", UserRole.USER);
        userRepository.save(user);

        ParsingParam entity = new ParsingParam(user, "not a name 123 @", "not a description 321 %");

        parsingParamRepository.save(entity);

        DeleteParsingParamRequest request = DeleteParsingParamRequest.newBuilder().setId(-2).build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.delete(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("didn't exist").contains("-2");
    }


    // Вспомогательный метод для тестов getList
    void checkGetListRequest(GetListParsingParamRequest request, List<Long> expectedParamIds) {
        List<User> users = List.of(
                User.builder().id(1L).name("user1").passwordHash("passwordHash1").role(UserRole.VISITOR).createdAt(Instant.parse("2026-01-01T00:00:00.000Z")).build(),
                User.builder().id(2L).name("user2").passwordHash("passwordHash2").role(UserRole.USER).createdAt(Instant.parse("2026-01-02T00:00:00.000Z")).build(),
                User.builder().id(3L).name("user3").passwordHash("passwordHash3").role(UserRole.ADMIN).createdAt(Instant.parse("2026-01-03T00:00:00.000Z")).build(),
                User.builder().id(4L).name("user4").passwordHash("passwordHash4").role(UserRole.VISITOR).createdAt(Instant.parse("2026-01-04T00:00:00.000Z")).build()
        );
        rawDbInsertUser(users);
        assertThat(userRepository.count()).isEqualTo(users.size());

        List<ParsingParam> entities = List.of(
                ParsingParam.builder().id(1L).user(users.get(0)).name("name1").description("description1").createdAt(Instant.parse("2026-01-01T00:00:00.000Z")).build(),
                ParsingParam.builder().id(2L).user(users.get(1)).name("name2").description("description2").createdAt(Instant.parse("2026-01-02T00:00:00.000Z")).build(),
                ParsingParam.builder().id(3L).user(users.get(1)).name("name3").description("description3").createdAt(Instant.parse("2026-01-03T00:00:00.000Z")).build(),
                ParsingParam.builder().id(4L).user(users.get(2)).name("name4").description("description4").createdAt(Instant.parse("2026-01-04T00:00:00.000Z")).build(),
                ParsingParam.builder().id(5L).user(users.get(2)).name("name5").description("description5").createdAt(Instant.parse("2026-01-05T00:00:00.000Z")).build(),
                ParsingParam.builder().id(6L).user(users.get(2)).name("name6").description("description6").createdAt(Instant.parse("2026-01-06T00:00:00.000Z")).build(),
                ParsingParam.builder().id(7L).user(users.get(3)).name("name7").description("description7").createdAt(Instant.parse("2026-01-07T00:00:00.000Z")).build(),
                ParsingParam.builder().id(8L).user(users.get(3)).name("name8").description("description8").createdAt(Instant.parse("2026-01-08T00:00:00.000Z")).build(),
                ParsingParam.builder().id(9L).user(users.get(3)).name("name9").description("description9").createdAt(Instant.parse("2026-01-09T00:00:00.000Z")).build(),
                ParsingParam.builder().id(10L).user(users.get(3)).name("name10").description("description10").createdAt(Instant.parse("2026-01-10T00:00:00.000Z")).build()
        );

        // Вставляем данные в базу данных
        rawDbInsertParsingParam(entities);
        assertThat(parsingParamRepository.count()).isEqualTo(entities.size());

        // Отправляем gRPC серверу запрос
        List<ParsingParamProto> actualParamsProto = blockingStub.getList(request).getParsingParamsList();
        assertThat(actualParamsProto.size()).isEqualTo(expectedParamIds.size());

        // Проверяем порядок и соответствие полей
        for (int i = 0; i < expectedParamIds.size(); i++) {
            ParsingParam parsingParamDomain = parsingParamRepository.findById(expectedParamIds.get(i)).orElseThrow();
            ParsingParamProto parsingParamProto = actualParamsProto.get(i);
            assertParsingParamDomainProtoValidity(parsingParamDomain, parsingParamProto, null, null);
        }
    }

    // Низкоуровневая вставка тестовых данных напрямую в PostgreSQL таблицу users
    void rawDbInsertUser(List<User> entities) {
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

    // Низкоуровневая вставка тестовых данных напрямую в PostgreSQL таблицу parsing_params
    void rawDbInsertParsingParam(List<ParsingParam> entities) {
        String sql = """
            INSERT INTO parsing_params (id, user_id, name, description, created_at)
            VALUES (:id, :user_id, :name, :description, :created_at)
            """;

        for (ParsingParam e : entities) {
            jdbcTemplate.update(sql, new MapSqlParameterSource()
                    .addValue("id", e.getId())
                    .addValue("user_id", e.getUser().getId())
                    .addValue("name", e.getName())
                    .addValue("description", e.getDescription())
                    .addValue("created_at", Timestamp.from(e.getCreatedAt()))
            );
        }
    }

    // Проверяет валидность полей ParsingParamProto (ответ gRPC, обычно после create)
    private void assertParsingParamProtoFieldsValidity(
            Long expectedUserId,
            String expectedName,
            String expectedDescription,
            Instant timeBefore,
            Instant timeAfter,
            ParsingParamProto actualProto
    ) {
        assertThat(actualProto.getUserId()).isEqualTo(expectedUserId);
        assertThat(actualProto.getName()).isEqualTo(expectedName);
        assertThat(actualProto.getDescription()).isEqualTo(expectedDescription);

        assertThat(ProtoTimeMapper.timestampToInstant(actualProto.getCreatedAt()))
                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
    }

    // Проверяет валидность полей доменной сущности ParsingParam, прочитанной из БД
    private void assertParsingParamDomainFieldsValidity(
            Long expectedUserId,
            String expectedName,
            String expectedDescription,
            Instant timeBefore,
            Instant timeAfter,
            ParsingParam actualDomain
    ) {
        assertThat(actualDomain.getUser().getId()).isEqualTo(expectedUserId);
        assertThat(actualDomain.getName()).isEqualTo(expectedName);
        assertThat(actualDomain.getDescription()).isEqualTo(expectedDescription);

        assertThat(actualDomain.getCreatedAt())
                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
    }

    // Проверяет соответствие одной и той же сущности в двух представлениях (domain vs proto)
    private void assertParsingParamDomainProtoValidity(
            ParsingParam domain,
            ParsingParamProto proto,
            @Nullable Instant timeBefore,
            @Nullable Instant timeAfter
    ) {
        assertThat(domain.getId()).isEqualTo(proto.getId());
        assertThat(domain.getUser().getId()).isEqualTo(proto.getUserId());
        assertThat(domain.getName()).isEqualTo(proto.getName());
        assertThat(domain.getDescription()).isEqualTo(proto.getDescription());
        assertThat(domain.getCreatedAt()).isCloseTo(
                ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()),
                within(1, ChronoUnit.MILLIS));

        if ((timeBefore != null) && (timeAfter != null)) {
            assertThat(domain.getCreatedAt())
                    .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
        }
    }
}