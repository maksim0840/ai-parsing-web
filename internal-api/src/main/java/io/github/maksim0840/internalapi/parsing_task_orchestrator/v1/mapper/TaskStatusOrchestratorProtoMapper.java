package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.*;
import io.github.maksim0840.parsing_task_orchestrator.v1.GetTaskStatusOrchestratorResponse;
import io.github.maksim0840.parsing_task_orchestrator.v1.TaskResultOrchestratorProto;
import io.github.maksim0840.parsing_task_orchestrator.v1.TaskStatusOrchestratorProto;

public class TaskStatusOrchestratorProtoMapper {

    public static TaskStatusOrchestratorProto dtoToProto(TaskStatusOrchestratorDTO dto) {
        return TaskStatusOrchestratorProto.newBuilder()
                .setTaskId(dto.taskId() != null ? dto.taskId() : "")
                .setStatus(TaskStatusProtoMapper.enumToProto(dto.status()))
                .setMessage(dto.message() != null ? dto.message() : "")
                .build();
    }

    public static TaskStatusOrchestratorDTO protoToDto(TaskStatusOrchestratorProto proto) {
        return TaskStatusOrchestratorDTO.builder()
                .taskId(proto.getTaskId())
                .status(TaskStatusProtoMapper.protoToEnum(proto.getStatus()))
                .message(proto.getMessage())
                .build();
    }
}
