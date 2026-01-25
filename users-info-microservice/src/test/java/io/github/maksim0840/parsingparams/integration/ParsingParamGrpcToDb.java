package io.github.maksim0840.parsingparams.integration;

import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.extraction_result.v1.mapper.ProtoJsonMapper;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import io.github.maksim0840.internalapi.user.v1.mapper.ProtoUserRoleMapper;
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
    private UserServiceGrpc.UserServiceBlockingStub blockingStub;

    // Репозиторий для отправки запросов к базе данных
    @Autowired
    ParsingParamRepository repository;

    // Объект для более низкоуровневых операций с базой данных (по сравнению с репозиторием)
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    // Очищаем базу перед каждым новым тестом
    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

//    @Test
//    void createSeveral() {
//        // Создаём запросы
//        UserRole role1 = UserRole.USER;
//        CreateUserRequest request1 = CreateUserRequest.newBuilder()
//                .setName("user123")
//                .setPassword("aHjksd82lsKK")
//                .setRole(ProtoUserRoleMapper.domainToProto(role1))
//                .build();
//
//        UserRole role2 = UserRole.VISITOR;
//        CreateUserRequest request2 = CreateUserRequest.newBuilder()
//                .setName("pdurov")
//                .setPassword("qwerty12345")
//                .setRole(ProtoUserRoleMapper.domainToProto(role2))
//                .build();
//
//        // Отправляем запросы на сервер и получаем ответы
//        Instant timeBefore = Instant.now();
//        UserProto responseProto1 = blockingStub.create(request1).getUser();
//        UserProto responseProto2 = blockingStub.create(request2).getUser();
//        Instant timeAfter = Instant.now();
//
//        // Проверяем валидность полей ответа от gRPC сервера
//        assertUserProtoFieldsValidity(request1.getName(), request1.getPassword(), role1, timeBefore, timeAfter, responseProto1);
//        assertUserProtoFieldsValidity(request2.getName(), request2.getPassword(), role2, timeBefore, timeAfter, responseProto2);
//        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());
//
//        // Получаем результаты записи в бд
//        User responseRepo1 = repository.findById(responseProto1.getId()).orElseThrow();
//        User responseRepo2 = repository.findById(responseProto2.getId()).orElseThrow();
//
//        // Проверяем валидность записанной в базу данных информации
//        assertUserDomainFieldsValidity(request1.getName(), request1.getPassword(), role1, timeBefore, timeAfter, responseRepo1);
//        assertUserDomainFieldsValidity(request2.getName(), request2.getPassword(), role2, timeBefore, timeAfter, responseRepo2);
//        assertThat(responseRepo1.getId()).isNotEqualTo(responseRepo2.getId());
//        assertThat(repository.count()).isEqualTo(2);
//    }
//
//    @Test
//    void createWithEmptyName() {
//        UserRole role = UserRole.USER;
//        CreateUserRequest request = CreateUserRequest.newBuilder()
//                .setPassword("zX9!mN4#pQ1")
//                .setRole(ProtoUserRoleMapper.domainToProto(role))
//                .build();
//
//        Instant timeBefore = Instant.now();
//        UserProto responseProto = blockingStub.create(request).getUser();
//        Instant timeAfter = Instant.now();
//
//        assertUserProtoFieldsValidity("", request.getPassword(), role, timeBefore, timeAfter, responseProto);
//
//        User responseRepo = repository.findById(responseProto.getId()).orElseThrow();
//
//        assertUserDomainFieldsValidity("", request.getPassword(), role, timeBefore, timeAfter, responseRepo);
//        assertThat(repository.count()).isEqualTo(1);
//    }
//
//    @Test
//    void createWithEmptyPassword() {
//        UserRole role = UserRole.ADMIN;
//        CreateUserRequest request = CreateUserRequest.newBuilder()
//                .setName("user\uD83D\uDD25")
//                .setRole(ProtoUserRoleMapper.domainToProto(role))
//                .build();
//
//        Instant timeBefore = Instant.now();
//        UserProto responseProto = blockingStub.create(request).getUser();
//        Instant timeAfter = Instant.now();
//
//        assertUserProtoFieldsValidity(request.getName(), "", role, timeBefore, timeAfter, responseProto);
//
//        User responseRepo = repository.findById(responseProto.getId()).orElseThrow();
//
//        assertUserDomainFieldsValidity(request.getName(), "", role, timeBefore, timeAfter, responseRepo);
//        assertThat(repository.count()).isEqualTo(1);
//    }
//
//    @Test
//    void createWithEmptyRoleInvalidArgumentException() {
//        CreateUserRequest request = CreateUserRequest.newBuilder()
//                .setName("frank-admin")
//                .setPassword("C0b0l&N@vy_1952")
//                .build();
//
//        // Ожидаем, что при запросе произошла ошибка
//        StatusRuntimeException ex = assertThrows(
//                StatusRuntimeException.class,
//                () -> blockingStub.create(request)
//        );
//
//        // Проверяем подробности ошибки
//        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
//        assertThat(ex.getStatus().getDescription()).contains("UserRoleProto").contains("not specified");
//    }
//
//
//    @Test
//    void getSeveralExistingData() {
//        String password1 = "T3st_Cas3$2026";
//        User entity1 = new User("grace_hopper", PasswordEncryption.makeHash(password1), UserRole.ADMIN);
//
//        String password2 = "Adm1n@Temp#47";
//        User entity2 = new User("boris_test", PasswordEncryption.makeHash(password2), UserRole.VISITOR);
//
//        Instant timeBefore = Instant.now();
//        repository.save(entity1);
//        repository.save(entity2);
//        Instant timeAfter = Instant.now();
//
//        GetUserRequest request1 = GetUserRequest.newBuilder().setId(entity1.getId()).build();
//        GetUserRequest request2 = GetUserRequest.newBuilder().setId(entity2.getId()).build();
//        UserProto responseProto1 = blockingStub.get(request1).getUser();
//        UserProto responseProto2 = blockingStub.get(request2).getUser();
//
//        assertUserDomainProtoValidity(entity1, responseProto1, password1, timeBefore, timeAfter);
//        assertUserDomainProtoValidity(entity2, responseProto2, password2, timeBefore, timeAfter);
//        assertThat(responseProto1.getId()).isNotEqualTo(responseProto2.getId());
//    }
//
//
//    @Test
//    void getWrongIdNotFoundException() {
//        String password = "'ILLK'*prep*2";
//        User entity = new User("ivan.petrov", PasswordEncryption.makeHash(password), UserRole.USER);
//
//        repository.save(entity);
//
//        GetUserRequest request = GetUserRequest.newBuilder().setId(12).build();
//
//        StatusRuntimeException ex = assertThrows(
//                StatusRuntimeException.class,
//                () -> blockingStub.get(request)
//        );
//
//        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
//        assertThat(ex.getStatus().getDescription()).contains("not found").contains("12");
//    }
//
//    @Test
//    void getListAllParams() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setRole(ProtoUserRoleMapper.domainToProto(UserRole.USER))
//                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-05T00:00:00.000Z")))
//                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-08T00:00:00.000Z")))
//                .setPageNum(0)
//                .setPageSize(100)
//                .setSortCreatedDesc(true)
//                .build();
//
//        List<Long> expectedUserIds = List.of(8L, 5L);
//
//        checkGetListRequest(request, expectedUserIds);
//    }
//
//    @Test
//    void getListNoOptionalParams() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<Long> expectedUserIds = List.of(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);
//
//        checkGetListRequest(request, expectedUserIds);
//    }
//
//    @Test
//    void getListAscSorting() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setPageNum(0)
//                .setPageSize(100)
//                .setSortCreatedDesc(false)
//                .build();
//
//        List<Long> expectedUserIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
//
//        checkGetListRequest(request, expectedUserIds);
//    }
//
//    @Test
//    void getListByRole() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setRole(ProtoUserRoleMapper.domainToProto(UserRole.VISITOR))
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<Long> expectedUserIds = List.of(10L, 7L, 4L, 1L);
//
//        checkGetListRequest(request, expectedUserIds);
//    }
//
//    @Test
//    void getListByCreatedFrom() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-07T00:00:00.000Z")))
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<Long> expectedUserIds = List.of(10L, 9L, 8L, 7L);
//
//        checkGetListRequest(request, expectedUserIds);
//    }
//
//    @Test
//    void getListByCreatedTo() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-02T00:00:00.000Z")))
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<Long> expectedUserIds = List.of(2L, 1L);
//
//        checkGetListRequest(request, expectedUserIds);
//    }
//
//    @Test
//    void getListDatesBetween() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-02T00:00:00.000Z")))
//                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-06T00:00:00.000Z")))
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<Long> expectedUserIds = List.of(6L, 5L, 4L, 3L, 2L);
//
//        checkGetListRequest(request, expectedUserIds);
//    }
//
//    @Test
//    void getListConflictDatesNoData() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setCreatedFrom(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-07T00:00:00.000Z")))
//                .setCreatedTo(ProtoTimeMapper.instantToTimestamp(Instant.parse("2026-01-05T00:00:00.000Z")))
//                .setPageNum(0)
//                .setPageSize(100)
//                .build();
//
//        List<Long> expectedUserIds = List.of();
//
//        checkGetListRequest(request, expectedUserIds);
//    }
//
//    @Test
//    void getListMidPage() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setPageNum(2)
//                .setPageSize(3)
//                .build();
//
//        List<Long> expectedUserIds = List.of(4L, 3L, 2L);
//
//        checkGetListRequest(request, expectedUserIds);
//    }
//
//    @Test
//    void getListExceedingPageNum() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setPageNum(5)
//                .setPageSize(2)
//                .build();
//
//        List<Long> expectedUserIds = List.of();
//
//        checkGetListRequest(request, expectedUserIds);
//    }
//
//    @Test
//    void getListZeroPageSizeUnavailableException() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setPageNum(0)
//                .setPageSize(0)
//                .build();
//
//        StatusRuntimeException ex = assertThrows(
//                StatusRuntimeException.class,
//                () -> blockingStub.getList(request)
//        );
//
//        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
//        assertThat(ex.getStatus().getDescription()).contains("size").contains("less than one");
//    }
//
//    @Test
//    void getListNegativePageNumUnavailableException() {
//        GetListUserRequest request = GetListUserRequest.newBuilder()
//                .setPageNum(-1)
//                .setPageSize(37)
//                .build();
//
//        StatusRuntimeException ex = assertThrows(
//                StatusRuntimeException.class,
//                () -> blockingStub.getList(request)
//        );
//
//        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
//        assertThat(ex.getStatus().getDescription()).contains("index").contains("less than zero");
//    }
//
//
//    @Test
//    void deleteSeveralExistingData() {
//        User entity1 = new User("daria.qa", PasswordEncryption.makeHash("Em0ji_\uD83D\uDD25_P@ss9"), UserRole.ADMIN);
//        User entity2 = new User("eve.user", PasswordEncryption.makeHash("V!olet-88-Keys"), UserRole.USER);
//
//        repository.save(entity1);
//        repository.save(entity2);
//
//        DeleteUserRequest request1 = DeleteUserRequest.newBuilder().setId(entity1.getId()).build();
//        DeleteUserRequest request2 = DeleteUserRequest.newBuilder().setId(entity2.getId()).build();
//
//        assertThat(repository.count()).isEqualTo(2);
//        blockingStub.delete(request1);
//        assertThat(repository.count()).isEqualTo(1);
//        blockingStub.delete(request2);
//        assertThat(repository.count()).isEqualTo(0);
//    }
//
//    @Test
//    void deleteWrongIdNotFoundException() {
//        User entity = new User("student", PasswordEncryption.makeHash("not a password"), UserRole.VISITOR);
//
//        repository.save(entity);
//
//        DeleteUserRequest request = DeleteUserRequest.newBuilder().setId(-2).build();
//
//        StatusRuntimeException ex = assertThrows(
//                StatusRuntimeException.class,
//                () -> blockingStub.delete(request)
//        );
//
//        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
//        assertThat(ex.getStatus().getDescription()).contains("didn't exist").contains("-2");
//    }
//
//    void checkGetListRequest(GetListUserRequest request, List<Long> expectedUserIds) {
//        List<String> passwords = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
//        List<User> entities = List.of(
//                User.builder().id(1L).name("name1").passwordHash(PasswordEncryption.makeHash(passwords.get(0))).role(UserRole.VISITOR).createdAt(Instant.parse("2026-01-01T00:00:00.000Z")).build(),
//                User.builder().id(2L).name("name2").passwordHash(PasswordEncryption.makeHash(passwords.get(1))).role(UserRole.USER).createdAt(Instant.parse("2026-01-02T00:00:00.000Z")).build(),
//                User.builder().id(3L).name("name3").passwordHash(PasswordEncryption.makeHash(passwords.get(2))).role(UserRole.ADMIN).createdAt(Instant.parse("2026-01-03T00:00:00.000Z")).build(),
//                User.builder().id(4L).name("name4").passwordHash(PasswordEncryption.makeHash(passwords.get(3))).role(UserRole.VISITOR).createdAt(Instant.parse("2026-01-04T00:00:00.000Z")).build(),
//                User.builder().id(5L).name("name5").passwordHash(PasswordEncryption.makeHash(passwords.get(4))).role(UserRole.USER).createdAt(Instant.parse("2026-01-05T00:00:00.000Z")).build(),
//                User.builder().id(6L).name("name6").passwordHash(PasswordEncryption.makeHash(passwords.get(5))).role(UserRole.ADMIN).createdAt(Instant.parse("2026-01-06T00:00:00.000Z")).build(),
//                User.builder().id(7L).name("name7").passwordHash(PasswordEncryption.makeHash(passwords.get(6))).role(UserRole.VISITOR).createdAt(Instant.parse("2026-01-07T00:00:00.000Z")).build(),
//                User.builder().id(8L).name("name8").passwordHash(PasswordEncryption.makeHash(passwords.get(7))).role(UserRole.USER).createdAt(Instant.parse("2026-01-08T00:00:00.000Z")).build(),
//                User.builder().id(9L).name("name9").passwordHash(PasswordEncryption.makeHash(passwords.get(8))).role(UserRole.ADMIN).createdAt(Instant.parse("2026-01-09T00:00:00.000Z")).build(),
//                User.builder().id(10L).name("name10").passwordHash(PasswordEncryption.makeHash(passwords.get(9))).role(UserRole.VISITOR).createdAt(Instant.parse("2026-01-10T00:00:00.000Z")).build()
//        );
//
//        // Вставляем данные в базу данных
//        rawDbInsert(entities);
//        assertThat(repository.count()).isEqualTo(entities.size());
//
//        // Отправляем gRPC серверу запрос
//        List<UserProto> actualUsersProto = blockingStub.getList(request).getUsersList();
//        assertThat(actualUsersProto.size()).isEqualTo(expectedUserIds.size());
//
//        // Проверяем порядок и соответствие полей
//        for (int i = 0; i < expectedUserIds.size(); i++) {
//            int expectedPasswordIdx = Math.toIntExact(expectedUserIds.get(i)) - 1;
//            String expectedPassword = passwords.get(expectedPasswordIdx);
//            User userDomain = repository.findById(expectedUserIds.get(i)).orElseThrow();
//            UserProto userProto = actualUsersProto.get(i);
//            assertUserDomainProtoValidity(userDomain, userProto, expectedPassword, null, null);
//        }
//    }
//
//    void rawDbInsert(List<User> entities) {
//        String sql = """
//            INSERT INTO users (id, name, password_hash, role, created_at)
//            VALUES (:id, :name, :password_hash, :role, :created_at)
//            """;
//
//        for (User e : entities) {
//            jdbcTemplate.update(sql, new MapSqlParameterSource()
//                    .addValue("id", e.getId())
//                    .addValue("name", e.getName())
//                    .addValue("password_hash", e.getPasswordHash())
//                    .addValue("role", e.getRole().name())
//                    .addValue("created_at", Timestamp.from(e.getCreatedAt()))
//            );
//        }
//    }
//
//    private void assertUserProtoFieldsValidity(
//            String expectedName,
//            String expectedPassword,
//            UserRole expectedRole,
//            Instant timeBefore,
//            Instant timeAfter,
//            UserProto actualProto
//    ) {
//        assertThat(actualProto.getName()).isEqualTo(expectedName);
//        assertThat(PasswordEncryption.checkMatching(expectedPassword, actualProto.getPasswordHash())).isTrue();
//        assertThat(ProtoUserRoleMapper.protoToDomain(actualProto.getRole())).isEqualTo(expectedRole);
//
//        assertThat(ProtoTimeMapper.timestampToInstant(actualProto.getCreatedAt()))
//                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
//    }
//
//    private void assertUserDomainFieldsValidity(
//            String expectedName,
//            String expectedPassword,
//            UserRole expectedRole,
//            Instant timeBefore,
//            Instant timeAfter,
//            User actualDomain
//    ) {
//        assertThat(actualDomain.getName()).isEqualTo(expectedName);
//        assertThat(PasswordEncryption.checkMatching(expectedPassword, actualDomain.getPasswordHash())).isTrue();
//        assertThat(actualDomain.getRole()).isEqualTo(expectedRole);
//
//        assertThat(actualDomain.getCreatedAt())
//                .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
//    }
//
//    private void assertUserDomainProtoValidity(
//            User domain,
//            UserProto proto,
//            String expectedPassword,
//            @Nullable Instant timeBefore,
//            @Nullable Instant timeAfter
//    ) {
//        assertThat(domain.getId()).isEqualTo(proto.getId());
//        assertThat(domain.getName()).isEqualTo(proto.getName());
//        assertThat(PasswordEncryption.checkMatching(expectedPassword, domain.getPasswordHash())).isTrue();
//        assertThat(PasswordEncryption.checkMatching(expectedPassword, proto.getPasswordHash())).isTrue();
//        assertThat(domain.getRole()).isEqualTo(ProtoUserRoleMapper.protoToDomain(proto.getRole()));
//        assertThat(domain.getCreatedAt()).isCloseTo(
//                ProtoTimeMapper.timestampToInstant(proto.getCreatedAt()),
//                within(1, ChronoUnit.MILLIS));
//
//        if ((timeBefore != null) && (timeAfter != null)) {
//            assertThat(domain.getCreatedAt())
//                    .isBetween(timeBefore.minusSeconds(2), timeAfter.plusSeconds(2));
//        }
//    }
}