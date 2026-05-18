package io.github.maksim0840.apigateway.dto.api;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.FileInfoDTO;

import java.util.List;

public record RecognitionApiRequest(
        List<FileInfoDTO> images
) {
}
