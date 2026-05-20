package io.github.maksim0840.parsingtaskorchestrator.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.LLMRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TextRecognitionRequestMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.*;
import io.github.maksim0840.parsingtaskorchestrator.dto.TaskDTO;
import io.github.maksim0840.parsingtaskorchestrator.service.OrchestratorService;
import io.github.maksim0840.parsingtaskorchestrator.service.TaskService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;

@GrpcService
public class OrchestratorStartGrpcEndpoint extends OrchestratorStartServiceGrpc.OrchestratorStartServiceImplBase {
    private final OrchestratorService orchestratorService;
    private final TaskService taskService;

    public OrchestratorStartGrpcEndpoint(OrchestratorService orchestratorService, TaskService taskService) {
        this.orchestratorService = orchestratorService;
        this.taskService = taskService;
    }

    @Override
    public void startParsing(StartParsingOrchestratorRequest request, StreamObserver<StartParsingOrchestratorResponse> responseObserver) {
        String taskId = request.getTaskId();
        HtmlParserRequestDTO htmlParserRequest =
                request.hasHtmlParserRequest()
                    ? HtmlParserRequestMapper.protoToDto(request.getHtmlParserRequest())
                    : null;
        HtmlPreprocessingRequestDTO htmlPreprocessingRequest =
                request.hasHtmlPreprocessingRequest()
                        ? HtmlPreprocessingRequestMapper.protoToDto(request.getHtmlPreprocessingRequest())
                        : null;
        TextRecognitionRequestDTO textRecognitionRequest =
                request.hasTextRecognitionRequest()
                        ? TextRecognitionRequestMapper.protoToDto(request.getTextRecognitionRequest())
                        : null;
        LLMRequestDTO llmRequestDTO =
                request.hasLlmRequest()
                        ? LLMRequestMapper.protoToDto(request.getLlmRequest())
                        : null;

        TaskDTO task = taskService.addTask(taskId, htmlParserRequest, htmlPreprocessingRequest, textRecognitionRequest, llmRequestDTO);

        StartParsingOrchestratorResponse response = StartParsingOrchestratorResponse.newBuilder()
                .setTaskId(task.id()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        try {
            orchestratorService.startRequestsPipeline(task);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
