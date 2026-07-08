package io.github.maksim0840.internalapi.extraction_result.v1.mapper;

import io.github.maksim0840.extraction_result.v1.ResultFormatProto;
import io.github.maksim0840.internalapi.extraction_result.v1.enums.ResultFormat;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.FileType;
import io.github.maksim0840.parsing_task_orchestrator.v1.FileTypeProto;

public class ResultFormatProtoMapper {
    public static ResultFormatProto enumToProto(ResultFormat format) {
        return switch (format) {
            case JSON -> ResultFormatProto.JSON;
            case XML -> ResultFormatProto.XML;
            case CSV -> ResultFormatProto.CSV;
            default -> ResultFormatProto.RESULT_FORMAT_UNSPECIFIED;
        };
    }

    public static ResultFormat protoToEnum(ResultFormatProto proto) {
        return switch (proto) {
            case RESULT_FORMAT_UNSPECIFIED -> null;
            case JSON -> ResultFormat.JSON;
            case XML -> ResultFormat.XML;
            case CSV -> ResultFormat.CSV;
            default -> null;
        };
    }
}
