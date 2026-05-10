package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.apigateway.dto.OrchestratorStatusDTO;
import io.github.maksim0840.apigateway.mapper.ProtoDTOOrchestratorStatusMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStartServiceGrpc;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStatusRequest;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStatusResponse;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStatusServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorStatusGrpcClient {

    @GrpcClient("parsing_orchestrator_start")
    OrchestratorStatusServiceGrpc.OrchestratorStatusServiceBlockingStub blockingStub;

    public OrchestratorStatusDTO getStatus(String taskId) {
        System.out.println("send getStatus");
        OrchestratorStatusRequest request = OrchestratorStatusRequest.newBuilder()
                .setTaskId(taskId)
                .build();

        OrchestratorStatusResponse response = blockingStub.getStatus(request);
        System.out.println("receive getStatus");
        return ProtoDTOOrchestratorStatusMapper.protoToDto(response);
    }
}
