package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.api.*;
import io.github.maksim0840.apigateway.security.JwtPrincipal;
import io.github.maksim0840.apigateway.service.OrchestratorStartService;
import io.github.maksim0840.apigateway.service.TaskService;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TaskResultOrchestratorDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TaskStatusOrchestratorDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MainController {

    private final OrchestratorStartService orchestratorStartService;
    private final TaskService taskService;

    public MainController(OrchestratorStartService orchestratorStartService, TaskService taskService) {
        this.orchestratorStartService = orchestratorStartService;
        this.taskService = taskService;
    }

    @GetMapping("/sessionId")
    public String getSessionId() {
        String sessionId = UUID.randomUUID().toString();
        return sessionId;
    }

    @PostMapping("/pipeline/{sessionId}")
    public PipelineStartResponse pipelineStart(
            @PathVariable String sessionId,
            @RequestBody PipelineApiRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        String userIdStr = String.valueOf(principal.userId());
        String taskId = orchestratorStartService.sendPipelineRequest(sessionId, userIdStr, request);
        return new PipelineStartResponse(taskId);
    }

    @GetMapping("/pipeline/{taskId}/status")
    public TaskStatusOrchestratorDTO getPipelineStatus(
            @PathVariable String taskId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        String userIdStr = String.valueOf(principal.userId());
        return taskService.getStatus(taskId, userIdStr);
    }

    @GetMapping("/pipeline/{taskId}/result")
    public TaskResultOrchestratorDTO getPipelineResult(
            @PathVariable String taskId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        String userIdStr = String.valueOf(principal.userId());
        return taskService.getResult(taskId, userIdStr);
    }
//
//    @PostMapping("/parsing")
//    public void parsing(@RequestBody ParsingApiRequest request) {
//        orchestratorStartService.sendParsingRequest();
//    }
//
//    @PostMapping("/preprocessing")
//    public void preprocessing(@RequestBody PreprocessingApiRequest request) {
//        orchestratorStartService.sendPreprocessingRequest();
//    }
//
//    @PostMapping("/recognition")
//    public void recognition(@RequestBody RecognitionApiRequest request) {
//        orchestratorStartService.sendRecognitionRequest();
//    }
//
//    @PostMapping("/llm")
//    public void llm(@RequestBody LLMApiRequest request) {
//        orchestratorStartService.sendLLMRequest();
//    }
}
