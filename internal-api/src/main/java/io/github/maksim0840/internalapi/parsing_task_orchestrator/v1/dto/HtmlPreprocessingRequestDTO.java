package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.List;

@Builder
@Nullable
public record HtmlPreprocessingRequestDTO(
        String taskId,
        List<String> htmlPaths,
        Boolean noscriptProcessing,
        Boolean linkProcessing,
        Boolean styleProcessing,
        Boolean metaProcessing,
        Boolean scriptProcessing,
        Boolean canvasProcessing,
        Boolean svgProcessing,
        Boolean areaProcessing,
        Boolean imgProcessing,
        Boolean videoProcessing,
        Boolean audioProcessing,
        Boolean iframeProcessing,
        Boolean portalProcessing,
        Boolean embedProcessing,
        Boolean objectProcessing,
        Boolean sourceProcessing
) {
}
