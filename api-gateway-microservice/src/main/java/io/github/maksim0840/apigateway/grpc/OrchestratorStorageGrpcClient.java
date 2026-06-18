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

    public TaskStatusOrchestratorDTO getTaskStatus(String taskId) {
        System.out.println("send getStatus");
        GetTaskStatusOrchestratorRequest request = GetTaskStatusOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .build();

        GetTaskStatusOrchestratorResponse response = blockingStub.getTaskStatus(request);
        System.out.println("receive getStatus");
        return TaskStatusOrchestratorProtoMapper.protoToDto(response.getTaskStatusOrchestrator());
    }

    public TaskResultOrchestratorDTO getTaskResult(String taskId) {
        System.out.println("send getResult");
        GetTaskResultOrchestratorRequest request = GetTaskResultOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .build();

        try {
            GetTaskResultOrchestratorResponse response = blockingStub.getTaskResult(request);
            System.out.println("receive getResult");
            return TaskResultOrchestratorProtoMapper.protoToDto(response.getTaskResultOrchestrator());
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }
}
