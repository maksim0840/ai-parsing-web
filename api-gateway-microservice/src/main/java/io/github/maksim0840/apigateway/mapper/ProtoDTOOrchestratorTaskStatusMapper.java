package io.github.maksim0840.apigateway.mapper;

import io.github.maksim0840.apigateway.dto.OrchestratorTaskStatusDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TaskStatusMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.GetTaskStatusOrchestratorResponse;

public class ProtoDTOOrchestratorTaskStatusMapper {

    public static OrchestratorTaskStatusDTO protoToDto(GetTaskStatusOrchestratorResponse proto) {
        return OrchestratorTaskStatusDTO.builder()
                .taskId(proto.getTaskId())
                .status(TaskStatusMapper.protoToEnum(proto.getStatus()))
                .message(proto.getMessage())
                .build();
    }
}
