package io.github.maksim0840.apigateway.mapper;

import io.github.maksim0840.apigateway.dto.api.LLMApiRequest;
import io.github.maksim0840.apigateway.dto.api.ParsingApiRequest;
import io.github.maksim0840.apigateway.dto.api.PreprocessingApiRequest;
import io.github.maksim0840.apigateway.dto.api.RecognitionApiRequest;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;

import java.util.List;
import java.util.Map;

public class ApiRequestsDTOMapper {

    public static HtmlParserRequestDTO parsingApiToDto(ParsingApiRequest apiRequest, String taskId, String htmlOutDir, String imagesOutDir) {
        return HtmlParserRequestDTO.builder()
                .taskId(taskId)
                .url(apiRequest.url())
                .htmlOutDir(htmlOutDir)
                .imagesOutDir(imagesOutDir)
                .downloadImages(apiRequest.downloadImages())
                .headers(apiRequest.headers())
                .cookies(apiRequest.cookies())
                .proxy(apiRequest.proxy())
                .pageComplexity(apiRequest.pageComplexity())
                .additionalPageLoadTimeoutS(apiRequest.additionalPageLoadTimeoutS())
                .build();
    }

    public static HtmlPreprocessingRequestDTO preprocessingApiToDto(PreprocessingApiRequest apiRequest, String taskId) {
        return HtmlPreprocessingRequestDTO.builder()
                .taskId(taskId)
                .htmlDocs(apiRequest.htmlDocs())
                .noscriptProcessing(apiRequest.noscript())
                .linkProcessing(apiRequest.link())
                .styleProcessing(apiRequest.style())
                .metaProcessing(apiRequest.meta())
                .scriptProcessing(apiRequest.script())
                .canvasProcessing(apiRequest.canvas())
                .svgProcessing(apiRequest.svg())
                .areaProcessing(apiRequest.area())
                .imgProcessing(apiRequest.img())
                .videoProcessing(apiRequest.video())
                .audioProcessing(apiRequest.audio())
                .iframeProcessing(apiRequest.iframe())
                .portalProcessing(apiRequest.portal())
                .embedProcessing(apiRequest.embed())
                .objectProcessing(apiRequest.object())
                .sourceProcessing(apiRequest.source())
                .build();
    }

    public static TextRecognitionRequestDTO recognitionApiToDto(RecognitionApiRequest apiRequest, String taskId) {
        return TextRecognitionRequestDTO.builder()
                .taskId(taskId)
                .images(apiRequest.images())
                .build();
    }

    public static LLMRequestDTO llmApiToDto(LLMApiRequest apiRequest, String taskId) {
        return LLMRequestDTO.builder()
                .taskId(taskId)
                .modelName(apiRequest.modelName())
                .systemMessage(apiRequest.systemMessage())
                .userMessage(apiRequest.userMessage())
                .temperature(apiRequest.temperature())
                .maxOutputTokens(apiRequest.maxOutputTokens())
                .htmlDocs(apiRequest.htmlDocs())
                .images(apiRequest.images())
                .build();
    }
}
