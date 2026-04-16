package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingResponseDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.HtmlPreprocessingResponseProto;

public class HtmlPreprocessingResponseMapper {

    public static HtmlPreprocessingResponseProto dtoToProto(HtmlPreprocessingResponseDTO dto) {
        return HtmlPreprocessingResponseProto.newBuilder()
                .setTaskId(dto.taskId())
                .setSuccess(dto.success())
                .setMessage(dto.message())
                .addAllHtmlPaths(dto.htmlPaths())
                .build();
    }

    public static HtmlPreprocessingResponseDTO protoToDto(HtmlPreprocessingResponseProto proto) {
        return HtmlPreprocessingResponseDTO.builder()
                .taskId(proto.getTaskId())
                .success(proto.getSuccess())
                .message(proto.getMessage())
                .htmlPaths(proto.getHtmlPathsList())
                .build();
    }
}
