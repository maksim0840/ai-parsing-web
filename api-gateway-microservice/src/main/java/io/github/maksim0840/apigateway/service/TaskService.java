package io.github.maksim0840.apigateway.service;

import io.github.maksim0840.apigateway.dto.OrchestratorFinishDTO;
import io.github.maksim0840.apigateway.dto.OrchestratorStatusDTO;
import io.github.maksim0840.apigateway.grpc.OrchestratorStatusGrpcClient;
import io.github.maksim0840.apigateway.storage.TaskStorage;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final OrchestratorStatusGrpcClient statusGrpcClient;
    private final TaskStorage storage;

    public TaskService(OrchestratorStatusGrpcClient statusGrpcClient, TaskStorage storage) {
        this.statusGrpcClient = statusGrpcClient;
        this.storage = storage;
    }

    public OrchestratorStatusDTO getStatus(String taskId) {
        return statusGrpcClient.getStatus(taskId);
    }

    public OrchestratorFinishDTO getResult(String taskId) {
        return storage.getResult(taskId);
    }

    public void addResult(String taskId, OrchestratorFinishDTO result) {
        storage.addResult(taskId, result);
    }
}
