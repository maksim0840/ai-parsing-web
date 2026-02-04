package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.apigateway.dto.UserDTO;
import io.github.maksim0840.apigateway.mapper.ProtoDTOUserMapper;
import io.github.maksim0840.internalapi.common.v1.mapper.ProtoTimeMapper;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import io.github.maksim0840.internalapi.user.v1.mapper.ProtoUserRoleMapper;
import io.github.maksim0840.parsing_param.v1.GetListParsingParamRequest;
import io.github.maksim0840.user.v1.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserGrpcClient {

    @GrpcClient("usersInfo")
    private UserServiceGrpc.UserServiceBlockingStub blockingStub;

    public UserDTO create(String name, String password, UserRole role) {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName(name)
                .setPassword(password)
                .setRole(ProtoUserRoleMapper.domainToProto(role))
                .build();

        try {
            CreateUserResponse response = blockingStub.create(request);
            UserProto userProto = response.getUser();
            return ProtoDTOUserMapper.protoToDto(userProto);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public UserDTO get(Long id) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId(id)
                .build();

        try {
            GetUserResponse response = blockingStub.get(request);
            UserProto userProto = response.getUser();
            return ProtoDTOUserMapper.protoToDto(userProto);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }

    }

    public List<UserDTO> getList(UserRole role, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
        GetListUserRequest.Builder requestBuilder = GetListUserRequest.newBuilder()
                .setPageNum(pageNum)
                .setPageSize(pageSize);
        if (role != null) requestBuilder.setRole(ProtoUserRoleMapper.domainToProto(role));
        if (dateFrom != null) requestBuilder.setCreatedFrom(ProtoTimeMapper.instantToTimestamp(dateFrom));
        if (dateTo != null) requestBuilder.setCreatedTo(ProtoTimeMapper.instantToTimestamp(dateTo));
        if (isSortDesc != null) requestBuilder.setSortCreatedDesc(isSortDesc);
        GetListUserRequest request = requestBuilder.build();

        try {
            GetListUserResponse response = blockingStub.getList(request);
            List<UserProto> usersProto = response.getUsersList();
            return usersProto.stream()
                    .map(ProtoDTOUserMapper::protoToDto)
                    .toList();
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public void delete(Long id) {
        DeleteUserRequest request = DeleteUserRequest.newBuilder()
                .setId(id)
                .build();

        try {
            DeleteUserResponse response = blockingStub.delete(request);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public UserDTO setRole(Long id, UserRole role) {
        SetUserRoleRequest request = SetUserRoleRequest.newBuilder()
                .setId(id)
                .setRole(ProtoUserRoleMapper.domainToProto(role))
                .build();

        try {
            SetUserRoleResponse response = blockingStub.setRole(request);
            UserProto userProto = response.getUser();
            return ProtoDTOUserMapper.protoToDto(userProto);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public boolean checkPassword(Long id, String password) {
        CheckUserPasswordRequest request = CheckUserPasswordRequest.newBuilder()
                .setId(id)
                .setPassword(password)
                .build();

        try {
            CheckUserPasswordResponse response = blockingStub.checkPassword(request);
            return response.getMatch();
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }
}
