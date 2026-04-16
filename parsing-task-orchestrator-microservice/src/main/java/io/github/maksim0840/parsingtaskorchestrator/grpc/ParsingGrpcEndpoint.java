package io.github.maksim0840.parsingtaskorchestrator.grpc;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TextRecognitionRequestMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.*;
import io.github.maksim0840.parsingtaskorchestrator.service.OrchestratorService;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
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
        orchestratorService.startRequestsPipeline(taskId, htmlParserRequest, htmlPreprocessingRequest, textRecognitionRequest);
    }
}
