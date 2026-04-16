package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.List;

@Builder
@Nullable
public record HtmlPreprocessingResponseDTO(
        String taskId,
        boolean success,
        String message,
        List<String> htmlPaths
) {
}
