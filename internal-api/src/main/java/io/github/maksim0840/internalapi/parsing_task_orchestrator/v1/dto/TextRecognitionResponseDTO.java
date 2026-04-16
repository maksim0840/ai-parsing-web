package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.Map;

@Builder
@Nullable
public record TextRecognitionResponseDTO(
        String taskId,
        boolean success,
        String message,
        Map<String, String> textByImage
) {
}
