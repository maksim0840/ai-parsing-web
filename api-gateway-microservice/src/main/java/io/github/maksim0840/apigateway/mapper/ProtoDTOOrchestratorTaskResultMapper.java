package io.github.maksim0840.apigateway.mapper;

import io.github.maksim0840.apigateway.dto.OrchestratorTaskResultDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserResponseProtoMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingResponseProtoMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.LLMResponseProtoMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TextRecognitionResponseProtoMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.GetTaskResultOrchestratorResponse;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishRequest;

public class ProtoDTOOrchestratorTaskResultMapper {
    public static OrchestratorTaskResultDTO protoToDto(GetTaskResultOrchestratorResponse proto) {
        String taskId = proto.getTaskId();
        HtmlParserResponseDTO htmlParserResponse =
                proto.hasHtmlParserResponse()
                        ? HtmlParserResponseProtoMapper.protoToDto(proto.getHtmlParserResponse())
                        : null;
        HtmlPreprocessingResponseDTO htmlPreprocessingResponse =
                proto.hasHtmlPreprocessingResponse()
                        ? HtmlPreprocessingResponseProtoMapper.protoToDto(proto.getHtmlPreprocessingResponse())
                        : null;
        TextRecognitionResponseDTO textRecognitionResponseDTO =
                proto.hasTextRecognitionResponse()
                        ? TextRecognitionResponseProtoMapper.protoToDto(proto.getTextRecognitionResponse())
                        : null;
        LLMResponseDTO llmResponseDTO =
                proto.hasLlmResponse()
                        ? LLMResponseProtoMapper.protoToDto(proto.getLlmResponse())
                        : null;
        return OrchestratorTaskResultDTO.builder()
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