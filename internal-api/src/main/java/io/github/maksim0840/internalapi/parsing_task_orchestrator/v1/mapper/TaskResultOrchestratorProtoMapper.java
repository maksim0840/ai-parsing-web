package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.parsing_task_orchestrator.v1.LLMRequestProto;
import io.github.maksim0840.parsing_task_orchestrator.v1.LLMResponseProto;
import io.github.maksim0840.parsing_task_orchestrator.v1.TaskResultOrchestratorProto;

public class TaskResultOrchestratorProtoMapper {

    public static TaskResultOrchestratorProto dtoToProto(TaskResultOrchestratorDTO dto) {
        TaskResultOrchestratorProto.Builder protoBuilder = TaskResultOrchestratorProto.newBuilder();
        protoBuilder.setTaskId(dto.taskId() != null ? dto.taskId() : "");
        if (dto.htmlParserResponse() != null) protoBuilder.setHtmlParserResponse(HtmlParserResponseProtoMapper.dtoToProto(dto.htmlParserResponse()));
        if (dto.htmlPreprocessingResponse() != null) protoBuilder.setHtmlPreprocessingResponse(HtmlPreprocessingResponseProtoMapper.dtoToProto(dto.htmlPreprocessingResponse()));
        if (dto.textRecognitionResponse() != null) protoBuilder.setTextRecognitionResponse(TextRecognitionResponseProtoMapper.dtoToProto(dto.textRecognitionResponse()));
        if (dto.llmResponse() != null) protoBuilder.setLlmResponse(LLMResponseProtoMapper.dtoToProto(dto.llmResponse()));
        return protoBuilder.build();
    }

    public static TaskResultOrchestratorDTO protoToDto(TaskResultOrchestratorProto proto) {
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
        return TaskResultOrchestratorDTO.builder()
                .taskId(proto.getTaskId())
                .htmlParserResponse(htmlParserResponse)
                .htmlPreprocessingResponse(htmlPreprocessingResponse)
                .textRecognitionResponse(textRecognitionResponseDTO)
                .llmResponse(llmResponseDTO)
                .build();
    }
}
