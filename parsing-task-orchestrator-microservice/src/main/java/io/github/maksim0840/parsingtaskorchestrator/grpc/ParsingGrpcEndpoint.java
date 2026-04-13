package io.github.maksim0840.parsingtaskorchestrator.grpc;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.ProtoHtmlParserRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.ProtoHtmlPreprocessingRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.ProtoTextRecognitionRequestMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.*;
import io.github.maksim0840.parsingtaskorchestrator.service.OrchestratorService;
import io.grpc.stub.StreamObserver;

public class ParsingGrpcEndpoint extends ParsingServiceGrpc.ParsingServiceImplBase {
    private final OrchestratorService orchestratorService;

    public ParsingGrpcEndpoint(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Override
    public void start(ParsingRequest request, StreamObserver<ParsingResponse> responseObserver) {
        String taskId = request.getTaskId();
        HtmlParserRequestDTO htmlParserRequest =
                request.hasHtmlParserRequest()
                    ? ProtoHtmlParserRequestMapper.protoToDto(request.getHtmlParserRequest())
                    : null;
        HtmlPreprocessingRequestDTO htmlPreprocessingRequest =
                request.hasHtmlPreprocessingRequest()
                        ? ProtoHtmlPreprocessingRequestMapper.protoToDto(request.getHtmlPreprocessingRequest())
                        : null;
        TextRecognitionRequestDTO textRecognitionRequest =
                request.hasTextRecognitionRequest()
                        ? ProtoTextRecognitionRequestMapper.protoToDto(request.getTextRecognitionRequest())
                        : null;
        orchestratorService.distributeRequests(taskId, htmlParserRequest, htmlPreprocessingRequest, textRecognitionRequest);
    }
}
