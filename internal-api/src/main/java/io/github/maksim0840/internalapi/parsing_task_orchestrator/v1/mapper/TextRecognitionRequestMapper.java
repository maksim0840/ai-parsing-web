package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionRequestDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.TextRecognitionRequestProto;

import java.util.List;

public class TextRecognitionRequestMapper {

    public static TextRecognitionRequestProto dtoToProto(TextRecognitionRequestDTO dto) {
        return TextRecognitionRequestProto.newBuilder()
                .setTaskId(dto.taskId() != null ? dto.taskId() : "")
                .addAllImagePaths(dto.imagePaths() != null ? dto.imagePaths() : List.of())
                .build();
    }

    public static TextRecognitionRequestDTO protoToDto(TextRecognitionRequestProto proto) {
        return TextRecognitionRequestDTO.builder()
                .taskId(proto.getTaskId())
                .imagePaths(proto.getImagePathsList())
                .build();
    }
}
