package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.Map;

@Builder
@Nullable
public record HtmlParserRequestDTO(
        String taskId,
        String url,
        String htmlOutDir,
        String imagesOutDir,
        Boolean downloadImages,
        Map<String, String> headers,
        Map<String, String> cookies,
        Map<String, String> proxy,
        String pageComplexity,
        Integer additionalPageLoadTimeoutS
) {
}
