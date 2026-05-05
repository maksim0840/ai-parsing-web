package io.github.maksim0840.apigateway.service;

import io.github.maksim0840.apigateway.dto.api.*;
import io.github.maksim0840.apigateway.grpc.OrchestratorStartGrpcClient;
import io.github.maksim0840.apigateway.mapper.ApiRequestsDTOMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrchestratorStartService {

    private final OrchestratorStartGrpcClient grpcClient;

    public OrchestratorStartService(OrchestratorStartGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public void sendPipelineRequest(String userId, PipelineApiRequest pipeline) {
        String taskId = userId;
        String htmlOutDir = taskId + "/html";
        String imagesOutDir = taskId + "/imgs";
        List<String> htmlPaths = List.of();
        List<String> imagePaths = List.of();
        HtmlParserRequestDTO htmlParserRequestDTO = ApiRequestsDTOMapper.parsingApiToDto(pipeline.parsing(), taskId, htmlOutDir, imagesOutDir);
        HtmlPreprocessingRequestDTO htmlPreprocessingRequestDTO = ApiRequestsDTOMapper.preprocessingApiToDto(pipeline.preprocessing(), taskId, htmlPaths);
        TextRecognitionRequestDTO textRecognitionRequestDTO = new TextRecognitionRequestDTO(taskId, imagePaths);
        LLMRequestDTO llmRequestDTO = ApiRequestsDTOMapper.llmApiToDto(pipeline.llm(), taskId);
        grpcClient.startParsing(
                taskId,
                htmlParserRequestDTO,
                htmlPreprocessingRequestDTO,
                textRecognitionRequestDTO,
                llmRequestDTO
        );
    }

    public void sendParsingRequest(String userId, ParsingApiRequest parsingRequest) {
        String taskId = userId;
        String htmlOutDir = taskId + "/html";
        String imagesOutDir = taskId + "/imgs";
        HtmlParserRequestDTO htmlParserRequestDTO = ApiRequestsDTOMapper.parsingApiToDto(parsingRequest, taskId, htmlOutDir, imagesOutDir);
        grpcClient.startParsing(
                taskId,
                htmlParserRequestDTO,
                null,
                null,
                null
        );
    }

    public void sendPreprocessingRequest(String userId, List<String> htmlPaths, PreprocessingApiRequest preprocessingRequest) {
        String taskId = userId;
        HtmlPreprocessingRequestDTO htmlPreprocessingRequestDTO = ApiRequestsDTOMapper.preprocessingApiToDto(preprocessingRequest, taskId, htmlPaths);
        grpcClient.startParsing(
                taskId,
                null,
                htmlPreprocessingRequestDTO,
                null,
                null
        );
    }

    public void sendRecognitionRequest(String userId, List<String> imagePaths, RecognitionApiRequest recognitionRequest) {
        String taskId = userId;
        TextRecognitionRequestDTO textRecognitionRequestDTO = ApiRequestsDTOMapper.recognitionApiToDto(recognitionRequest, taskId, imagePaths);
        grpcClient.startParsing(
                taskId,
                null,
                null,
                textRecognitionRequestDTO,
                null
        );
    }

    public void sendLLMRequest(String userId, LLMApiRequest llmRequest) {
        String taskId = userId;
        LLMRequestDTO llmRequestDTO = ApiRequestsDTOMapper.llmApiToDto(llmRequest, taskId);
        grpcClient.startParsing(
                taskId,
                null,
                null,
                null,
                llmRequestDTO
        );
    }
}
