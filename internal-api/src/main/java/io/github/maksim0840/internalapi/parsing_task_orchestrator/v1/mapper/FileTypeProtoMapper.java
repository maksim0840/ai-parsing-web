package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.FileType;
import io.github.maksim0840.parsing_task_orchestrator.v1.FileTypeProto;

public class FileTypeProtoMapper {

    public static FileTypeProto enumToProto(FileType status) {
        return switch (status) {
            case HTML -> FileTypeProto.HTML;
            case IMG -> FileTypeProto.IMG;
            default -> FileTypeProto.FILE_TYPE_UNSPECIFIED;
        };
    }

    public static FileType protoToEnum(FileTypeProto proto) {
        return switch (proto) {
            case FILE_TYPE_UNSPECIFIED -> null;
            case HTML -> FileType.HTML;
            case IMG -> FileType.IMG;
            default -> null;
        };
    }
}
