package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.apigateway.dto.OrchestratorFinishDTO;
import io.github.maksim0840.apigateway.mapper.ProtoDTOOrchestratorFinishMapper;
import io.github.maksim0840.apigateway.storage.TaskStorage;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlPreprocessingResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.TextRecognitionResponseDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlParserResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.HtmlPreprocessingResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.LLMResponseMapper;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.mapper.TextRecognitionResponseMapper;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishRequest;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishResponse;
import io.github.maksim0840.parsing_task_orchestrator.v1.OrchestratorFinishServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;

@GrpcService
public class OrchestratorFinishGrpcEndpoint extends OrchestratorFinishServiceGrpc.OrchestratorFinishServiceImplBase {

    private final TaskStorage taskStorage;

    public OrchestratorFinishGrpcEndpoint(TaskStorage taskStorage) {
        this.taskStorage = taskStorage;
    }

    @Override
    public void finishParsing(OrchestratorFinishRequest request, StreamObserver<OrchestratorFinishResponse> responseObserver) {
        System.out.println("OrchestratorFinishRequest:");
        System.out.println(request);
        System.out.println("Ответ нейросети:");
        String a = request.getLlmResponse().getLlmOutput();
        System.out.println(a);

        OrchestratorFinishDTO orchestratorFinishDTO = ProtoDTOOrchestratorFinishMapper.protoToDto(request);
        taskStorage.addResult(orchestratorFinishDTO.taskId(), orchestratorFinishDTO);

        OrchestratorFinishResponse response = OrchestratorFinishResponse.newBuilder()
                .setTaskId(orchestratorFinishDTO.taskId()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
