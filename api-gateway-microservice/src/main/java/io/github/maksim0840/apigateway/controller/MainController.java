package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.OrchestratorFinishDTO;
import io.github.maksim0840.apigateway.dto.OrchestratorStatusDTO;
import io.github.maksim0840.apigateway.dto.api.*;
import io.github.maksim0840.apigateway.service.OrchestratorStartService;
import io.github.maksim0840.apigateway.service.TaskService;
import io.github.maksim0840.apigateway.storage.TaskStorage;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MainController {

    private final OrchestratorStartService orchestratorStartService;
    private final TaskService taskService;

    public MainController(OrchestratorStartService orchestratorStartService, TaskService taskService) {
        this.orchestratorStartService = orchestratorStartService;
        this.taskService = taskService;
    }

    @PostMapping("/pipeline")
    public PipelineStartResponse pipelineStart(@RequestBody PipelineApiRequest request) {
        System.out.println(request);
        System.out.println("pipeline rest random=" + new java.util.Random().nextInt());
        String userId = "test";
        orchestratorStartService.sendPipelineRequest(userId, request);
        return new PipelineStartResponse(userId);
    }

    @GetMapping("/pipeline/{taskId}/status")
    public OrchestratorStatusDTO getPipelineStatus(@PathVariable String taskId) {
        System.out.println("getPipelineStatus: " + taskId);
        return taskService.getStatus(taskId);
    }

    @GetMapping("/pipeline/{taskId}/result")
    public OrchestratorFinishDTO getPipelineResult(@PathVariable String taskId) {
        System.out.println("getPipelineResult: " + taskId);
        return taskService.getResult(taskId);
    }

    @PostMapping("/parsing")
    public void parsing(@RequestBody ParsingApiRequest request) {
        System.out.println(request);
        String userId = "test";
        orchestratorStartService.sendParsingRequest(userId, request);
    }

    @PostMapping("/preprocessing")
    public void preprocessing(@RequestBody PreprocessingApiRequest request) {
        System.out.println(request);
        String userId = "test";
        List<String> htmlPaths = List.of();
        orchestratorStartService.sendPreprocessingRequest(userId, htmlPaths, request);
    }

    @PostMapping("/recognition")
    public void recognition(@RequestBody RecognitionApiRequest request) {
        System.out.println(request);
        String userId = "test";
        List<String> imagePaths = List.of();
        orchestratorStartService.sendRecognitionRequest(userId, imagePaths, request);
    }

    @PostMapping("/llm")
    public void llm(@RequestBody LLMApiRequest request) {
        System.out.println(request);
        String userId = "test";
        List<String> imagePaths = List.of();
        Map<String, String> textByImage = Map.of();
        orchestratorStartService.sendLLMRequest(userId, imagePaths, textByImage, request);
    }
}
