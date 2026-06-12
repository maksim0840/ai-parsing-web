package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionResponseDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.TextRecognitionResponseProto;

import java.util.List;

public class TextRecognitionResponseProtoMapper {

    public static TextRecognitionResponseProto dtoToProto(TextRecognitionResponseDTO dto) {
        return TextRecognitionResponseProto.newBuilder()
                .setTaskId(dto.taskId() != null ? dto.taskId() : "")
                .setSuccess(dto.success())
                .setMessage(dto.message() != null ? dto.message() : "")
                .addAllImages(dto.images() != null ? FileInfoProtoMapper.dtoToProtoList(dto.images()) : List.of())
                .build();
    }

    public static TextRecognitionResponseDTO protoToDto(TextRecognitionResponseProto proto) {
        return TextRecognitionResponseDTO.builder()
                .taskId(proto.getTaskId())
                .success(proto.getSuccess())
                .message(proto.getMessage())
                .images(FileInfoProtoMapper.protoToDtoList(proto.getImagesList()))
                .build();
    }
}