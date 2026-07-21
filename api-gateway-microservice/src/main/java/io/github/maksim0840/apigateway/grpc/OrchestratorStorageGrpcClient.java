package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TaskResultOrchestratorDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TaskStatusOrchestratorDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TaskResultOrchestratorProtoMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TaskStatusOrchestratorProtoMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorStorageGrpcClient {

    @GrpcClient("parsing_orchestrator")
    OrchestratorStorageServiceGrpc.OrchestratorStorageServiceBlockingStub blockingStub;

    public TaskStatusOrchestratorDTO getTaskStatus(String taskId, String userId) {
        GetTaskStatusOrchestratorRequest request = GetTaskStatusOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(userId)
                .build();

        try {
            GetTaskStatusOrchestratorResponse response = blockingStub.getTaskStatus(request);
            return TaskStatusOrchestratorProtoMapper.protoToDto(response.getTaskStatusOrchestrator());
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }

    public TaskResultOrchestratorDTO getTaskResult(String taskId, String userId) {
        GetTaskResultOrchestratorRequest request = GetTaskResultOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .setUserId(userId)
                .build();

        try {
            GetTaskResultOrchestratorResponse response = blockingStub.getTaskResult(request);
            return TaskResultOrchestratorProtoMapper.protoToDto(response.getTaskResultOrchestrator());
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }
}
