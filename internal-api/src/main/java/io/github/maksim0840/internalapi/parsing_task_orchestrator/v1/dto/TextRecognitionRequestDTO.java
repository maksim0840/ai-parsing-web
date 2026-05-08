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

    // Добавить несколько элементов в imagePaths
    public TextRecognitionRequestDTO addAllToImagePaths(List<String> extraImagePaths) {
        List<String> newImagePaths = this.imagePaths;
        newImagePaths.addAll(extraImagePaths);
        return withImagePaths(newImagePaths);
    }

    // Добавить один элемент в imagePaths
    public TextRecognitionRequestDTO addToImagePaths(String extraImagePaths) {
        List<String> newImagePaths = this.imagePaths;
        newImagePaths.add(extraImagePaths);
        return withImagePaths(newImagePaths);
    }
}
