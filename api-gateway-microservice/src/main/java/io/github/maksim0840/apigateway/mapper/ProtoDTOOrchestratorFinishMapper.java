package io.github.maksim0840.apigateway.mapper;

import io.github.maksim0840.apigateway.dto.OrchestratorFinishDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.LLMResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TextRecognitionResponseMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishRequest;

public class ProtoDTOOrchestratorFinishMapper {
    public static OrchestratorFinishDTO protoToDto(OrchestratorFinishRequest proto) {
        String taskId = proto.getTaskId();
        HtmlParserResponseDTO htmlParserResponse =
                proto.hasHtmlParserResponse()
                        ? HtmlParserResponseMapper.protoToDto(proto.getHtmlParserResponse())
                        : null;
        HtmlPreprocessingResponseDTO htmlPreprocessingResponse =
                proto.hasHtmlPreprocessingResponse()
                        ? HtmlPreprocessingResponseMapper.protoToDto(proto.getHtmlPreprocessingResponse())
                        : null;
        TextRecognitionResponseDTO textRecognitionResponseDTO =
                proto.hasTextRecognitionResponse()
                        ? TextRecognitionResponseMapper.protoToDto(proto.getTextRecognitionResponse())
                        : null;
        LLMResponseDTO llmResponseDTO =
                proto.hasLlmResponse()
                        ? LLMResponseMapper.protoToDto(proto.getLlmResponse())
                        : null;
        return OrchestratorFinishDTO.builder()
                .taskId(proto.getTaskId())
                .htmlParserResponse(htmlParserResponse)
                .htmlPreprocessingResponse(htmlPreprocessingResponse)
                .textRecognitionResponse(textRecognitionResponseDTO)
                .llmResponse(llmResponseDTO)
                .build();
    }
}

//
//String taskId,
//HtmlParserResponseDTO htmlParserResponse,
//HtmlPreprocessingResponseDTO htmlPreprocessingResponse,
//TextRecognitionResponseDTO textRecognitionResponse,
//LLMResponseDTO llmResponse