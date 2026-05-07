package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingRequestMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.LLMRequestMapper;
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
            TextRecognitionRequestDTO textRecognitionRequestDTO,
            LLMRequestDTO llmRequestDTO) {

        System.out.println("startParsing");
        OrchestratorStartRequest.Builder requestBuilder = OrchestratorStartRequest.newBuilder();
        requestBuilder.setTaskId(taskId);
        if (htmlParserRequestDTO != null) {
            requestBuilder.setHtmlParserRequest(HtmlParserRequestMapper.dtoToProto(htmlParserRequestDTO));
        }
        if (htmlPreprocessingRequestDTO != null) {
            requestBuilder.setHtmlPreprocessingRequest(HtmlPreprocessingRequestMapper.dtoToProto(htmlPreprocessingRequestDTO));
        }
        if (textRecognitionRequestDTO != null) {
            requestBuilder.setTextRecognitionRequest(TextRecognitionRequestMapper.dtoToProto(textRecognitionRequestDTO));
        }
        if (llmRequestDTO != null) {
            requestBuilder.setLlmRequest(LLMRequestMapper.dtoToProto(llmRequestDTO));
        }

        OrchestratorStartResponse response = blockingStub.startParsing(requestBuilder.build());
        return taskId;
    }
}
