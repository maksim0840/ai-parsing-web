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
import java.util.Map;

@Service
public class OrchestratorStartService {
    private final OrchestratorStartGrpcClient grpcClient;
    private final FileService fileService;

    public OrchestratorStartService(OrchestratorStartGrpcClient grpcClient, FileService fileService) {
        this.grpcClient = grpcClient;
        this.fileService = fileService;
    }

    public String sendPipelineRequest(String sessionId, PipelineApiRequest pipeline) {
        String htmlOutDir = fileService.getHtmlOutDir(sessionId);
        String imagesOutDir = fileService.getImagesOutDir(sessionId);

        HtmlParserRequestDTO htmlParserRequestDTO = isParsingNecessaryForPipeline(pipeline)
                ? ApiRequestsDTOMapper.parsingApiToDto(pipeline.parsing(), sessionId, htmlOutDir, imagesOutDir)
                : null;
        HtmlPreprocessingRequestDTO htmlPreprocessingRequestDTO = isPreprocessingNecessaryForPipeline(pipeline)
                ? ApiRequestsDTOMapper.preprocessingApiToDto(pipeline.preprocessing(), sessionId)
                : null;
        TextRecognitionRequestDTO textRecognitionRequestDTO = isRecognitionNecessaryForPipeline(pipeline)
                ? ApiRequestsDTOMapper.recognitionApiToDto(pipeline.recognition(), sessionId)
                : null;
        LLMRequestDTO llmRequestDTO = isLlmNecessaryForPipeline(pipeline)
                ? ApiRequestsDTOMapper.llmApiToDto(pipeline.llm(), sessionId)
                : null;

        return grpcClient.startParsing(
                sessionId,
                htmlParserRequestDTO,
                htmlPreprocessingRequestDTO,
                textRecognitionRequestDTO,
                llmRequestDTO
        );
    }

    public String sendParsingRequest(String sessionId, ParsingApiRequest parsingRequest) {
        String htmlOutDir = fileService.getHtmlOutDir(sessionId);
        String imagesOutDir = fileService.getImagesOutDir(sessionId);
        HtmlParserRequestDTO htmlParserRequestDTO = ApiRequestsDTOMapper.parsingApiToDto(parsingRequest, sessionId, htmlOutDir, imagesOutDir);

        return grpcClient.startParsing(
                sessionId,
                htmlParserRequestDTO,
                null,
                null,
                null
        );
    }

    public String sendPreprocessingRequest(String sessionId, PreprocessingApiRequest preprocessingRequest) {
        HtmlPreprocessingRequestDTO htmlPreprocessingRequestDTO = ApiRequestsDTOMapper.preprocessingApiToDto(preprocessingRequest, sessionId);

        return grpcClient.startParsing(
                sessionId,
                null,
                htmlPreprocessingRequestDTO,
                null,
                null
        );
    }

    public String sendRecognitionRequest(String sessionId, RecognitionApiRequest recognitionRequest) {
        TextRecognitionRequestDTO textRecognitionRequestDTO = ApiRequestsDTOMapper.recognitionApiToDto(recognitionRequest, sessionId);

        return grpcClient.startParsing(
                sessionId,
                null,
                null,
                textRecognitionRequestDTO,
                null
        );
    }

    public String sendLLMRequest(String sessionId, LLMApiRequest llmRequest) {
        LLMRequestDTO llmRequestDTO = ApiRequestsDTOMapper.llmApiToDto(llmRequest, sessionId);

        return grpcClient.startParsing(
                sessionId,
                null,
                null,
                null,
                llmRequestDTO
        );
    }

    private boolean isParsingNecessaryForPipeline(PipelineApiRequest pipelineRequest) {
        ParsingApiRequest parsingRequest = pipelineRequest.parsing();
        return parsingRequest.url() != null;
    }

    private boolean isPreprocessingNecessaryForPipeline(PipelineApiRequest pipelineRequest) {
        PreprocessingApiRequest preprocessingRequest = pipelineRequest.preprocessing();
        return preprocessingRequest.noscript()
                || preprocessingRequest.link()
                || preprocessingRequest.style()
                || preprocessingRequest.meta()
                || preprocessingRequest.script()
                || preprocessingRequest.canvas()
                || preprocessingRequest.svg()
                || preprocessingRequest.area()
                || preprocessingRequest.img()
                || preprocessingRequest.video()
                || preprocessingRequest.audio()
                || preprocessingRequest.iframe()
                || preprocessingRequest.portal()
                || preprocessingRequest.embed()
                || preprocessingRequest.object()
                || preprocessingRequest.source();
    }

    private boolean isRecognitionNecessaryForPipeline(PipelineApiRequest pipelineRequest) {
        ParsingApiRequest parsingRequest = pipelineRequest.parsing();
        return parsingRequest.downloadImages();
    }

    private boolean isLlmNecessaryForPipeline(PipelineApiRequest pipelineRequest) {
        return true;
    }
}