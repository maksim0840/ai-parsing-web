package io.github.maksim0840.internalapi.user.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;

@Builder
@Nullable
public record HtmlPreprocessingParamsDTO(
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
