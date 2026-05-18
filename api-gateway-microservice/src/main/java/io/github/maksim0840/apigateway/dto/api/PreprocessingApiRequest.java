package io.github.maksim0840.apigateway.dto.api;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.FileInfoDTO;

import java.util.List;

public record PreprocessingApiRequest(
        List<FileInfoDTO> htmlDocs,
        boolean noscript,
        boolean link,
        boolean style,
        boolean meta,
        boolean script,
        boolean canvas,
        boolean svg,
        boolean area,
        boolean img,
        boolean video,
        boolean audio,
        boolean iframe,
        boolean portal,
        boolean embed,
        boolean object,
        boolean source
) {
}
