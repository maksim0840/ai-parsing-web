package io.github.maksim0840.parsingtaskorchestrator.grpc;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.LLMResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TextRecognitionResponseMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishRequest;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishResponse;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorFinishGrpcClient {

    @GrpcClient("parsing_orchestrator_finish")
    private OrchestratorFinishServiceGrpc.OrchestratorFinishServiceBlockingStub blockingStub;

    public String finishParsing(
            String taskId,
            HtmlParserResponseDTO htmlParserResponseDTO,
            HtmlPreprocessingResponseDTO htmlPreprocessingResponseDTO,
            TextRecognitionResponseDTO textRecognitionResponseDTO,
            LLMResponseDTO llmResponseDTO) {

        OrchestratorFinishRequest.Builder request = OrchestratorFinishRequest.newBuilder();
        request.setTaskId(taskId);
        if (htmlParserResponseDTO != null) request.setHtmlParserResponse(HtmlParserResponseMapper.dtoToProto(htmlParserResponseDTO));
        if (htmlPreprocessingResponseDTO != null) request.setHtmlPreprocessingResponse(HtmlPreprocessingResponseMapper.dtoToProto(htmlPreprocessingResponseDTO));
        if (textRecognitionResponseDTO != null) request.setTextRecognitionResponse(TextRecognitionResponseMapper.dtoToProto(textRecognitionResponseDTO));
        if (llmResponseDTO != null) request.setLlmResponse(LLMResponseMapper.dtoToProto(llmResponseDTO));

        OrchestratorFinishResponse response = blockingStub.finishParsing(request.build());
        return response.getTaskId();
    }
}
