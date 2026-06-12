package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingResponseDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.HtmlPreprocessingResponseProto;

import java.util.List;

public class HtmlPreprocessingResponseProtoMapper {

    public static HtmlPreprocessingResponseProto dtoToProto(HtmlPreprocessingResponseDTO dto) {
        return HtmlPreprocessingResponseProto.newBuilder()
                .setTaskId(dto.taskId() != null ? dto.taskId() : "")
                .setSuccess(dto.success())
                .setMessage(dto.message() != null ? dto.message() : "")
                .addAllHtmlDocs(dto.htmlDocs() != null ? FileInfoProtoMapper.dtoToProtoList(dto.htmlDocs()) : List.of())
                .build();
    }

    public static HtmlPreprocessingResponseDTO protoToDto(HtmlPreprocessingResponseProto proto) {
        return HtmlPreprocessingResponseDTO.builder()
                .taskId(proto.getTaskId())
                .success(proto.getSuccess())
                .message(proto.getMessage())
                .htmlDocs(FileInfoProtoMapper.protoToDtoList(proto.getHtmlDocsList()))
                .build();
    }
}
