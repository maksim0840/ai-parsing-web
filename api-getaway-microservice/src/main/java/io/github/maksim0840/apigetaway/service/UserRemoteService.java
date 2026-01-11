package io.github.maksim0840.apigetaway.service;

import io.github.maksim0840.apigetaway.dto.UserDTO;
import io.github.maksim0840.apigetaway.grpc.UserGrpcClient;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserRemoteService {

    private final UserGrpcClient grpcClient;

    public UserRemoteService(UserGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public UserDTO createUser(String name, String password, UserRole role) {
        return grpcClient.create(name, password, role);
    }

    public UserDTO getUserById(Long id) {
        return grpcClient.get(id);
    }

    public List<UserDTO> getListUserByPageWithFiltering(UserRole role, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
        return grpcClient.getList(role, dateFrom, dateTo, pageNum, pageSize, isSortDesc);
    }

    public void deleteUserById(Long id) {
        grpcClient.delete(id);
    }

    public UserDTO setUserRoleById(Long id, UserRole role) {
        return grpcClient.setRole(id, role);
    }

    public boolean checkUserPasswordById(Long id, String password) {
        return grpcClient.checkPassword(id, password);
    }
}
