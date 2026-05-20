package io.github.maksim0840.apigateway.service;

import io.github.maksim0840.apigateway.dto.OrchestratorTaskResultDTO;
import io.github.maksim0840.apigateway.dto.OrchestratorTaskStatusDTO;
import io.github.maksim0840.apigateway.grpc.OrchestratorStorageGrpcClient;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final OrchestratorStorageGrpcClient storageGrpcClient;

    public TaskService(OrchestratorStorageGrpcClient storageGrpcClient) {
        this.storageGrpcClient = storageGrpcClient;
    }

    public OrchestratorTaskStatusDTO getStatus(String taskId) {
        return storageGrpcClient.getTaskStatus(taskId);
    }

    public OrchestratorTaskResultDTO getResult(String taskId) {
        return storageGrpcClient.getTaskResult(taskId);
    }
}
