package io.github.maksim0840.parsingtaskorchestrator.grpc;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TaskStatusMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStatusRequest;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStatusResponse;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStatusServiceGrpc;
import io.github.maksim0840.parsingtaskorchestrator.service.OrchestratorService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class OrchestratorStatusGrpcEndpoint extends OrchestratorStatusServiceGrpc.OrchestratorStatusServiceImplBase {
    private final OrchestratorService orchestratorService;

    public OrchestratorStatusGrpcEndpoint(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Override
    public void getStatus(OrchestratorStatusRequest request, StreamObserver<OrchestratorStatusResponse> responseObserver) {
        System.out.println("getStatus");
        TaskStatus taskStatus = orchestratorService.getStatus(request.getTaskId());
        String message = orchestratorService.getMessage(request.getTaskId());
        System.out.println(taskStatus);

        OrchestratorStatusResponse response = OrchestratorStatusResponse.newBuilder()
                .setTaskId(request.getTaskId())
                .setStatus(TaskStatusMapper.enumToProto(taskStatus))
                .setMessage(message)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
