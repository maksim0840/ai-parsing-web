package io.github.maksim0840.internalapi.user.v1.dto;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.HtmlParserRequestDTO;
import io.github.maksim0840.parsing_task_orchestrator.v1.HtmlParserRequestProto;
import lombok.Builder;

import javax.annotation.Nullable;
import java.util.Map;

@Builder
@Nullable
public record HtmlParserParamsDTO(
    Boolean downloadImages,
    Map<String, String> headers,
    Map<String, String> cookies,
    Map<String, String> proxy,
    String pageComplexity,
    Integer additionalPageLoadTimeoutS
) {
}
