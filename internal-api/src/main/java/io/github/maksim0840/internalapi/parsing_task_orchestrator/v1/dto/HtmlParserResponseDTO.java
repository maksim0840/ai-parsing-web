package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.List;

@Builder
@Nullable
public record HtmlParserResponseDTO(
        String taskId,
        boolean success,
        String message,
        String htmlPath,
        List<String> imagePaths
) {
}
