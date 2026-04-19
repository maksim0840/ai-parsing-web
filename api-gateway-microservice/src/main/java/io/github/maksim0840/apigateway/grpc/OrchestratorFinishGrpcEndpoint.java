package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TextRecognitionResponseMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishRequest;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishResponse;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;

@GrpcService
public class OrchestratorFinishGrpcEndpoint extends OrchestratorFinishServiceGrpc.OrchestratorFinishServiceImplBase {

    @Override
    public void finishParsing(OrchestratorFinishRequest request, StreamObserver<OrchestratorFinishResponse> responseObserver) {
        String taskId = request.getTaskId();
        HtmlParserResponseDTO htmlParserResponse =
                request.hasHtmlParserResponse()
                    ? HtmlParserResponseMapper.protoToDto(request.getHtmlParserResponse())
                    : null;
        HtmlPreprocessingResponseDTO htmlPreprocessingResponse =
                request.hasHtmlPreprocessingResponse()
                        ? HtmlPreprocessingResponseMapper.protoToDto(request.getHtmlPreprocessingResponse())
                        : null;
        TextRecognitionResponseDTO textRecognitionResponseDTO =
                request.hasTextRecognitionResponse()
                        ? TextRecognitionResponseMapper.protoToDto(request.getTextRecognitionResponse())
                        : null;

        OrchestratorFinishResponse response = OrchestratorFinishResponse.newBuilder()
                .setTaskId(taskId).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
