package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.List;

@Builder
@Nullable
public record TextRecognitionRequestDTO(
        String taskId,
        List<String> imagePaths
) {
    // Использовать значения этого же объекта, но с измененным полем imagePaths
    public TextRecognitionRequestDTO withImagePaths(List<String> newImagePaths) {
        return new TextRecognitionRequestDTO(
                this.taskId,
                newImagePaths
        );
    }
}
