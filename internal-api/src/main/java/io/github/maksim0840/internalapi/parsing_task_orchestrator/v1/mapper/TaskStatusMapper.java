package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import io.github.maksim0840.parsing_task_orchestrator.v1.LLMResponseProto;
import io.github.maksim0840.parsing_task_orchestrator.v1.TaskStatusProto;

public class TaskStatusMapper {
    public static TaskStatusProto enumToProto(TaskStatus status) {
        return switch (status) {
            case NOT_REGISTERED -> TaskStatusProto.NOT_REGISTERED;
            case CREATED -> TaskStatusProto.CREATED;
            case HTML_PARSING -> TaskStatusProto.HTML_PARSING;
            case HTML_PREPROCESSING -> TaskStatusProto.HTML_PREPROCESSING;
            case TEXT_RECOGNITION -> TaskStatusProto.TEXT_RECOGNITION;
            case LLM_PROCESSING -> TaskStatusProto.LLM_PROCESSING;
            case DONE -> TaskStatusProto.DONE;
            case FAILED -> TaskStatusProto.FAILED;
            default -> TaskStatusProto.TASK_STATUS_UNSPECIFIED;
        };
    }

    public static TaskStatus protoToEnum(TaskStatusProto proto) {
        return switch (proto) {
            case TASK_STATUS_UNSPECIFIED -> null;
            case CREATED -> TaskStatus.CREATED;
            case HTML_PARSING -> TaskStatus.HTML_PARSING;
            case HTML_PREPROCESSING -> TaskStatus.HTML_PREPROCESSING;
            case TEXT_RECOGNITION -> TaskStatus.TEXT_RECOGNITION;
            case LLM_PROCESSING -> TaskStatus.LLM_PROCESSING;
            case DONE -> TaskStatus.DONE;
            case FAILED -> TaskStatus.FAILED;
            default -> null;
        };
    }
}
