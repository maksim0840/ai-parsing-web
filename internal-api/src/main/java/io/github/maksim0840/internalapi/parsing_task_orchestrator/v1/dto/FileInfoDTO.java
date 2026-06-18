package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.FileType;
import lombok.Builder;

import javax.annotation.Nullable;

@Builder
@Nullable
public record FileInfoDTO(
    String filePath,
    String fileName,
    FileType fileType,
    Long sizeBytes,
    String description,
    boolean valid,
    String errorMessage
) {
}
