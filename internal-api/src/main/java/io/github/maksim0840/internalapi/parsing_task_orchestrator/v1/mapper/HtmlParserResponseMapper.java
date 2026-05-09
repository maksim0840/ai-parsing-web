package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserResponseDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.HtmlParserResponseProto;

import java.util.List;

public class HtmlParserResponseMapper {

    public static HtmlParserResponseProto dtoToProto(HtmlParserResponseDTO dto) {
        return HtmlParserResponseProto.newBuilder()
                .setTaskId(dto.taskId() != null ? dto.taskId() : "")
                .setSuccess(dto.success())
                .setMessage(dto.message() != null ? dto.message() : "")
                .setHtmlPath(dto.htmlPath() != null ? dto.htmlPath() : "")
                .addAllImagePaths(dto.imagePaths() != null ? dto.imagePaths() : List.of())
                .build();
    }

    public static HtmlParserResponseDTO protoToDto(HtmlParserResponseProto proto) {
        return HtmlParserResponseDTO.builder()
                .taskId(proto.getTaskId())
                .success(proto.getSuccess())
                .message(proto.getMessage())
                .htmlPath(proto.getHtmlPath())
                .imagePaths(proto.getImagePathsList())
                .build();
    }
}
