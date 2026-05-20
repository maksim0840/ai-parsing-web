package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.apigateway.dto.OrchestratorTaskResultDTO;
import io.github.maksim0840.apigateway.dto.OrchestratorTaskStatusDTO;
import io.github.maksim0840.apigateway.mapper.ProtoDTOOrchestratorTaskResultMapper;
import io.github.maksim0840.apigateway.mapper.ProtoDTOOrchestratorTaskStatusMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorStorageGrpcClient {

    @GrpcClient("parsing_orchestrator")
    OrchestratorStorageServiceGrpc.OrchestratorStorageServiceBlockingStub blockingStub;

    public OrchestratorTaskStatusDTO getTaskStatus(String taskId) {
        System.out.println("send getStatus");
        GetTaskStatusOrchestratorRequest request = GetTaskStatusOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .build();

        GetTaskStatusOrchestratorResponse response = blockingStub.getTaskStatus(request);
        System.out.println("receive getStatus");
        return ProtoDTOOrchestratorTaskStatusMapper.protoToDto(response);
    }

    public OrchestratorTaskResultDTO getTaskResult(String taskId) {
        System.out.println("send getResult");
        GetTaskResultOrchestratorRequest request = GetTaskResultOrchestratorRequest.newBuilder()
                .setTaskId(taskId)
                .build();

        try {
            GetTaskResultOrchestratorResponse response = blockingStub.getTaskResult(request);
            System.out.println("receive getResult");
            return ProtoDTOOrchestratorTaskResultMapper.protoToDto(response);
        } catch (StatusRuntimeException e) {
            throw GrpcExceptionMapper.map(e);
        }
    }
}
