package io.github.maksim0840.apigateway.storage;

import io.github.maksim0840.apigateway.dto.OrchestratorFinishDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskStorage {
    private final Map<String, OrchestratorFinishDTO> orchestratorFinishById = new ConcurrentHashMap<>();;

    public void addResult(String taskId, OrchestratorFinishDTO orchestratorFinish) {
        orchestratorFinishById.put(taskId, orchestratorFinish);
    }

    public OrchestratorFinishDTO getResult(String taskId) {
        return orchestratorFinishById.get(taskId);
    }
}
