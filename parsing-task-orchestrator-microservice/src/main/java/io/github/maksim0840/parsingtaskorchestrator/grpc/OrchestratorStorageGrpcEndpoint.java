package io.github.maksim0840.parsingtaskorchestrator.grpc;

import com.openai.models.beta.threads.runs.Run;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TaskResultOrchestratorDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TaskStatusOrchestratorDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.*;
import io.github.maksim0840.parsing_task_orchestrator.v1.*;
import io.github.maksim0840.parsingtaskorchestrator.dto.StatusDTO;
import io.github.maksim0840.parsingtaskorchestrator.dto.TaskDTO;
import io.github.maksim0840.parsingtaskorchestrator.exception.TaskNotFoundException;
import io.github.maksim0840.parsingtaskorchestrator.service.OrchestratorService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class OrchestratorStorageGrpcEndpoint extends OrchestratorStorageServiceGrpc.OrchestratorStorageServiceImplBase {
    private final OrchestratorService orchestratorService;

    public OrchestratorStorageGrpcEndpoint(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Override
    public void getTaskStatus(GetTaskStatusOrchestratorRequest request, StreamObserver<GetTaskStatusOrchestratorResponse> responseObserver) {
        StatusDTO statusDTO;
        try {
            statusDTO = orchestratorService.getStatusInfo(request.getTaskId(), request.getUserId());
        } catch (RuntimeException e) {
            responseObserver.onError(error(Status.NOT_FOUND, e.getMessage()));
            return;
        }

        TaskStatusOrchestratorDTO taskStatusOrchestratorDTO = TaskStatusOrchestratorDTO.builder()
                .taskId(request.getTaskId())
                .status(statusDTO.status())
                .message(statusDTO.message())
                .build();
        GetTaskStatusOrchestratorResponse response = GetTaskStatusOrchestratorResponse.newBuilder()
                .setTaskStatusOrchestrator(TaskStatusOrchestratorProtoMapper.dtoToProto(taskStatusOrchestratorDTO))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTaskResult(GetTaskResultOrchestratorRequest request, StreamObserver<GetTaskResultOrchestratorResponse> responseObserver) {
        TaskDTO taskDTO;
        try {
            taskDTO = orchestratorService.getTask(request.getTaskId(), request.getUserId());
        } catch (TaskNotFoundException e) {
            responseObserver.onError(error(Status.NOT_FOUND, e.getMessage()));
            return;
        } catch (RuntimeException e) {
            responseObserver.onError(error(Status.UNAVAILABLE, e.getMessage()));
            return;
        }

        TaskResultOrchestratorDTO taskResultOrchestratorDTO = TaskResultOrchestratorDTO.builder()
                .taskId(request.getTaskId())
                .htmlParserResponse(taskDTO.htmlParserResponse())
                .htmlPreprocessingResponse(taskDTO.htmlPreprocessingResponse())
                .textRecognitionResponse(taskDTO.textRecognitionResponse())
                .llmResponse(taskDTO.llmResponse())
                .build();
        GetTaskResultOrchestratorResponse response = GetTaskResultOrchestratorResponse.newBuilder()
                .setTaskResultOrchestrator(TaskResultOrchestratorProtoMapper.dtoToProto(taskResultOrchestratorDTO))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private StatusRuntimeException error(Status status, String description) {
        return status.withDescription(description).asRuntimeException();
    }
}
