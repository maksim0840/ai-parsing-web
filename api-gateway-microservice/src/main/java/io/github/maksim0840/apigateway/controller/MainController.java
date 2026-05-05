package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.api.*;
import io.github.maksim0840.apigateway.service.OrchestratorStartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MainController {

    private final OrchestratorStartService orchestratorStartService;

    public MainController(OrchestratorStartService orchestratorStartService) {
        this.orchestratorStartService = orchestratorStartService;
    }

    @PostMapping("/pipeline")
    public void pipeline(@RequestBody PipelineApiRequest request) {
        System.out.println(request);
        String userId = "test";
        orchestratorStartService.sendPipelineRequest(userId, request);
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
        orchestratorStartService.sendLLMRequest(userId, request);
    }
}
