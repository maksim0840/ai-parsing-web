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
                .addAllHtmlDocs(dto.htmlDocs() != null ? FileInfoMapper.dtoToProtoList(dto.htmlDocs()) : List.of())
                .addAllImages(dto.images() != null ? FileInfoMapper.dtoToProtoList(dto.images()) : List.of())
                .build();
    }

    public static HtmlParserResponseDTO protoToDto(HtmlParserResponseProto proto) {
        return HtmlParserResponseDTO.builder()
                .taskId(proto.getTaskId())
                .success(proto.getSuccess())
                .message(proto.getMessage())
                .htmlDocs(FileInfoMapper.protoToDtoList(proto.getHtmlDocsList()))
                .images(FileInfoMapper.protoToDtoList(proto.getImagesList()))
                .build();
    }
}
