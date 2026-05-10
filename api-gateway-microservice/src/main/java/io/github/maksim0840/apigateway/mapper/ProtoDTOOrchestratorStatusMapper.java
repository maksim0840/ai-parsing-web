package io.github.maksim0840.apigateway.mapper;

import io.github.maksim0840.apigateway.dto.OrchestratorStatusDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TaskStatusMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorStatusResponse;

public class ProtoDTOOrchestratorStatusMapper {

    public static OrchestratorStatusDTO protoToDto(OrchestratorStatusResponse proto) {
        return OrchestratorStatusDTO.builder()
                .taskId(proto.getTaskId())
                .status(TaskStatusMapper.protoToEnum(proto.getStatus()))
                .message(proto.getMessage())
                .build();
    }
}
