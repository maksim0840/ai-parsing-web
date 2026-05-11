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

    public void sendPipelineRequest(String sessionId, PipelineApiRequest pipeline) {
        String htmlOutDir = fileService.getHtmlOutDir(sessionId);
        String imagesOutDir = fileService.getImagesOutDir(sessionId);
        List<String> htmlPaths = fileService.getObjectKeysByPrefix(htmlOutDir);
        List<String> imagePaths = fileService.getObjectKeysByPrefix(imagesOutDir);
        Map<String, String> textByImage = Map.of();
        HtmlParserRequestDTO htmlParserRequestDTO = isParsingNecessaryForPipeline(pipeline)
                ? ApiRequestsDTOMapper.parsingApiToDto(pipeline.parsing(), sessionId, htmlOutDir, imagesOutDir)
                : null;
        HtmlPreprocessingRequestDTO htmlPreprocessingRequestDTO = isPreprocessingNecessaryForPipeline(pipeline)
                ? ApiRequestsDTOMapper.preprocessingApiToDto(pipeline.preprocessing(), sessionId, htmlPaths)
                : null;
        TextRecognitionRequestDTO textRecognitionRequestDTO = isRecognitionNecessaryForPipeline(pipeline)
                ? new TextRecognitionRequestDTO(sessionId, imagePaths)
                : null;
        LLMRequestDTO llmRequestDTO = ApiRequestsDTOMapper.llmApiToDto(pipeline.llm(), sessionId, htmlPaths, textByImage);
        grpcClient.startParsing(
                sessionId,
                htmlParserRequestDTO,
                htmlPreprocessingRequestDTO,
                textRecognitionRequestDTO,
                llmRequestDTO
        );
    }

    public void sendParsingRequest(String sessionId, ParsingApiRequest parsingRequest) {
        String htmlOutDir = fileService.getHtmlOutDir(sessionId);
        String imagesOutDir = fileService.getImagesOutDir(sessionId);
        HtmlParserRequestDTO htmlParserRequestDTO = ApiRequestsDTOMapper.parsingApiToDto(parsingRequest, sessionId, htmlOutDir, imagesOutDir);
        grpcClient.startParsing(
                sessionId,
                htmlParserRequestDTO,
                null,
                null,
                null
        );
    }

    public void sendPreprocessingRequest(String sessionId, PreprocessingApiRequest preprocessingRequest) {
        String htmlOutDir = fileService.getHtmlOutDir(sessionId);
        List<String> htmlPaths = fileService.getObjectKeysByPrefix(htmlOutDir);
        HtmlPreprocessingRequestDTO htmlPreprocessingRequestDTO = ApiRequestsDTOMapper.preprocessingApiToDto(preprocessingRequest, sessionId, htmlPaths);
        grpcClient.startParsing(
                sessionId,
                null,
                htmlPreprocessingRequestDTO,
                null,
                null
        );
    }

    public void sendRecognitionRequest(String sessionId, RecognitionApiRequest recognitionRequest) {
        String imagesOutDir = fileService.getImagesOutDir(sessionId);
        List<String> imagePaths = fileService.getObjectKeysByPrefix(imagesOutDir);
        TextRecognitionRequestDTO textRecognitionRequestDTO = ApiRequestsDTOMapper.recognitionApiToDto(recognitionRequest, sessionId, imagePaths);
        grpcClient.startParsing(
                sessionId,
                null,
                null,
                textRecognitionRequestDTO,
                null
        );
    }

    public void sendLLMRequest(String sessionId, Map<String, String> textByImage, LLMApiRequest llmRequest) {
        String imagesOutDir = fileService.getImagesOutDir(sessionId);
        List<String> imagePaths = fileService.getObjectKeysByPrefix(imagesOutDir);
        LLMRequestDTO llmRequestDTO = ApiRequestsDTOMapper.llmApiToDto(llmRequest, sessionId, imagePaths, textByImage);
        grpcClient.startParsing(
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
}