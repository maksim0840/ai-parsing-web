package io.github.maksim0840.parsingtaskorchestrator.grpc;

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
        System.out.println("getTaskStatus");
        StatusDTO statusDTO = orchestratorService.getStatusInfo(request.getTaskId());
        System.out.println(statusDTO);

        GetTaskStatusOrchestratorResponse response = GetTaskStatusOrchestratorResponse.newBuilder()
                .setTaskId(request.getTaskId())
                .setStatus(TaskStatusProtoMapper.enumToProto(statusDTO.status()))
                .setMessage(statusDTO.message() == null ? "" : statusDTO.message())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTaskResult(GetTaskResultOrchestratorRequest request, StreamObserver<GetTaskResultOrchestratorResponse> responseObserver) {
        System.out.println("getTaskResult");
        TaskDTO taskDTO;
        try {
            taskDTO = orchestratorService.getTask(request.getTaskId());
        } catch (TaskNotFoundException e) {
            responseObserver.onError(error(Status.NOT_FOUND, e.getMessage()));
            return;
        }

        GetTaskResultOrchestratorResponse.Builder responseBuilder = GetTaskResultOrchestratorResponse.newBuilder();
        responseBuilder.setTaskId(request.getTaskId());
        if (taskDTO.htmlParserResponse() != null) {
            responseBuilder.setHtmlParserResponse(HtmlParserResponseProtoMapper.dtoToProto(taskDTO.htmlParserResponse()));
        }
        if (taskDTO.htmlPreprocessingResponse() != null) {
            responseBuilder.setHtmlPreprocessingResponse(HtmlPreprocessingResponseProtoMapper.dtoToProto(taskDTO.htmlPreprocessingResponse()));
        }
        if (taskDTO.textRecognitionResponse() != null) {
            responseBuilder.setTextRecognitionResponse(TextRecognitionResponseProtoMapper.dtoToProto(taskDTO.textRecognitionResponse()));
        }
        if (taskDTO.llmResponse() != null) {
            responseBuilder.setLlmResponse(LLMResponseProtoMapper.dtoToProto(taskDTO.llmResponse()));
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private StatusRuntimeException error(Status status, String description) {
        return status.withDescription(description).asRuntimeException();
    }
}
