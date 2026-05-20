package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.OrchestratorTaskStatusDTO;
import io.github.maksim0840.apigateway.dto.api.*;
import io.github.maksim0840.apigateway.service.OrchestratorStartService;
import io.github.maksim0840.apigateway.service.TaskService;
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
        System.out.println("getSessionId: " + sessionId);
        return sessionId;
    }

    @PostMapping("/pipeline/{sessionId}")
    public PipelineStartResponse pipelineStart(@PathVariable String sessionId, @RequestBody PipelineApiRequest request) {
        System.out.println(request);
        System.out.println("pipeline sessionId = " + sessionId);
        String taskId = orchestratorStartService.sendPipelineRequest(sessionId, request);
        return new PipelineStartResponse(taskId);
    }

    @GetMapping("/pipeline/{taskId}/status")
    public OrchestratorTaskStatusDTO getPipelineStatus(@PathVariable String taskId) {
        System.out.println("getPipelineStatus: " + taskId);
        return taskService.getStatus(taskId);
    }

    @GetMapping("/pipeline/{taskId}/result")
    public OrchestratorFinishDTO getPipelineResult(@PathVariable String taskId) {
        System.out.println("getPipelineResult: " + taskId);
        return taskService.getResult(taskId);
    }
//
//    @PostMapping("/parsing")
//    public void parsing(@RequestBody ParsingApiRequest request) {
//        System.out.println(request);
//        String sessionId = "test";
//        orchestratorStartService.sendParsingRequest(sessionId, request);
//    }
//
//    @PostMapping("/preprocessing")
//    public void preprocessing(@RequestBody PreprocessingApiRequest request) {
//        System.out.println(request);
//        String sessionId = "test";
//        orchestratorStartService.sendPreprocessingRequest(sessionId, request);
//    }
//
//    @PostMapping("/recognition")
//    public void recognition(@RequestBody RecognitionApiRequest request) {
//        System.out.println(request);
//        String sessionId = "test";
//        orchestratorStartService.sendRecognitionRequest(sessionId, request);
//    }
//
//    @PostMapping("/llm")
//    public void llm(@RequestBody LLMApiRequest request) {
//        System.out.println(request);
//        String sessionId = "test";
//        Map<String, String> textByImage = Map.of();
//        orchestratorStartService.sendLLMRequest(sessionId, textByImage, request);
//    }
}
