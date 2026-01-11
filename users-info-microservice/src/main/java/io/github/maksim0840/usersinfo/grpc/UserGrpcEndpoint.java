package io.github.maksim0840.usersinfo.grpc;

import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import io.github.maksim0840.internalapi.user.v1.mapper.ProtoUserRoleMapper;
import io.github.maksim0840.user.v1.*;
import io.github.maksim0840.usersinfo.domain.User;
import io.github.maksim0840.usersinfo.exception.EncryptionException;
import io.github.maksim0840.usersinfo.exception.EncryptionIllegalArgumentException;
import io.github.maksim0840.usersinfo.exception.NotFoundException;
import io.github.maksim0840.usersinfo.mapper.ProtoDomainUserMapper;
import io.github.maksim0840.usersinfo.service.UserService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.List;

@GrpcService
public class UserGrpcEndpoint extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;

    public UserGrpcEndpoint(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void create(CreateUserRequest request, StreamObserver<CreateUserResponse> observerResponse) {
        String name = request.getName();
        String password = request.getPassword();
        UserRole role;
        try {
            role = ProtoUserRoleMapper.protoToDomain(request.getRole());
        } catch (IllegalArgumentException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
            return;
        }

        try {
            User user = userService.createUser(name, password, role);
            UserProto userProto = ProtoDomainUserMapper.domainToProto(user);
            CreateUserResponse response = CreateUserResponse.newBuilder()
                    .setUser(userProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (EncryptionIllegalArgumentException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
        } catch (EncryptionException e) {
            observerResponse.onError(error(Status.INTERNAL, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void get(GetUserRequest request, StreamObserver<GetUserResponse> observerResponse) {
        Long id = request.getId();

        try {
            User user = userService.getUserById(id);
            UserProto userProto = ProtoDomainUserMapper.domainToProto(user);
            GetUserResponse response = GetUserResponse.newBuilder()
                    .setUser(userProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void getList(GetListUserRequest request, StreamObserver<GetListUserResponse> observerResponse) {
        UserRole role;
        try {
            role = request.hasRole() ? ProtoUserRoleMapper.protoToDomain(request.getRole()) : null;
        } catch (IllegalArgumentException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
            return;
        }
        Instant createdFrom = request.hasCreatedFrom() ? ProtoTimeMapper.timestampToInstant(request.getCreatedFrom()) : null;
        Instant createdTo = request.hasCreatedTo() ? ProtoTimeMapper.timestampToInstant(request.getCreatedTo()) : null;
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        Boolean isSortDesc = request.hasSortCreatedDesc() ? request.getSortCreatedDesc() : null;

        try {
            List<User> users = userService.getListUserByPageWithFiltering(role, createdFrom, createdTo, pageNum, pageSize, isSortDesc);
            List<UserProto> usersProto = users.stream()
                    .map(ProtoDomainUserMapper::domainToProto)
                    .toList();
            GetListUserResponse response = GetListUserResponse.newBuilder()
                    .addAllUsers(usersProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void delete(DeleteUserRequest request, StreamObserver<DeleteUserResponse> observerResponse) {
        Long id = request.getId();

        try {
            userService.deleteUserById(id);
            DeleteUserResponse response = DeleteUserResponse.newBuilder().build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void setRole(SetUserRoleRequest request, StreamObserver<SetUserRoleResponse> observerResponse) {
        Long id = request.getId();
        UserRole role;
        try {
            role = ProtoUserRoleMapper.protoToDomain(request.getRole());
        } catch (IllegalArgumentException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
            return;
        }

        try {
            User user = userService.setUserRoleById(id, role);
            UserProto userProto = ProtoDomainUserMapper.domainToProto(user);
            SetUserRoleResponse response = SetUserRoleResponse.newBuilder()
                    .setUser(userProto).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    @Override
    public void checkPassword(CheckUserPasswordRequest request, StreamObserver<CheckUserPasswordResponse> observerResponse) {
        Long id = request.getId();
        String password = request.getPassword();

        try {
            boolean match = userService.checkUserPasswordById(id, password);
            CheckUserPasswordResponse response = CheckUserPasswordResponse.newBuilder()
                    .setMatch(match).build();

            observerResponse.onNext(response);
            observerResponse.onCompleted();
        } catch (EncryptionIllegalArgumentException e) {
            observerResponse.onError(error(Status.INVALID_ARGUMENT, e.getMessage()));
        } catch (EncryptionException e) {
            observerResponse.onError(error(Status.INTERNAL, e.getMessage()));
        } catch (NotFoundException e) {
            observerResponse.onError(error(Status.NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            observerResponse.onError(error(Status.UNAVAILABLE, e.getMessage()));
        }
    }

    private StatusRuntimeException error(Status status, String description) {
        return status.withDescription(description).asRuntimeException();
    }
}
