package io.github.maksim0840.apigateway.service;

import io.github.maksim0840.apigateway.grpc.OrchestratorStorageGrpcClient;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TaskResultOrchestratorDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TaskStatusOrchestratorDTO;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final OrchestratorStorageGrpcClient storageGrpcClient;

    public TaskService(OrchestratorStorageGrpcClient storageGrpcClient) {
        this.storageGrpcClient = storageGrpcClient;
    }

    public TaskStatusOrchestratorDTO getStatus(String taskId, String userId) {
        return storageGrpcClient.getTaskStatus(taskId, userId);
    }

    public TaskResultOrchestratorDTO getResult(String taskId, String userId) {
        return storageGrpcClient.getTaskResult(taskId, userId);
    }
}
