package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.FileInfoDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.FileInfoProto;
import io.github.maksim0840.parsing_task_orchestrator.v1.HtmlParserRequestProto;

import java.util.List;
import java.util.Map;

public class FileInfoMapper {
    public static FileInfoProto dtoToProto(FileInfoDTO dto) {
        FileInfoProto.Builder protoBuilder = FileInfoProto.newBuilder();
        protoBuilder.setFilePath(dto.filePath() != null ? dto.filePath() : "");
        protoBuilder.setFileName(dto.fileName() != null ? dto.fileName() : "");
        protoBuilder.setFileType(FileTypeMapper.enumToProto(dto.fileType()));
        protoBuilder.setSizeBytes(dto.sizeBytes() != null ? dto.sizeBytes() : 0L);
        protoBuilder.setDescription(dto.description() != null ? dto.description() : "");
        protoBuilder.setIsValid(dto.isValid());
        protoBuilder.setErrorMessage(dto.errorMessage() != null ? dto.errorMessage() : "");
        return protoBuilder.build();
    }

    public static FileInfoDTO protoToDto(FileInfoProto proto) {
        return FileInfoDTO.builder()
                .filePath(proto.getFilePath())
                .fileName(proto.getFileName())
                .fileType(FileTypeMapper.protoToEnum(proto.getFileType()))
                .sizeBytes(proto.getSizeBytes())
                .description(proto.getDescription())
                .isValid(proto.getIsValid())
                .errorMessage(proto.getErrorMessage())
                .build();
    }

    public static List<FileInfoProto> dtoToProtoList(List<FileInfoDTO> dtoList) {
        return dtoList.stream().map(FileInfoMapper::dtoToProto).toList();
    }

    public static List<FileInfoDTO> protoToDtoList(List<FileInfoProto> protoList) {
        return protoList.stream().map(FileInfoMapper::protoToDto).toList();
    }
}
