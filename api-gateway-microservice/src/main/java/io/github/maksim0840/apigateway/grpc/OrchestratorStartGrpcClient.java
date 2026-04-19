package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TextRecognitionRequestMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStartRequest;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStartResponse;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStartServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorStartGrpcClient {

    @GrpcClient("parsing_orchestrator_start")
    private OrchestratorStartServiceGrpc.OrchestratorStartServiceBlockingStub blockingStub;

    public String startParsing(
            String taskId,
            HtmlParserRequestDTO htmlParserRequestDTO,
            HtmlPreprocessingRequestDTO htmlPreprocessingRequestDTO,
            TextRecognitionRequestDTO textRecognitionRequestDTO) {

        OrchestratorStartRequest request = OrchestratorStartRequest.newBuilder()
                .setTaskId(taskId)
                .setHtmlParserRequest(HtmlParserRequestMapper.dtoToProto(htmlParserRequestDTO))
                .setHtmlPreprocessingRequest(HtmlPreprocessingRequestMapper.dtoToProto(htmlPreprocessingRequestDTO))
                .setTextRecognitionRequest(TextRecognitionRequestMapper.dtoToProto(textRecognitionRequestDTO))
                .build();

        OrchestratorStartResponse response = blockingStub.startParsing(request);
        return taskId;

    }
}
