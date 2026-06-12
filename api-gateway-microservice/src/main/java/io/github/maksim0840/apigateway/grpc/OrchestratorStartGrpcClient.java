package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserRequestProtoMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingRequestProtoMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.LLMRequestProtoMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TextRecognitionRequestProtoMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorStartGrpcClient {

    @GrpcClient("parsing_orchestrator")
    private OrchestratorStartServiceGrpc.OrchestratorStartServiceBlockingStub blockingStub;

    public String startParsing(
            String taskId,
            HtmlParserRequestDTO htmlParserRequestDTO,
            HtmlPreprocessingRequestDTO htmlPreprocessingRequestDTO,
            TextRecognitionRequestDTO textRecognitionRequestDTO,
            LLMRequestDTO llmRequestDTO) {

        System.out.println("startParsing");
        StartParsingOrchestratorRequest.Builder requestBuilder = StartParsingOrchestratorRequest.newBuilder();
        requestBuilder.setTaskId(taskId);
        if (htmlParserRequestDTO != null) {
            requestBuilder.setHtmlParserRequest(HtmlParserRequestProtoMapper.dtoToProto(htmlParserRequestDTO));
        }
        if (htmlPreprocessingRequestDTO != null) {
            requestBuilder.setHtmlPreprocessingRequest(HtmlPreprocessingRequestProtoMapper.dtoToProto(htmlPreprocessingRequestDTO));
        }
        if (textRecognitionRequestDTO != null) {
            requestBuilder.setTextRecognitionRequest(TextRecognitionRequestProtoMapper.dtoToProto(textRecognitionRequestDTO));
        }
        if (llmRequestDTO != null) {
            requestBuilder.setLlmRequest(LLMRequestProtoMapper.dtoToProto(llmRequestDTO));
        }

        StartParsingOrchestratorResponse response = blockingStub.startParsing(requestBuilder.build());
        return response.getTaskId();
    }
}
