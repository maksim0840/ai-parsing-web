package io.github.maksim0840.usersinfo.users.integration;

import io.github.maksim0840.extraction_result.v1.*;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import io.github.maksim0840.internalapi.user.v1.mapper.ProtoUserRoleMapper;
import io.github.maksim0840.parsing_param.v1.GetListParsingParamRequest;
import io.github.maksim0840.parsing_param.v1.ParsingParamServiceGrpc;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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
import org.testcontainers.utility.DockerImageName;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты для проверки корректности работы grpc сервера users-info (User domain).
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
public class UserGrpcToDb {

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
    private UserServiceGrpc.UserServiceBlockingStub blockingStub;

    // Репозиторий для отправки запросов к базе данных
    @Autowired
    UserRepository userRepository;
    @Autowired
    ParsingParamRepository parsingParamRepository;

    // Объект для более низкоуровневых операций с базой данных (по сравнению с репозиторием)
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    // Очищаем базу перед каждым новым тестом
    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }


    /*
    Проверяет happy-path для create:
    - два gRPC create-запроса корректно возвращают заполненных пользователей (id, name, passwordHash, role, createdAt)
    - создаются две независимые записи (id разные)
    - обе записи действительно сохраняются в PostgreSQL и данные совпадают с запросом (роль/имя/пароль, createdAt)
    */
    @Test
    void createSeveral() {
        // Создаём запросы
        UserRole role1 = UserRole.USER;
        CreateUserRequest request1 = CreateUserRequest.newBuilder()
                .setName("user123")
                .setPassword("aHjksd82lsKK")
                .setRole(ProtoUserRoleMapper.domainToProto(role1))
                .build();

        UserRole role2 = UserRole.VISITOR;
        CreateUserRequest request2 = CreateUserRequest.newBuilder()
                .setName("pdurov")
                .setPassword("qwerty12345")
                .setRole(ProtoUserRoleMapper.domainToProto(role2))
                .build();

        // Отправляем запросы на сервер и получаем ответы
        Instant timeBefore = Instant.now();
        UserProto responseProto1 = blockingStub.create(request1).getUser();
        UserProto responseProto2 = blockingStub.create(request2).getUser();
        Instant timeAfter = Instant.now();

        // Проверяем валидность полей ответа от gRPC сервера
        assertUserProtoFieldsValidity(request1.getName(), request1.getPassword(), role1, timeBefore, timeAfter, responseProto1);
        assertUserProtoFieldsValidity(request2.getName(), request2.getPassword(), role2, timeBefore, timeAfter, responseProto2);
        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());

        // Получаем результаты записи в бд
        User responseRepo1 = userRepository.findById(responseProto1.getId()).orElseThrow();
        User responseRepo2 = userRepository.findById(responseProto2.getId()).orElseThrow();

        // Проверяем валидность записанной в базу данных информации
        assertUserDomainFieldsValidity(request1.getName(), request1.getPassword(), role1, timeBefore, timeAfter, responseRepo1);
        assertUserDomainFieldsValidity(request2.getName(), request2.getPassword(), role2, timeBefore, timeAfter, responseRepo2);
        assertThat(responseRepo1.getId()).isNotEqualTo(responseRepo2.getId());
        assertThat(userRepository.count()).isEqualTo(2);
    }

    /*
    Проверяет поведение create при отсутствии name в запросе:
    - сервер принимает запрос без name (в protobuf это становится пустой строкой)
    - в ответе и в базе name сохраняется как ""
    - запись успешно создаётся, остальные поля (passwordHash/role/createdAt) валидны
    */
    @Test
    void createWithEmptyName() {
        UserRole role = UserRole.USER;
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setPassword("zX9!mN4#pQ1")
                .setRole(ProtoUserRoleMapper.domainToProto(role))
                .build();

        Instant timeBefore = Instant.now();
        UserProto responseProto = blockingStub.create(request).getUser();
        Instant timeAfter = Instant.now();

        assertUserProtoFieldsValidity("", request.getPassword(), role, timeBefore, timeAfter, responseProto);

        User responseRepo = userRepository.findById(responseProto.getId()).orElseThrow();

        assertUserDomainFieldsValidity("", request.getPassword(), role, timeBefore, timeAfter, responseRepo);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    /*
    Проверяет поведение create при отсутствии password в запросе:
    - сервер принимает запрос без password (в protobuf это пустая строка)
    - passwordHash в ответе и в базе соответствует хэшу пустого пароля
    - запись успешно создаётся и доступна по id
    */
    @Test
    void createWithEmptyPassword() {
        UserRole role = UserRole.ADMIN;
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName("user\uD83D\uDD25")
                .setRole(ProtoUserRoleMapper.domainToProto(role))
                .build();

        Instant timeBefore = Instant.now();
        UserProto responseProto = blockingStub.create(request).getUser();
        Instant timeAfter = Instant.now();

        assertUserProtoFieldsValidity(request.getName(), "", role, timeBefore, timeAfter, responseProto);

        User responseRepo = userRepository.findById(responseProto.getId()).orElseThrow();

        assertUserDomainFieldsValidity(request.getName(), "", role, timeBefore, timeAfter, responseRepo);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    /*
    Проверяет валидацию create при отсутствии role:
    - отправляет запрос без role
    - ожидает StatusRuntimeException со статусом INVALID_ARGUMENT
    - проверяет, что описание ошибки указывает на отсутствие UserRoleProto (not specified)
    */
    @Test
    void createWithEmptyRoleInvalidArgumentException() {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName("frank-admin")
                .setPassword("C0b0l&N@vy_1952")
                .build();

        // Ожидаем, что при запросе произошла ошибка
        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.create(request)
        );

        // Проверяем подробности ошибки
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(ex.getStatus().getDescription()).contains("UserRoleProto").contains("not specified");
    }


    /*
    Проверяет happy-path для get по существующим данным:
    - заранее сохраняет двух пользователей в PostgreSQL (с известными паролями, но в виде hash)
    - отправляет два gRPC get-запроса по их id
    - проверяет совпадение полей domain vs proto и корректность passwordHash (пароль матчится)
    - дополнительно убеждается, что возвращаются разные записи (id различаются)
    */
    @Test
    void getSeveralExistingData() {
        String password1 = "T3st_Cas3$2026";
        User entity1 = new User("grace_hopper", PasswordEncryption.makeHash(password1), UserRole.ADMIN);

        String password2 = "Adm1n@Temp#47";
        User entity2 = new User("boris_test", PasswordEncryption.makeHash(password2), UserRole.VISITOR);

        Instant timeBefore = Instant.now();
        userRepository.save(entity1);
        userRepository.save(entity2);
        Instant timeAfter = Instant.now();

        GetUserRequest request1 = GetUserRequest.newBuilder().setId(entity1.getId()).build();
        GetUserRequest request2 = GetUserRequest.newBuilder().setId(entity2.getId()).build();
        UserProto responseProto1 = blockingStub.get(request1).getUser();
        UserProto responseProto2 = blockingStub.get(request2).getUser();

        assertUserDomainProtoValidity(entity1, responseProto1, password1, timeBefore, timeAfter);
        assertUserDomainProtoValidity(entity2, responseProto2, password2, timeBefore, timeAfter);
        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());
    }

    /*
    Проверяет обработку get для отсутствующего id:
    - сохраняет одну реальную запись в базе
    - запрашивает get по id=12 (которого нет)
    - ожидает StatusRuntimeException со статусом NOT_FOUND
    - проверяет, что описание ошибки содержит "not found" и сам id
    */
    @Test
    void getWrongIdNotFoundException() {
        String password = "'ILLK'*prep*2";
        User entity = new User("ivan.petrov", PasswordEncryption.makeHash(password), UserRole.USER);

        userRepository.save(entity);

        GetUserRequest request = GetUserRequest.newBuilder().setId(12).build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.get(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("not found").contains("12");
    }


    /*
    Проверяет getList при указании всех параметров:
    - фильтрация по role
    - фильтрация по диапазону createdAt [createdFrom; createdTo]
    - пагинация (pageNum/pageSize)
    - сортировка по createdAt по убыванию (sortCreatedDesc=true)
    - ожидает строго определённый порядок id в ответе
    */
    @Test
    void getListAllParams() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setRole(ProtoUserRoleMapper.domainToProto(UserRole.USER))
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-05T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-08T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .setSortCreatedDesc(true)
                .build();

        List<Long> expectedUserIds = List.of(8L, 5L);

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет getList без опциональных фильтров:
    - передаёт только pageNum/pageSize
    - ожидает, что сервер вернёт все записи (в пределах размера страницы)
    - проверяет порядок результатов согласно дефолтной сортировке сервера (в тесте ожидается убывание)
    */
    @Test
    void getListNoOptionalParams() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedUserIds = List.of(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет сортировку getList по возрастанию createdAt:
    - передаёт sortCreatedDesc=false
    - ожидает порядок от самых ранних записей к самым поздним
    - проверяет корректность данных и последовательность id
    */
    @Test
    void getListAscSorting() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setPageNum(0)
                .setPageSize(100)
                .setSortCreatedDesc(false)
                .build();

        List<Long> expectedUserIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет фильтрацию getList по роли пользователя:
    - задаёт role=VISITOR
    - ожидает, что вернутся только пользователи с этой ролью
    - проверяет порядок результатов согласно сортировке сервера
    */
    @Test
    void getListByRole() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setRole(ProtoUserRoleMapper.domainToProto(UserRole.VISITOR))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedUserIds = List.of(10L, 7L, 4L, 1L);

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет фильтрацию getList по нижней границе createdFrom:
    - задаёт createdFrom (начиная с 2026-01-07)
    - ожидает, что вернутся записи с createdAt >= createdFrom
    - проверяет порядок и корректность результатов
    */
    @Test
    void getListByCreatedFrom() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-07T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedUserIds = List.of(10L, 9L, 8L, 7L);

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет фильтрацию getList по верхней границе createdTo:
    - задаёт createdTo (до 2026-01-02)
    - ожидает, что вернутся записи с createdAt <= createdTo (или согласно правилам сервера для границы)
    - проверяет порядок и корректность результатов
    */
    @Test
    void getListByCreatedTo() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-02T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedUserIds = List.of(2L, 1L);

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет фильтрацию getList по диапазону createdAt:
    - задаёт createdFrom и createdTo
    - ожидает, что вернутся записи внутри интервала
    - проверяет порядок и корректность результатов
    */
    @Test
    void getListDatesBetween() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-02T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-06T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedUserIds = List.of(6L, 5L, 4L, 3L, 2L);

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет фильтрацию getList пользователей по диапазону дат createdFrom/createdTo, который “не совпадает” с точными датами в тестовой БД, но включает все записи
    - отправляет запрос с createdFrom="2025-01-01" и createdTo="2027-01-01" (границы шире дат у созданных пользователей), pageNum=0, pageSize=100
    - ожидает, что фильтрация отработает без ошибок и вернёт всех пользователей, попадающих в диапазон (в данном наборе — все 10)
    - проверяет, что результаты возвращаются в ожидаемом порядке (сортировка по createdAt/id согласно реализации сервиса) и соответствуют ожидаемым userId (через checkGetListRequest)
    */
    @Test
    void getListNotFromDbDatesBetween() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2025-01-01T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2027-01-01T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedUserIds = List.of(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет поведение getList при конфликтном диапазоне дат:
    - задаёт createdFrom позже, чем createdTo
    - ожидает пустой список (без исключения)
    */
    @Test
    void getListConflictDatesNoData() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-07T00:00:00.000Z")))
                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-05T00:00:00.000Z")))
                .setPageNum(0)
                .setPageSize(100)
                .build();

        List<Long> expectedUserIds = List.of();

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет пагинацию getList на “средней” странице:
    - задаёт pageNum=2 и pageSize=3
    - ожидает конкретный срез данных (строго определённые id)
    - проверяет порядок и корректность каждого элемента
    */
    @Test
    void getListMidPage() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setPageNum(2)
                .setPageSize(3)
                .build();

        List<Long> expectedUserIds = List.of(4L, 3L, 2L);

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет поведение getList при выходе за пределы страниц:
    - задаёт слишком большой pageNum относительно размера набора данных
    - ожидает пустой результат
    */
    @Test
    void getListExceedingPageNum() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
                .setPageNum(5)
                .setPageSize(2)
                .build();

        List<Long> expectedUserIds = List.of();

        checkGetListRequest(request, expectedUserIds);
    }

    /*
    Проверяет валидацию параметров getList: pageSize = 0
    - отправляет запрос с pageSize=0
    - ожидает StatusRuntimeException со статусом UNAVAILABLE
    - проверяет, что описание ошибки содержит упоминание некорректного размера (less than one)
    */
    @Test
    void getListZeroPageSizeUnavailableException() {
        GetListUserRequest request = GetListUserRequest.newBuilder()
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
        GetListUserRequest request = GetListUserRequest.newBuilder()
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
    Проверяет happy-path для delete по существующим данным:
    - сохраняет двух пользователей в базе
    - удаляет их по очереди через gRPC delete
    - проверяет, что количество записей уменьшается 2 -> 1 -> 0
    */
    @Test
    void deleteSeveralExistingData() {
        User entity1 = new User("daria.qa", PasswordEncryption.makeHash("Em0ji_\uD83D\uDD25_P@ss9"), UserRole.ADMIN);
        User entity2 = new User("eve.user", PasswordEncryption.makeHash("V!olet-88-Keys"), UserRole.USER);

        userRepository.save(entity1);
        userRepository.save(entity2);

        DeleteUserRequest request1 = DeleteUserRequest.newBuilder().setId(entity1.getId()).build();
        DeleteUserRequest request2 = DeleteUserRequest.newBuilder().setId(entity2.getId()).build();

        assertThat(userRepository.count()).isEqualTo(2);
        blockingStub.delete(request1);
        assertThat(userRepository.count()).isEqualTo(1);
        blockingStub.delete(request2);
        assertThat(userRepository.count()).isEqualTo(0);
    }

    /*
    Проверяет обработку delete для отсутствующего id:
    - сохраняет одну запись в базе
    - пытается удалить пользователя по id=-2
    - ожидает StatusRuntimeException со статусом NOT_FOUND
    - проверяет, что описание ошибки содержит "didn't exist" и id
    */
    @Test
    void deleteWrongIdNotFoundException() {
        User entity = new User("student", PasswordEncryption.makeHash("not a password"), UserRole.VISITOR);

        userRepository.save(entity);

        DeleteUserRequest request = DeleteUserRequest.newBuilder().setId(-2).build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.delete(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("didn't exist").contains("-2");
    }

    /*
    Проверяет каскадное удаление зависимых сущностей при удалении пользователя (User -> ParsingParam)
    - создаёт пользователя и сохраняет его в БД
    - создаёт две записи ParsingParam, привязанные к этому пользователю, и сохраняет их
    - убеждается, что в БД 1 пользователь и 2 зависимых параметра
    - вызывает gRPC метод delete по id пользователя
    - ожидает, что удаление пользователя приведёт к каскадному удалению всех связанных ParsingParam
    - проверяет, что в БД не осталось ни пользователя, ни его зависимых параметров
    */
    @Test
    void deleteOnCascade() {
        User entity = new User("mmnsm", PasswordEncryption.makeHash("mmnsm_pass"), UserRole.USER);
        userRepository.save(entity);

        ParsingParam parsingParam1 = new ParsingParam(entity,"name1", "description1");
        ParsingParam parsingParam2 = new ParsingParam(entity,"name2", "description2");
        parsingParamRepository.save(parsingParam1);
        parsingParamRepository.save(parsingParam2);

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(parsingParamRepository.count()).isEqualTo(2);

        DeleteUserRequest request = DeleteUserRequest.newBuilder().setId(entity.getId()).build();
        blockingStub.delete(request);

        // Пользователь удалился вместе с зависимыми от него параметрами
        assertThat(userRepository.count()).isEqualTo(0);
        assertThat(parsingParamRepository.count()).isEqualTo(0);
    }


    /*
    Проверяет изменение роли (setRole) для нескольких пользователей:
    - сохраняет двух пользователей с разными исходными ролями
    - отправляет два gRPC setRole-запроса на смену ролей
    - перечитывает пользователей из базы и проверяет, что роль реально изменилась
    - дополнительно проверяет, что роли в ответах сервера совпадают с обновлёнными значениями
    */
    @Test
    void setRoleSeveral() {
        User entity1 = new User("user+tag", PasswordEncryption.makeHash("t@g+user_4"), UserRole.ADMIN);
        User entity2 = new User("space user", PasswordEncryption.makeHash("sp ace__PW9!"), UserRole.VISITOR);

        userRepository.save(entity1);
        userRepository.save(entity2);

        // Проверяем, что в базе находятся старые роли
        assertThat(entity1.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(entity2.getRole()).isEqualTo(UserRole.VISITOR);

        // Делаем запрос на изменение ролей
        SetUserRoleRequest request1 = SetUserRoleRequest.newBuilder()
                .setId(entity1.getId())
                .setRole(ProtoUserRoleMapper.domainToProto(UserRole.USER))
                .build();
        SetUserRoleRequest request2 = SetUserRoleRequest.newBuilder()
                .setId(entity2.getId())
                .setRole(ProtoUserRoleMapper.domainToProto(UserRole.ADMIN))
                .build();
        UserProto responseProto1 = blockingStub.setRole(request1).getUser();
        UserProto responseProto2 = blockingStub.setRole(request2).getUser();

        // Получаем новые (изменённые) объекты из базы данных
        User updatedEntity1 = userRepository.findById(entity1.getId()).orElseThrow();
        User updatedEntity2 = userRepository.findById(entity2.getId()).orElseThrow();

        // Проверяем, что роли изменились (в response и в реальной базе данных)
        assertThat(ProtoUserRoleMapper.protoToDomain(responseProto1.getRole())).isEqualTo(UserRole.USER);
        assertThat(ProtoUserRoleMapper.protoToDomain(responseProto2.getRole())).isEqualTo(UserRole.ADMIN);
        assertThat(updatedEntity1.getRole()).isEqualTo(UserRole.USER);
        assertThat(updatedEntity2.getRole()).isEqualTo(UserRole.ADMIN);
    }

    /*
    Проверяет setRole при установке той же самой роли:
    - сохраняет пользователя с ролью ADMIN
    - отправляет setRole с ролью ADMIN (без фактического изменения)
    - проверяет, что сервер возвращает ADMIN и в базе роль остаётся ADMIN
    */
    @Test
    void setRoleNoChanges() {
        User entity = new User("admin.test", PasswordEncryption.makeHash("Adm1n$ecret!!"), UserRole.ADMIN);

        userRepository.save(entity);

        assertThat(entity.getRole()).isEqualTo(UserRole.ADMIN);

        SetUserRoleRequest request = SetUserRoleRequest.newBuilder()
                .setId(entity.getId())
                .setRole(ProtoUserRoleMapper.domainToProto(UserRole.ADMIN))
                .build();
        UserProto responseProto = blockingStub.setRole(request).getUser();

        User updatedEntity = userRepository.findById(entity.getId()).orElseThrow();

        assertThat(ProtoUserRoleMapper.protoToDomain(responseProto.getRole())).isEqualTo(UserRole.ADMIN);
        assertThat(updatedEntity.getRole()).isEqualTo(UserRole.ADMIN);
    }

    /*
    Проверяет валидацию setRole при отсутствии role в запросе:
    - сохраняет пользователя (роль USER)
    - отправляет запрос setRole без role
    - ожидает StatusRuntimeException со статусом INVALID_ARGUMENT
    - проверяет сообщение об ошибке (UserRoleProto not specified)
    - убеждается, что роль пользователя в базе не изменилась
    */
    @Test
    void setRoleEmptyRoleInvalidArgumentException() {
        User entity = new User("boris_01", PasswordEncryption.makeHash("borisPASS_2026"), UserRole.USER);

        userRepository.save(entity);

        assertThat(entity.getRole()).isEqualTo(UserRole.USER);

        SetUserRoleRequest request = SetUserRoleRequest.newBuilder()
                .setId(entity.getId())
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.setRole(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(ex.getStatus().getDescription()).contains("UserRoleProto").contains("not specified");

        // Роль не изменилась
        assertThat(entity.getRole()).isEqualTo(UserRole.USER);
    }

    /*
    Проверяет обработку setRole для отсутствующего id:
    - сохраняет пользователя (роль VISITOR)
    - отправляет setRole по id=-1
    - ожидает StatusRuntimeException со статусом NOT_FOUND и описанием "not found"
    - убеждается, что роль пользователя в базе не изменилась
    */
    @Test
    void setRoleWrongIdNotFoundException() {
        User entity = new User("unicode_пользователь", PasswordEncryption.makeHash("Пароль#2026"), UserRole.VISITOR);

        userRepository.save(entity);

        assertThat(entity.getRole()).isEqualTo(UserRole.VISITOR);

        SetUserRoleRequest request = SetUserRoleRequest.newBuilder()
                .setId(-1)
                .setRole(ProtoUserRoleMapper.domainToProto(UserRole.USER))
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.setRole(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("not found").contains("-1");

        // Роль не изменилась
        assertThat(entity.getRole()).isEqualTo(UserRole.VISITOR);
    }


    /*
    Проверяет checkPassword для корректных паролей:
    - сохраняет двух пользователей с хэшами известных паролей
    - дополнительно проверяет локально, что PasswordEncryption.matching работает корректно
    - отправляет два gRPC checkPassword-запроса с правильными паролями
    - ожидает match=true для обоих пользователей
    */
    @Test
    void checkPasswordSeveralCorrect() {
        User entity1 = new User("a", PasswordEncryption.makeHash("A_very_long_password_0000000001!"), UserRole.VISITOR);
        User entity2 = new User("a", PasswordEncryption.makeHash("Jd-2026-Strong*Pw"), UserRole.USER);

        userRepository.save(entity1);
        userRepository.save(entity2);

        // Проверяем, что в базе находятся заданные пароли и их проверка корректно работает
        assertThat(PasswordEncryption.checkMatching("A_very_long_password_0000000001!", entity1.getPasswordHash())).isTrue();
        assertThat(PasswordEncryption.checkMatching("Jd-2026-Strong*Pw", entity2.getPasswordHash())).isTrue();

        CheckUserPasswordRequest request1 = CheckUserPasswordRequest.newBuilder()
                .setId(entity1.getId())
                .setPassword("A_very_long_password_0000000001!")
                .build();
        CheckUserPasswordRequest request2 = CheckUserPasswordRequest.newBuilder()
                .setId(entity2.getId())
                .setPassword("Jd-2026-Strong*Pw")
                .build();
        boolean isMatched1 = blockingStub.checkPassword(request1).getMatch();
        boolean isMatched2 = blockingStub.checkPassword(request2).getMatch();

        assertThat(isMatched1).isTrue();
        assertThat(isMatched2).isTrue();
    }

    /*
    Проверяет checkPassword для неверных паролей:
    - сохраняет двух пользователей с валидными хэшами известных паролей
    - отправляет gRPC checkPassword с неправильными паролями
    - ожидает match=false для обоих запросов
    */
    @Test
    void checkPasswordSeveralIncorrect() {
        User entity1 = new User("grb", PasswordEncryption.makeHash("FirePass!23"), UserRole.VISITOR);
        User entity2 = new User("kit123", PasswordEncryption.makeHash("OldPassword13748"), UserRole.USER);

        userRepository.save(entity1);
        userRepository.save(entity2);

        // Проверяем, что в базе находятся заданные пароли и их проверка корректно работает
        assertThat(PasswordEncryption.checkMatching("FirePass!23", entity1.getPasswordHash())).isTrue();
        assertThat(PasswordEncryption.checkMatching("OldPassword13748", entity2.getPasswordHash())).isTrue();

        CheckUserPasswordRequest request1 = CheckUserPasswordRequest.newBuilder()
                .setId(entity1.getId())
                .setPassword("incorrect password 1")
                .build();
        CheckUserPasswordRequest request2 = CheckUserPasswordRequest.newBuilder()
                .setId(entity2.getId())
                .setPassword("incorrect password 2")
                .build();
        boolean isMatched1 = blockingStub.checkPassword(request1).getMatch();
        boolean isMatched2 = blockingStub.checkPassword(request2).getMatch();

        assertThat(isMatched1).isFalse();
        assertThat(isMatched2).isFalse();
    }

    /*
    Проверяет checkPassword при пустом пароле:
    - сохраняет пользователя с хэшом пустой строки
    - отправляет checkPassword без поля password (в protobuf это пустая строка)
    - ожидает match=true, т.к. пустой пароль должен совпасть с сохранённым хэшем
    */
    @Test
    void checkPasswordEmptyPassword() {
        User entity = new User("empty mt", PasswordEncryption.makeHash(""), UserRole.ADMIN);

        userRepository.save(entity);

        // Проверяем, что в базе находятся заданные пароли и их проверка корректно работает
        assertThat(PasswordEncryption.checkMatching("", entity.getPasswordHash())).isTrue();

        CheckUserPasswordRequest request = CheckUserPasswordRequest.newBuilder()
                .setId(entity.getId())
                .build();
        boolean isMatched = blockingStub.checkPassword(request).getMatch();

        assertThat(isMatched).isTrue();
    }

    /*
    Проверяет обработку checkPassword для отсутствующего id:
    - сохраняет одного пользователя
    - запрашивает проверку пароля по id=-1
    - ожидает StatusRuntimeException со статусом NOT_FOUND
    - проверяет, что описание ошибки содержит "not found" и id
    */
    @Test
    void checkPasswordWrongIdNotFoundException() {
        User entity = new User("user_name", PasswordEncryption.makeHash("123"), UserRole.USER);

        userRepository.save(entity);

        CheckUserPasswordRequest request = CheckUserPasswordRequest.newBuilder()
                .setId(-1)
                .build();

        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.checkPassword(request)
        );

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ex.getStatus().getDescription()).contains("not found").contains("-1");
    }


    // Вспомогательный метод для тестов getList
    void checkGetListRequest(GetListUserRequest request, List<Long> expectedUserIds) {
        List<String> passwords = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        List<User> entities = List.of(
                User.builder().id(1L).name("name1").passwordHash(PasswordEncryption.makeHash(passwords.get(0))).role(UserRole.VISITOR).createdAt(Instant.parse("2026-01-01T00:00:00.000Z")).build(),
                User.builder().id(2L).name("name2").passwordHash(PasswordEncryption.makeHash(passwords.get(1))).role(UserRole.USER).createdAt(Instant.parse("2026-01-02T00:00:00.000Z")).build(),
                User.builder().id(3L).name("name3").passwordHash(PasswordEncryption.makeHash(passwords.get(2))).role(UserRole.ADMIN).createdAt(Instant.parse("2026-01-03T00:00:00.000Z")).build(),
                User.builder().id(4L).name("name4").passwordHash(PasswordEncryption.makeHash(passwords.get(3))).role(UserRole.VISITOR).createdAt(Instant.parse("2026-01-04T00:00:00.000Z")).build(),
                User.builder().id(5L).name("name5").passwordHash(PasswordEncryption.makeHash(passwords.get(4))).role(UserRole.USER).createdAt(Instant.parse("2026-01-05T00:00:00.000Z")).build(),
                User.builder().id(6L).name("name6").passwordHash(PasswordEncryption.makeHash(passwords.get(5))).role(UserRole.ADMIN).createdAt(Instant.parse("2026-01-06T00:00:00.000Z")).build(),
                User.builder().id(7L).name("name7").passwordHash(PasswordEncryption.makeHash(passwords.get(6))).role(UserRole.VISITOR).createdAt(Instant.parse("2026-01-07T00:00:00.000Z")).build(),
                User.builder().id(8L).name("name8").passwordHash(PasswordEncryption.makeHash(passwords.get(7))).role(UserRole.USER).createdAt(Instant.parse("2026-01-08T00:00:00.000Z")).build(),
                User.builder().id(9L).name("name9").passwordHash(PasswordEncryption.makeHash(passwords.get(8))).role(UserRole.ADMIN).createdAt(Instant.parse("2026-01-09T00:00:00.000Z")).build(),
                User.builder().id(10L).name("name10").passwordHash(PasswordEncryption.makeHash(passwords.get(9))).role(UserRole.VISITOR).createdAt(Instant.parse("2026-01-10T00:00:00.000Z")).build()
        );

        // Вставляем данные в базу данных
        rawDbInsert(entities);
        assertThat(userRepository.count()).isEqualTo(entities.size());

        // Отправляем gRPC серверу запрос
        List<UserProto> actualUsersProto = blockingStub.getList(request).getUsersList();
        assertThat(actualUsersProto.size()).isEqualTo(expectedUserIds.size());

        // Проверяем порядок и соответствие полей
        for (int i = 0; i < expectedUserIds.size(); i++) {
            int expectedPasswordIdx = Math.toIntExact(expectedUserIds.get(i)) - 1;
            String expectedPassword = passwords.get(expectedPasswordIdx);
            User userDomain = userRepository.findById(expectedUserIds.get(i)).orElseThrow();
            UserProto userProto = actualUsersProto.get(i);
            assertUserDomainProtoValidity(userDomain, userProto, expectedPassword, null, null);
        }
    }

    // Низкоуровневая вставка тестовых данных напрямую в PostgreSQL
    void rawDbInsert(List<User> entities) {
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

    // Проверяет валидность полей UserProto (ответ gRPC, обычно после create)
    private void assertUserProtoFieldsValidity(
            String expectedName,
            String expectedPassword,
            UserRole expectedRole,
            Instant timeBefore,
            Instant timeAfter,
            UserProto actualProto
    ) {
        assertThat(actualProto.getName()).isEqualTo(expectedName);
        assertThat(PasswordEncryption.checkMatching(expectedPassword, actualProto.getPasswordHash())).isTrue();
        assertThat(ProtoUserRoleMapper.protoToDomain(actualProto.getRole())).isEqualTo(expectedRole);

        assertThat(ProtoTimeMapper.timestampToInstant(actualProto.getCreatedAt()))
                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
    }

    // Проверяет валидность полей доменной сущности User, прочитанной из БД
    private void assertUserDomainFieldsValidity(
            String expectedName,
            String expectedPassword,
            UserRole expectedRole,
            Instant timeBefore,
            Instant timeAfter,
            User actualDomain
    ) {
        assertThat(actualDomain.getName()).isEqualTo(expectedName);
        assertThat(PasswordEncryption.checkMatching(expectedPassword, actualDomain.getPasswordHash())).isTrue();
        assertThat(actualDomain.getRole()).isEqualTo(expectedRole);

        assertThat(actualDomain.getCreatedAt())
                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
    }

    // Проверяет соответствие одной и той же сущности в двух представлениях (domain vs proto)
    private void assertUserDomainProtoValidity(
            User domain,
            UserProto proto,
            String expectedPassword,
            @Nullable Instant timeBefore,
            @Nullable Instant timeAfter
    ) {
        assertThat(domain.getId()).isEqualTo(proto.getId());
        assertThat(domain.getName()).isEqualTo(proto.getName());
        assertThat(PasswordEncryption.checkMatching(expectedPassword, domain.getPasswordHash())).isTrue();
        assertThat(PasswordEncryption.checkMatching(expectedPassword, proto.getPasswordHash())).isTrue();
        assertThat(domain.getRole()).isEqualTo(ProtoUserRoleMapper.protoToDomain(proto.getRole()));
        assertThat(domain.getCreatedAt()).isCloseTo(
                ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()),
                within(1, ChronoUnit.MILLIS));

        if ((timeBefore != null) && (timeAfter != null)) {
            assertThat(domain.getCreatedAt())
                    .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
        }
    }
}